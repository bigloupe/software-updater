package updater.patch;

import com.nothome.delta.GDiffPatcher;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import updater.crypto.AESKey;
import updater.script.InvalidFormatException;
import updater.script.Patch;
import updater.script.Patch.Operation;
import updater.script.Patch.ValidationFile;
import updater.util.CommonUtil;
import updater.util.InterruptibleInputStream;
import updater.util.InterruptibleOutputStream;
import updater.util.Pausable;
import updater.util.SeekableFile;
import watne.seis720.project.AESForFile;
import watne.seis720.project.AESForFileListener;
import watne.seis720.project.KeySize;
import watne.seis720.project.Mode;
import watne.seis720.project.Padding;

/**
 * The patch patcher.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Patcher implements Pausable {

  /**
   * The log file.
   */
  protected File logFile;
  /**
   * The buffer for general purpose.
   */
  private byte[] buf;
  /**
   * The patching progress, from 0 to 100.
   */
  protected float progress;
  /**
   * Temporary references for {@link #doPatch(updater.patch.PatcherListener, 
   * java.io.File, int, updater.crypto.AESKey, java.io.File, java.io.File, java.util.Map)}
   */
  /**
   * The listener to listen to patching event and information.
   */
  protected PatcherListener listener;
  /**
   * The directory where the patch apply to. (With a file separator at the end.)
   */
  protected String softwareDir;
  /**
   * The temporary directory to store the patched file.
   */
  protected File tempDir;
  /**
   * The log writer for logging patching event.
   */
  protected LogWriter log;
  /**
   * Pausable.
   */
  /**
   * The output stream of 'temporary storage when patching old file to new file'.
   */
  protected InterruptibleOutputStream newFileOut;
  /**
   * The input stream of the patch.
   */
  protected InterruptibleInputStream interruptiblePatchIn;
  /**
   * The random accessible file of old file (when patching old file to new file).
   */
  protected SeekableFile seekableRandomAccessOldFile;
  /**
   * The AES cryptor.
   */
  protected AESForFile aesCryptor;

  /**
   * Constructor.
   * @param logFile the log file
   * @throws IOException {@code softwareDir} or {@code tempDir} is not a 
   * valid directory
   */
  public Patcher(File logFile) throws IOException {
    if (logFile == null) {
      throw new NullPointerException("argument 'logFile' cannot be null");
    }

    this.logFile = logFile;

    buf = new byte[32768];
    progress = 0;

    newFileOut = null;
    interruptiblePatchIn = null;
    seekableRandomAccessOldFile = null;
    aesCryptor = null;
  }

  /**
   * Pause or resume the patching.
   * @param pause true to pause, false to resume
   */
  @Override
  public void pause(boolean pause) {
    synchronized (this) {
      if (newFileOut != null) {
        newFileOut.pause(pause);
      }
      if (interruptiblePatchIn != null) {
        interruptiblePatchIn.pause(pause);
      }
      if (seekableRandomAccessOldFile != null) {
        seekableRandomAccessOldFile.pause(pause);
      }
      if (aesCryptor != null) {
        aesCryptor.pause(pause);
      }
    }
  }

  /**
   * Prepare the new file according to the {@code operation}.
   * @param operation the operation to get the information from
   * @param patchIn the patch input stream
   * @param newFile the new file
   * @param destFile the destination file
   * @throws IOException error occurred when creating the new file
   */
  protected void prepareNewFile(Operation operation, InterruptibleInputStream patchIn, File newFile, File destFile) throws IOException {
    if (operation == null) {
      throw new NullPointerException("argument 'operation' cannot be null");
    }
    if (patchIn == null) {
      throw new NullPointerException("argument 'patchIn' cannot be null");
    }
    if (newFile == null) {
      throw new NullPointerException("argument 'newFile' cannot be null");
    }
    if (destFile == null) {
      throw new NullPointerException("argument 'destFile' cannot be null");
    }

    interruptiblePatchIn = patchIn;

    OperationType operationType = OperationType.get(operation.getType());
    if (operationType == null) {
      return;
    }

    // check if the new file is already patched and waiting for do replacement already
    if (newFile.exists()) {
      if (newFile.length() == operation.getNewFileLength() && CommonUtil.getSHA256String(newFile).equals(operation.getNewFileChecksum())) {
        long byteSkipped = interruptiblePatchIn.skip(operation.getPatchLength());
        if (byteSkipped != operation.getPatchLength()) {
          throw new IOException("Failed to skip remaining bytes in 'patchIn'.");
        }
        return;
      } else {
        if (!newFile.delete()) {
          throw new IOException(String.format("Failed to delete new file: %1$s", newFile.getAbsolutePath()));
        }
      }
    }

    // do operation
    newFileOut = null;
    RandomAccessFile randomAccessOldFile = null;
    seekableRandomAccessOldFile = null;
    try {
      newFileOut = new InterruptibleOutputStream(new BufferedOutputStream(new FileOutputStream(newFile)));

      switch (operationType) {
        case FORCE:
        case NEW:
        case REPLACE:
          //<editor-fold defaultstate="collapsed" desc="add interrupted tasks">
          Runnable _interruptedTask = new Runnable() {

            @Override
            public void run() {
              CommonUtil.closeQuietly(newFileOut);
              CommonUtil.closeQuietly(interruptiblePatchIn);
            }
          };
          newFileOut.addInterruptedTask(_interruptedTask);
          interruptiblePatchIn.addInterruptedTask(_interruptedTask);
          //</editor-fold>

          int byteRead;
          int remaining = operation.getPatchLength();
          while (true) {
            if (remaining <= 0) {
              break;
            }

            int lengthToRead = buf.length > remaining ? remaining : buf.length;
            byteRead = interruptiblePatchIn.read(buf, 0, lengthToRead);
            if (byteRead == -1) {
              break;
            }
            newFileOut.write(buf, 0, byteRead);
            remaining -= byteRead;
          }
          break;
        case PATCH:
          GDiffPatcher diffPatcher = new GDiffPatcher();
          randomAccessOldFile = new RandomAccessFile(destFile, "r");
          seekableRandomAccessOldFile = new SeekableFile(randomAccessOldFile);

          //<editor-fold defaultstate="collapsed" desc="add interrupted tasks">
          final RandomAccessFile _randomAccessOldFile = randomAccessOldFile;
          Runnable __interruptedTask = new Runnable() {

            @Override
            public void run() {
              CommonUtil.closeQuietly(newFileOut);
              CommonUtil.closeQuietly(interruptiblePatchIn);
              CommonUtil.closeQuietly(_randomAccessOldFile);
            }
          };
          newFileOut.addInterruptedTask(__interruptedTask);
          interruptiblePatchIn.addInterruptedTask(__interruptedTask);
          seekableRandomAccessOldFile.addInterruptedTask(__interruptedTask);
          //</editor-fold>

          diffPatcher.patch(seekableRandomAccessOldFile, interruptiblePatchIn, newFileOut);
          break;
      }
    } finally {
      CommonUtil.closeQuietly(randomAccessOldFile);
      CommonUtil.closeQuietly(newFileOut);
      newFileOut = null;
      interruptiblePatchIn = null;
      seekableRandomAccessOldFile = null;
    }
  }
  protected int logFileIndex;
  protected boolean logDestFileExist;
  protected String logBackupFileAbsPath;
  protected String logNewFileAbsPath;
  protected String logDestFileAbsPath;

  protected void prepareLog(int fileIndex,
          boolean destFileExist,
          String backupFileAbsPath, String newFileAbsPath, String destFileAbsPath) {
    this.logFileIndex = fileIndex;
    this.logDestFileExist = destFileExist;
    this.logBackupFileAbsPath = backupFileAbsPath;
    this.logNewFileAbsPath = newFileAbsPath;
    this.logDestFileAbsPath = destFileAbsPath;
  }

  protected void log(int operationId) throws IOException {
    log.logPatch(LogAction.START, logFileIndex, operationId, logDestFileExist, logBackupFileAbsPath, logNewFileAbsPath, logDestFileAbsPath);
  }

  /**
   * Do the operation. 'remove', 'new' (folder only) and 'force' have no action 
   * to do here.
   * @param previousRecord the previous operation record, null means no 
   * previous record
   * @param operation the operation to do
   * @param patchIn the stream to read in
   * @return null if all succeed, a {@link PatchRecord} if replacement of 
   * files failed
   * @throws IOException error occurred when doing operation
   */
  protected ReplacementRecord doOperation(PatchRecord previousRecord, Operation operation, InterruptibleInputStream patchIn) throws IOException {
    //<editor-fold defaultstate="collapsed" desc="logic behind this function">
//remove:
//  type folder: (dest, backup)
//    dest exist:
//      dest is folder:
//        dest folder empty:
//          ** dest->backup                 1
//        dest folder not empty:
//          ** {ok}                         2
//      dest is file:
//        ** {error}                        3
//    dest not exist:
//      ** {ok}                             4
//  type file: (dest, backup)
//    dest exist:
//      dest is folder:
//        ** {error}                        5
//      dest is file:
//        ** dest->backup                   6
//    dest not exist:
//      ** backup exist -> {ok};            7
//         backup not exist -> {error}      8
//new:
//  type folder: (dest)
//    dest exist:
//      dest is folder:
//        ** {ok}                           9
//      dest is file:
//        ** {error}                        10
//    dest not exist:
//      ** create dest folder               11
//  type file: (new, dest)
//    dest exist:
//      dest is folder:
//        ** {error}                        12
//      dest is file:
//        ** length & checksum of dest match new -> {ok};       13
//           else -> {error}                14
//    dest not exist:
//      ** output patch -> new, new->dest   15
//force:
//  type folder: (dest)
//    dest exist:
//      dest is folder:
//        ** {ok}                           16
//      dest is file:
//        ** {error}                        17
//    dest not exist:
//      ** create dest folder               18
//  type file: (new, dest, backup)
//    dest exist:
//      dest is folder:
//        ** {error}                        19
//      dest is file:
//        ** backup not exist & checksum of dest not match new: output patch -> new, dest->backup, new->dest;   20
//           checksum of dest match new: {ok};                                                                  21
//           else: {error}                  22
//    dest not exist:
//      ** output patch -> new, new->dest   23
//patch:
//  type file: (new, dest, backup)
//    dest exist:
//      dest is folder:
//        ** {error}                        24
//      dest is file:
//        ** backup exist: {ok};            25
//           length & checksum of dest match old: patch dest -> new, dest->backup, new->dest;                   26
//           else -> {error}                27
//    dest not exist:
//      ** backup exist & new exist -> new->dest;               28
//         else -> {error}                  29
//replace:
//  type file: (new, dest, backup)
//    dest exist:
//      dest is folder:
//        ** {error}
//      dest is file:
//        ** backup exist: {ok};
//           length & checksum of dest match old: output patch -> new, dest->backup, new->dest;
//           else -> {error}
//    dest not exist:
//      ** backup exist & new exist -> new->dest;
//         else -> {error}
    //</editor-fold>

    if (operation == null) {
      throw new NullPointerException("argument 'operation' cannot be null");
    }
    if (patchIn == null) {
      throw new NullPointerException("argument 'patchIn' cannot be null");
    }

    ReplacementRecord returnValue = null;

    OperationType operationType = OperationType.get(operation.getType());
    if (operationType == null) {
      return returnValue;
    }

    File newFile = new File(tempDir + File.separator + operation.getId());
    File destFile = new File(softwareDir + operation.getDestFilePath());
    File backupFile = new File(tempDir + File.separator + "old_" + operation.getId());
    boolean destFileExist;
    if (previousRecord != null) {
      destFileExist = previousRecord.isDestinationFileExist();
    } else {
      destFileExist = destFile.exists();
    }
    String newFileAbsPath = newFile.getAbsolutePath(), destFileAbsPath = destFile.getAbsolutePath(), backupFileAbsPath = backupFile.getAbsolutePath();
    switch (operationType) {
      case REMOVE:
        prepareLog(operation.getId(), destFileExist, backupFileAbsPath, "", destFileAbsPath);
        if (operation.getFileType().equals("folder")) {
          listener.patchProgress((int) progress, String.format("Removing folder %1$s ...", operation.getDestFilePath()));
          if (destFile.exists()) {
            if (destFile.isDirectory()) {
              if (destFile.list().length == 0) {
                log(1);
                if (!destFile.renameTo(backupFile)) {
                  returnValue = new ReplacementRecord(operationType, 1, destFileAbsPath, "", backupFileAbsPath);
                }
              } else {
                log(2);
                // succeed
              }
            } else {
              log(3);
              throw new IOException(String.format("Remove folder: destFile %1$s expected folder but is a file", destFileAbsPath));
            }
          } else {
            log(4);
            // succeed
          }
        } else {
          listener.patchProgress((int) progress, String.format("Removing file %1$s ...", operation.getDestFilePath()));
          if (destFile.exists()) {
            if (destFile.isDirectory()) {
              log(5);
              throw new IOException(String.format("Remove file: destFile %1$s expecting file but is a folder", destFileAbsPath));
            } else {
              log(6);
              if (!destFile.renameTo(backupFile)) {
                returnValue = new ReplacementRecord(operationType, 6, destFileAbsPath, "", backupFileAbsPath);
              }
            }
          } else {
            if (backupFile.exists()) {
              log(7);
              // succeed
            } else {
              log(8);
              throw new IOException(String.format("Remove file: destFile %1$s not found and backupFile %2$s not exist", destFileAbsPath, backupFileAbsPath));
            }
          }
        }
        break;
      case NEW:
        if (operation.getFileType().equals("folder")) {
          prepareLog(operation.getId(), destFileExist, "", "", destFileAbsPath);
          listener.patchProgress((int) progress, String.format("Creating new folder %1$s ...", operation.getDestFilePath()));
          if (destFile.exists()) {
            if (destFile.isDirectory()) {
              log(9);
              // succeed
            } else {
              log(10);
              throw new IOException(String.format("Add new folder: destFile %1$s expecting a folder but is a file", destFileAbsPath));
            }
          } else {
            log(11);
            if (!destFile.mkdirs()) {
              throw new IOException(String.format("Failed to create folder: %1$s", destFileAbsPath));
            }
          }
        } else {
          prepareLog(operation.getId(), destFileExist, "", newFileAbsPath, destFileAbsPath);
          listener.patchProgress((int) progress, String.format("Adding new file %1$s ...", operation.getDestFilePath()));
          if (destFile.exists()) {
            if (destFile.isDirectory()) {
              log(12);
              throw new IOException(String.format("Add new file: destFile %1$s expecting a file but is a folder", destFileAbsPath));
            } else {
              if (operation.getNewFileLength() == destFile.length() && operation.getNewFileChecksum().equals(CommonUtil.getSHA256String(destFile))) {
                log(13);
                // succeed
              } else {
                log(14);
                throw new IOException(String.format("Add new file: destFile %1$s exist and not match with the length & checksum of the new file", destFileAbsPath));
              }
            }
          } else {
            log(15);
            prepareNewFile(operation, patchIn, newFile, destFile);
            if (!newFile.renameTo(destFile)) {
              returnValue = new ReplacementRecord(operationType, 15, "", newFileAbsPath, backupFileAbsPath);
            }
          }
        }
        break;
      case FORCE:
        if (operation.getFileType().equals("folder")) {
          prepareLog(operation.getId(), destFileExist, "", "", destFileAbsPath);
          listener.patchProgress((int) progress, String.format("Creating folder %1$s ...", operation.getDestFilePath()));
          if (destFile.exists()) {
            if (destFile.isDirectory()) {
              log(16);
              // succeed
            } else {
              log(17);
              throw new IOException(String.format("Force folder: destFile %1$s expecting folder but is a file", destFileAbsPath));
            }
          } else {
            log(18);
            if (!destFile.mkdirs()) {
              throw new IOException(String.format("Failed to create folder: %1$s", destFileAbsPath));
            }
          }
        } else {
          prepareLog(operation.getId(), destFileExist, backupFileAbsPath, newFileAbsPath, destFileAbsPath);
          listener.patchProgress((int) progress, String.format("Adding file %1$s ...", operation.getDestFilePath()));
          if (destFile.exists()) {
            if (destFile.isDirectory()) {
              log(19);
              throw new IOException(String.format("Force file: destFile %1$s expecting file but is a folder", destFileAbsPath));
            } else {
              long destFileLength = destFile.length();
              String destFileChecksum = null;
              try {
                destFileChecksum = CommonUtil.getSHA256String(destFile);
              } catch (IOException ex) {
                // failure is allowed
              }
              if (destFileChecksum == null) {
                log(0);
                throw new IOException(String.format("Failed to read file %1$s", destFileAbsPath));
//                returnValue = new ReplacementRecord(operationType, 0, destFileAbsPath, newFileAbsPath, backupFileAbsPath);
              } else if (!backupFile.exists() && (operation.getNewFileLength() != destFileLength || !operation.getNewFileChecksum().equals(destFileChecksum))) {
                log(20);
                prepareNewFile(operation, patchIn, newFile, destFile);
                if (!destFile.renameTo(backupFile) || !newFile.renameTo(destFile)) {
                  returnValue = new ReplacementRecord(operationType, 20, destFileAbsPath, newFileAbsPath, backupFileAbsPath);
                }
              } else if (operation.getNewFileLength() == destFileLength && operation.getNewFileChecksum().equals(destFileChecksum)) {
                prepareLog(operation.getId(), destFileExist, "", "", "");
                log(21);
                // succeed
              } else {
                log(22);
                throw new IOException(String.format("Force file: error occurred when doing file %1$s", destFileAbsPath));
              }
            }
          } else {
            log(23);
            prepareNewFile(operation, patchIn, newFile, destFile);
            if (!newFile.renameTo(destFile)) {
              returnValue = new ReplacementRecord(operationType, 23, destFileAbsPath, newFileAbsPath, backupFileAbsPath);
            }
          }
        }
        break;
      case PATCH:
      case REPLACE:
        prepareLog(operation.getId(), destFileExist, backupFileAbsPath, newFileAbsPath, destFileAbsPath);
        listener.patchProgress((int) progress, String.format("Patching %1$s ...", operation.getDestFilePath()));
        if (destFile.exists()) {
          if (destFile.isDirectory()) {
            log(24);
            throw new IOException(String.format("Replace/Patch file: destFile %1$s expecting file but is a directory", destFileAbsPath));
          } else {
            String destFileChecksum = null;
            try {
              destFileChecksum = CommonUtil.getSHA256String(destFile);
            } catch (IOException ex) {
              // failure is allowed
            }
            if (destFileChecksum == null) {
              log(0);
              throw new IOException(String.format("Failed to read file %1$s", destFileAbsPath));
//              returnValue = new ReplacementRecord(operationType, 0, destFileAbsPath, newFileAbsPath, backupFileAbsPath);
            } else if (backupFile.exists()) {
              log(25);
              // succeed
            } else if (operation.getOldFileLength() == destFile.length() && operation.getOldFileChecksum().equals(destFileChecksum)) {
              log(26);
              prepareNewFile(operation, patchIn, newFile, destFile);
              if (!destFile.renameTo(backupFile) || !newFile.renameTo(destFile)) {
                returnValue = new ReplacementRecord(operationType, 26, destFileAbsPath, newFileAbsPath, backupFileAbsPath);
              }
            } else {
              log(27);
              throw new IOException(String.format("Replace/Patch file: error occurred when doing file %1$s", destFileAbsPath));
            }
          }
        } else {
          if (backupFile.exists() && newFile.exists()) {
            log(28);
            if (!newFile.renameTo(destFile)) {
              returnValue = new ReplacementRecord(operationType, 28, destFileAbsPath, newFileAbsPath, backupFileAbsPath);
            }
          } else {
            log(29);
            throw new IOException(String.format("Replace/Patch file: error occurred when doing file, destFile %1$s not found", destFileAbsPath));
          }
        }
        break;
    }

    log.logPatch(returnValue == null ? LogAction.FINISH : LogAction.FAILED, operation.getId());

    return returnValue;
  }

  /**
   * Apply the patch.
   * @param listener the listener to listen to patching event and information
   * @param patchFile the patch file
   * @param patchId the patch id
   * @param aesKey the cipher key, null means no encryption used
   * @param softwareDir the directory where the patch apply to
   * @param tempDir the temporary directory to store the patched file
   * @param destinationReplacement a map that used to replace the destination 
   * path in {@code Operation}s in {@code patchFile}
   * @return a list containing those failed replacement
   * @throws IOException error occurred when doing patching
   */
  public List<ReplacementRecord> doPatch(final PatcherListener listener, File patchFile, int patchId, AESKey aesKey, File softwareDir, File tempDir, Map<String, String> destinationReplacement) throws IOException {
    if (listener == null) {
      throw new NullPointerException("argument 'listener' cannot be null");
    }
    if (patchFile == null) {
      throw new NullPointerException("argument 'patchFile' cannot be null");
    }
    if (softwareDir == null) {
      throw new NullPointerException("argument 'softwareDir' cannot be null");
    }
    if (tempDir == null) {
      throw new NullPointerException("argument 'tempDir' cannot be null");
    }

    this.listener = listener;

    if (!patchFile.exists() || patchFile.isDirectory()) {
      throw new IOException("patch file not exist or not a file");
    }

    if (!softwareDir.exists() || !softwareDir.isDirectory()) {
      throw new IOException("software directory not exist or not a directory");
    }
    this.softwareDir = softwareDir.getAbsolutePath() + File.separator;

    this.tempDir = tempDir;
    if (!tempDir.exists() || !tempDir.isDirectory()) {
      throw new IOException("temporary directory not exist or not a directory");
    }


    File _patchFile = patchFile;
    List<ReplacementRecord> replacementFailedList = new ArrayList<ReplacementRecord>();
    int startFromFileIndex = 0;
    Map<Integer, PatchRecord> extraFileIndexes = new HashMap<Integer, PatchRecord>();
    boolean patchingStarted = false;

    //<editor-fold defaultstate="collapsed" desc="startFromFileIndex, extraFileIndexes & patchingStarted">
    if (logFile.exists()) {
      LogReader logReader = new LogReader(logFile);

      if (logReader.isLogEnded()) {
        return replacementFailedList;
      }

      startFromFileIndex = logReader.getStartFileIndex();
      List<PatchRecord> failList = logReader.getFailList();
      for (PatchRecord patchRecord : failList) {
        extraFileIndexes.put(patchRecord.getFileIndex(), patchRecord);
      }
//            List<PatchRecord> revertList = logReader.getRevertList();
//            for (PatchRecord patchRecord : revertList) {
//                extraFileIndexes.put(patchRecord.getFileIndex(), new Object());
//            }
      patchingStarted = logReader.isLogStarted();
    }
    //</editor-fold>


    float decryptProgress = 0;
    float prepareProgress = 5;
    float updateProgress = 65;
    float validateFilesProgress = 30;


    float stageMinimumProgress = 0;
    progress = stageMinimumProgress;


    //<editor-fold defaultstate="collapsed" desc="decrypt the patch">
    if (aesKey != null) {
      decryptProgress = 25;
      updateProgress = 50;
      validateFilesProgress = 20;

      File tempFileForDecryption = new File(tempDir.getAbsolutePath() + File.separator + patchId + ".patch.decrypted");

      final float _decryptProgress = decryptProgress;
      final float _stageMinimumProgress = stageMinimumProgress;
      try {
        AESForFileListener aesForFileListener = new AESForFileListener() {

          @Override
          public void cryptProgress(int percentage) {
            listener.patchProgress((int) (_stageMinimumProgress + ((float) percentage / 100F) * _decryptProgress), "Decrypting patch ...");
          }
        };

        aesCryptor = new AESForFile();
        aesCryptor.setListener(aesForFileListener);
        aesCryptor.setMode(Mode.CBC);
        aesCryptor.setPadding(Padding.PKCS5PADDING);
        aesCryptor.setKeySize(KeySize.BITS256);
        aesCryptor.setKey(aesKey.getKey());
        aesCryptor.setInitializationVector(aesKey.getIV());
        aesCryptor.decryptFile(_patchFile, tempFileForDecryption);
      } catch (Exception ex) {
        throw new IOException(ex);
      } finally {
        aesCryptor = null;
      }

      _patchFile = tempFileForDecryption;
    }
    //</editor-fold>


    stageMinimumProgress += decryptProgress;
    progress = stageMinimumProgress;


    InputStream patchIn = null;
    log = new LogWriter(logFile);
    try {
      patchIn = new BufferedInputStream(new FileInputStream(_patchFile));


      listener.patchProgress((int) progress, "Preparing new patch ...");
      listener.patchEnableCancel(false);
      // header
      PatchReadUtil.readHeader(patchIn);
      InputStream decompressedPatchIn = PatchReadUtil.readCompressionMethod(patchIn);
      Patch patch = null;
      try {
        patch = PatchReadUtil.readXML(decompressedPatchIn);
      } catch (InvalidFormatException ex) {
        throw new IOException(ex);
      }

      List<Operation> operations = patch.getOperations();
      List<ValidationFile> validations = patch.getValidations();
      for (Operation operation : operations) {
        String destChangeTo = null;
        if ((destChangeTo = destinationReplacement.get(operation.getDestFilePath())) != null) {
          operation.setDestFilePath(destChangeTo);
        }
      }
      for (ValidationFile validation : validations) {
        String destChangeTo = null;
        if ((destChangeTo = destinationReplacement.get(validation.getFilePath())) != null) {
          validation.setFilePath(destChangeTo);
        }
      }

      // start log
      if (!patchingStarted) {
        log.logStart();
      } else {
        log.logResume();
      }


      stageMinimumProgress += prepareProgress;
      progress = stageMinimumProgress;


      listener.patchProgress((int) progress, "Updating ...");
      listener.patchEnableCancel(true);
      // start patch - patch files and store to temporary directory first
      float progressStep = updateProgress / (float) operations.size();
      for (int i = 0, iEnd = operations.size(); i < iEnd; i++) {
        Operation _operation = operations.get(i);

        if (!(i + 1 >= startFromFileIndex || extraFileIndexes.get(i + 1) != null)) {
          long byteSkipped = decompressedPatchIn.skip(_operation.getPatchLength());
          if (byteSkipped != _operation.getPatchLength()) {
            throw new IOException("Failed to skip remaining bytes in 'interruptiblePatchIn'.");
          }
          continue;
        }

        InterruptibleInputStream operationIn = new InterruptibleInputStream(decompressedPatchIn, _operation.getPatchLength());
        ReplacementRecord failedReplacement = doOperation(extraFileIndexes.get(i + 1), _operation, operationIn);
        if (operationIn.remaining() != 0) {
          long byteSkipped = decompressedPatchIn.skip(operationIn.remaining());
          if (byteSkipped != operationIn.remaining()) {
            throw new IOException("Failed to skip remaining bytes in 'interruptiblePatchIn'.");
          }
        }
        if (failedReplacement != null) {
          replacementFailedList.add(failedReplacement);
        }

        progress += progressStep;
      }


      stageMinimumProgress += updateProgress;
      progress = stageMinimumProgress;


      if (replacementFailedList.isEmpty()) {
        listener.patchProgress((int) progress, "Validating files ...");
        listener.patchEnableCancel(false);
        // validate files
        progressStep = validateFilesProgress / (float) validations.size();
        for (ValidationFile _validationFile : validations) {
          listener.patchProgress((int) progress, String.format("Validating file: %1$s", _validationFile.getFilePath()));

          File _file = new File(this.softwareDir + _validationFile.getFilePath());

          if (_validationFile.getFileLength() == -1) {
            if (!_file.isDirectory()) {
              throw new IOException(String.format("Folder missed: %1$s", _file.getAbsolutePath()));
            }
          } else {
            if (!_file.exists()) {
              throw new IOException(String.format("File missed: %1$s", _file.getAbsolutePath()));
            }
            if (_file.length() != _validationFile.getFileLength()) {
              throw new IOException(String.format("File length not matched, file: %1$s, expected: %2$d, found: %3$d",
                      _file.getAbsolutePath(), _validationFile.getFileLength(), _file.length()));
            }
            if (!CommonUtil.getSHA256String(_file).equals(_validationFile.getFileChecksum())) {
              throw new IOException(String.format("File checksum incorrect: %1$s", _file.getAbsolutePath()));
            }
          }

          progress += progressStep;
        }
      }


      stageMinimumProgress += validateFilesProgress;
      progress = stageMinimumProgress;


      listener.patchProgress(100, "Finished.");
      if (replacementFailedList.isEmpty()) {
        log.logEnd();
      }
    } finally {
      CommonUtil.closeQuietly(log);
      log = null;
      CommonUtil.closeQuietly(patchIn);
    }

    return replacementFailedList;
  }

  /**
   * Revert the patching and restore to unpatched state.
   * @throws IOException read log failed, or error occurred when doing revert 
   * (replacing file)
   */
  public void revert() throws IOException {
    if (logFile.exists()) {
      LogReader logReader = new LogReader(logFile);

      try {
        log = new LogWriter(logFile);

        List<PatchRecord> failList = logReader.getFailList();
        for (PatchRecord patchRecord : failList) {
          revert(patchRecord);
          log.logRevert(patchRecord.getFileIndex());
        }

        List<PatchRecord> revertList = logReader.getRevertList();
        for (PatchRecord patchRecord : revertList) {
          revert(patchRecord);
          log.logRevert(patchRecord.getFileIndex());
        }
      } finally {
        CommonUtil.closeQuietly(log);
        log = null;
      }
    }
  }

  /**
   * Revert the replacement according to {@code patchRecord}.
   * @param patchRecord the patch record
   * @throws IOException error occurred when doing revert (replacing file)
   */
  protected void revert(PatchRecord patchRecord) throws IOException, IOException {
    switch (patchRecord.getOperationId()) {
      case 1:
      case 6:
      case 7:
      case 15:
      case 20:
      case 23:
      case 25:
      case 26:
      case 28:
        revertFile(patchRecord);
        break;
      case 0:
      case 2:
      case 3:
      case 4:
      case 5:
      case 8:
      case 10:
      case 12:
      case 14:
      case 17:
      case 19:
      case 22:
      case 24:
      case 27:
      case 29:
        break;
      case 9:
      case 13:
      case 16:
      case 21:
        if (patchRecord.isDestinationFileExist()) {
          break;
        } else {
          revertFile(patchRecord);
        }
        break;
      case 11:
      case 18:
        new File(patchRecord.getDestinationFilePath()).delete();
        break;
    }
  }

  protected void revertFile(PatchRecord patchRecord) throws IOException {
    File newFile = new File(patchRecord.getNewFilePath());
    File destFile = new File(patchRecord.getDestinationFilePath());
    File backupFile = new File(patchRecord.getBackupFilePath());
    if (!newFile.exists() && destFile.exists()) {
      if (patchRecord.getNewFilePath().isEmpty()) {
        if (destFile.isDirectory()) {
          destFile.delete();
        } else {
          if (!destFile.delete()) {
            throw new IOException(String.format("Failed to delete %1$s (dest)", patchRecord.getDestinationFilePath()));
          }
        }
      } else {
        if (!destFile.renameTo(newFile)) {
          throw new IOException(String.format("Failed to move %1$s to %2$s (dest->new)", patchRecord.getDestinationFilePath(), patchRecord.getBackupFilePath()));
        }
      }
    }
    if (!destFile.exists() && backupFile.exists()) {
      if (!backupFile.renameTo(destFile)) {
        throw new IOException(String.format("Failed to move %1$s to %2$s (backup->dest)", patchRecord.getBackupFilePath(), patchRecord.getDestinationFilePath()));
      }
    }
  }

  /**
   * Remove all backup file in {@link #tempDir}. 
   * Should invoke this after the patching succeed and status recorded.
   * @return true if clear succeed, false if failed
   */
  public boolean clearBackup() {
    File[] files = tempDir.listFiles();
    if (files == null) {
      return false;
    }
    for (File file : files) {
      if (file.getName().matches("old_[0-9]+")) {
        file.delete();
      }
    }
    return true;
  }
}