package updater.patch;

import com.nothome.delta.GDiffPatcher;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import updater.crypto.AESKey;
import updater.patch.PatchLogReader.PatchRecord;
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
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }
    /**
     * The listener to listen to patching event and information.
     */
    protected PatcherListener listener;
    /**
     * The log file.
     */
    protected File logFile;
    /**
     * The log writer for logging patching event.
     */
    protected PatchLogWriter log;
    /**
     * The temporary directory to store the patched file.
     */
    protected File tempDir;
    /**
     * The directory where the patch apply to. (With a file separator at the end.)
     */
    protected String softwareDir;
    /**
     * The buffer for general purpose.
     */
    private byte[] buf;
    /**
     * The patching progress, from 0 to 100.
     */
    protected float progress;
    /**
     * Pausable.
     */
    /**
     * The output stream of 'temporary storage when patching old file to new file'.
     */
    protected InterruptibleOutputStream tempNewFileOut;
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
     * @param listener the listener to listen to patching event and information
     * @param logFile the log file
     * @param softwareDir the directory where the patch apply to
     * @param tempDir the temporary directory to store the patched file
     * @throws IOException {@code softwareDir} or {@code tempDir} is not a valid directory
     */
    public Patcher(PatcherListener listener, File logFile, File softwareDir, File tempDir) throws IOException {
        if (listener == null) {
            throw new NullPointerException("argument 'listener' cannot be null");
        }
        if (logFile == null) {
            throw new NullPointerException("argument 'logFile' cannot be null");
        }
        if (softwareDir == null) {
            throw new NullPointerException("argument 'softwareDir' cannot be null");
        }
        if (tempDir == null) {
            throw new NullPointerException("argument 'tempDir' cannot be null");
        }

        this.listener = listener;
        this.logFile = logFile;

        if (!softwareDir.exists() || !softwareDir.isDirectory()) {
            throw new IOException("software directory not exist or not a directory");
        }
        this.softwareDir = softwareDir.getAbsolutePath() + File.separator;

        this.tempDir = tempDir;
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            throw new IOException("temporary directory not exist or not a directory");
        }

        buf = new byte[32768];
        progress = 0;

        tempNewFileOut = null;
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
            if (tempNewFileOut != null) {
                tempNewFileOut.pause(pause);
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
     * Do the operation. 'remove', 'new' (folder only) and 'force' have no action to do here.
     * @param operation the operation to do
     * @param patchIn the stream to read in
     * @param tempNewFile the place to store the temporary generated in this operation
     * @throws IOException error occurred when doing operation
     */
    protected void doOperation(Operation operation, InputStream patchIn, File tempNewFile) throws IOException {
        if (operation == null) {
            throw new NullPointerException("argument 'operation' cannot be null");
        }
        if (patchIn == null) {
            throw new NullPointerException("argument 'patchIn' cannot be null");
        }
        if (tempNewFile == null) {
            throw new NullPointerException("argument 'tempNewFile' cannot be null");
        }

        OperationType operationType = OperationType.get(operation.getType());
        if (operationType == null) {
            return;
        }

        switch (operationType) {
            case REMOVE:
                // doOperation will not change/remove all existing 'old files'
                return;
            case NEW:
            case FORCE:
                if (operation.getFileType().equals("folder")) {
                    // folder will be created in 'replacement' stage
                    return;
                }
                listener.patchProgress((int) progress, String.format("Creating new file %1$s ...", operation.getDestFilePath()));
                break;
            case REPLACE:
            case PATCH:
                // replace or patch
                listener.patchProgress((int) progress, String.format("Patching %1$s ...", operation.getDestFilePath()));
                break;
        }

        // check old file checksum and length
        File oldFile = null;
        if (operationType == OperationType.PATCH || operationType == OperationType.REPLACE) {
            oldFile = new File(softwareDir + operation.getDestFilePath());
            if (!oldFile.exists()) {
                throw new IOException(String.format("Old file not exist: %1$s%2$s", softwareDir, operation.getDestFilePath()));
            }
            String oldFileChecksum = CommonUtil.getSHA256String(oldFile);
            long oldFileLength = oldFile.length();
            if (!oldFileChecksum.equals(operation.getOldFileChecksum()) || oldFileLength != operation.getOldFileLength()) {
                if (operation.getNewFileChecksum() != null && oldFileChecksum.equals(operation.getNewFileChecksum()) && oldFileLength == operation.getNewFileLength()) {
                    // done
                    return;
                } else {
                    throw new IOException(String.format("Checksum or length does not match (old file): %1$s", softwareDir + operation.getDestFilePath()));
                }
            }
        }

        // check if it is patched and waiting for move already
        if (tempNewFile.exists() && CommonUtil.getSHA256String(tempNewFile).equals(operation.getNewFileChecksum()) && tempNewFile.length() == operation.getNewFileLength()) {
            long byteSkipped = patchIn.skip(operation.getPatchLength());
            if (byteSkipped != operation.getPatchLength()) {
                throw new IOException("Failed to skip remaining bytes in 'patchIn'.");
            }
            return;
        }

        // do operation
        tempNewFileOut = null;
        interruptiblePatchIn = null;
        RandomAccessFile randomAccessOldFile = null;
        seekableRandomAccessOldFile = null;
        try {
            tempNewFileOut = new InterruptibleOutputStream(new BufferedOutputStream(new FileOutputStream(tempNewFile)));
            interruptiblePatchIn = new InterruptibleInputStream(patchIn, operation.getPatchLength());

            if (operationType == OperationType.PATCH) {
                GDiffPatcher diffPatcher = new GDiffPatcher();
                randomAccessOldFile = new RandomAccessFile(oldFile, "r");
                seekableRandomAccessOldFile = new SeekableFile(randomAccessOldFile);

                //<editor-fold defaultstate="collapsed" desc="add interrupted tasks">
                final OutputStream _tempNewFileOut = tempNewFileOut;
                final InterruptibleInputStream _interruptiblePatchIn = interruptiblePatchIn;
                final RandomAccessFile _randomAccessOldFile = randomAccessOldFile;
                Runnable interruptedTask = new Runnable() {

                    @Override
                    public void run() {
                        CommonUtil.closeQuietly(_tempNewFileOut);
                        CommonUtil.closeQuietly(_interruptiblePatchIn);
                        CommonUtil.closeQuietly(_randomAccessOldFile);
                    }
                };
                tempNewFileOut.addInterruptedTask(interruptedTask);
                interruptiblePatchIn.addInterruptedTask(interruptedTask);
                seekableRandomAccessOldFile.addInterruptedTask(interruptedTask);
                //</editor-fold>

                diffPatcher.patch(seekableRandomAccessOldFile, interruptiblePatchIn, tempNewFileOut);
            } else {
                //<editor-fold defaultstate="collapsed" desc="add interrupted tasks">
                final OutputStream _tempNewFileOut = tempNewFileOut;
                final InterruptibleInputStream _interruptiblePatchIn = interruptiblePatchIn;
                Runnable interruptedTask = new Runnable() {

                    @Override
                    public void run() {
                        CommonUtil.closeQuietly(_tempNewFileOut);
                        CommonUtil.closeQuietly(_interruptiblePatchIn);
                    }
                };
                tempNewFileOut.addInterruptedTask(interruptedTask);
                interruptiblePatchIn.addInterruptedTask(interruptedTask);
                //</editor-fold>

                // replace, new or force
                int byteRead,
                        remaining = operation.getPatchLength();
                while (true) {
                    if (remaining <= 0) {
                        break;
                    }

                    int lengthToRead = buf.length > remaining ? remaining : buf.length;
                    byteRead = interruptiblePatchIn.read(buf, 0, lengthToRead);
                    if (byteRead == -1) {
                        break;
                    }
                    tempNewFileOut.write(buf, 0, byteRead);
                    remaining -= byteRead;
                }
            }
        } finally {
            CommonUtil.closeQuietly(randomAccessOldFile);
            if (interruptiblePatchIn != null) {
                long byteSkipped = patchIn.skip(interruptiblePatchIn.remaining());
                if (byteSkipped != interruptiblePatchIn.remaining()) {
                    throw new IOException("Failed to skip remaining bytes in 'interruptiblePatchIn'.");
                }
            }
            CommonUtil.closeQuietly(tempNewFileOut);
            tempNewFileOut = null;
            interruptiblePatchIn = null;
            seekableRandomAccessOldFile = null;
        }

        // check new file checksum and length
        if (operationType != OperationType.NEW) {
            String tempNewFileSHA256 = CommonUtil.getSHA256String(tempNewFile);
            if (!tempNewFileSHA256.equals(operation.getNewFileChecksum()) || tempNewFile.length() != operation.getNewFileLength()) {
                throw new IOException(String.format("Checksum or length does not match (new file): %1$s, old file path: %2$s, expected checksum: %3$s, actual checksum: %4$s, expected length: %5$d, actual length: %6$d",
                        tempNewFile.getAbsolutePath(), softwareDir + operation.getDestFilePath(), operation.getNewFileChecksum(), tempNewFileSHA256, operation.getNewFileLength(), tempNewFile.length()));
            }
        }
    }

    /**
     * Do file replacement of all {@code operations} start from {@code startFromFileIndex}.
     * @param operations the list of {@link operations} to do the replacement
     * @param startFromFileIndex the starting index to the {@code operations}, count from 0
     * @param progressOccupied the maximum progress can be occupied by this replacement action (from 0 to 100)
     * @return a list containing those failed replacement
     * @throws IOException error occurred when doing replacement
     */
    protected List<Replacement> doReplacement(List<Operation> operations, int startFromFileIndex, float progressOccupied) throws IOException {
        if (operations == null) {
            throw new NullPointerException("argument 'operations' cannot be null");
        }
        if (startFromFileIndex < 0) {
            throw new IllegalArgumentException("argument 'startFromFileIndex' should >= 0");
        }
        if (progressOccupied < 0) {
            throw new IllegalArgumentException("argument 'progressOccupied' should >= 0");
        }
        if (progressOccupied > 100) {
            throw new IllegalArgumentException("argument 'progressOccupied' should <= 100");
        }

        List<Replacement> replacementFailedList = new ArrayList<Replacement>();

        float progressStep = progressOccupied / (float) operations.size();
        progress += startFromFileIndex * progressStep;

        for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
            Operation _operation = operations.get(i);

            OperationType operationType = OperationType.get(_operation.getType());
            if (operationType == null) {
                continue;
            }

            File backupFile = new File(tempDir + File.separator + "old_" + i);

            boolean replacementSucceed = false;
            if (operationType == OperationType.REMOVE) {
                listener.patchProgress((int) progress, String.format("Removing %1$s ...", _operation.getDestFilePath()));

                File destinationFile = new File(softwareDir + _operation.getDestFilePath());

                log.logPatch(PatchLogWriter.Action.START, i, operationType, backupFile.getAbsolutePath(), "", destinationFile.getAbsolutePath());

                if (destinationFile.exists()) {
                    if (destinationFile.isDirectory()) {
                        if (destinationFile.listFiles().length == 0) {
                            replacementSucceed = destinationFile.renameTo(backupFile);
                        } else {
                            // folder not empty, no action
                            replacementSucceed = true;
                        }
                    } else {
                        replacementSucceed = destinationFile.renameTo(backupFile);
                    }
                } else {
                    replacementSucceed = true;
                }

                if (!replacementSucceed) {
                    replacementFailedList.add(new Replacement(operationType, destinationFile.getAbsolutePath(), "", backupFile.getAbsolutePath()));
                }
            } else if (operationType == OperationType.NEW || operationType == OperationType.FORCE) {
                listener.patchProgress((int) progress, String.format("Copying new file to %1$s ...", _operation.getDestFilePath()));

                File newFile = new File(tempDir + File.separator + i);
                File destinationFile = new File(softwareDir + _operation.getDestFilePath());

                if (_operation.getFileType().equals("folder")) {
                    log.logPatch(PatchLogWriter.Action.START, i, operationType, "", "", destinationFile.getAbsolutePath());

                    if (!destinationFile.isDirectory() && !destinationFile.mkdirs()) {
                        throw new IOException(String.format("Create folder failed: %1$s", softwareDir + _operation.getDestFilePath()));
                    }

                    replacementSucceed = true;
                } else {
                    log.logPatch(PatchLogWriter.Action.START, i, operationType, backupFile.getAbsolutePath(), newFile.getAbsolutePath(), destinationFile.getAbsolutePath());

                    File newFileFolder = new File(CommonUtil.getFileDirectory(destinationFile));
                    if (!newFileFolder.exists() && !newFileFolder.mkdirs()) {
                        throw new IOException(String.format("Failed to create folder %1$s for placing %2$s", newFileFolder.getAbsolutePath(), destinationFile.getAbsolutePath()));
                    }

                    if (destinationFile.exists() && destinationFile.length() == _operation.getNewFileLength() && CommonUtil.getSHA256String(destinationFile).equals(_operation.getNewFileChecksum())) {
                        // done
                    } else if ((destinationFile.exists() && !destinationFile.renameTo(backupFile)) || !newFile.renameTo(destinationFile)) {
                        replacementFailedList.add(new Replacement(operationType, destinationFile.getAbsolutePath(), newFile.getAbsolutePath(), backupFile.getAbsolutePath()));
                        replacementSucceed = false;
                    } else {
                        replacementSucceed = true;
                    }
                }
            } else {
                // patch or replace
                listener.patchProgress((int) progress, String.format("Copying from %1$s to %2$s ...", tempDir + File.separator + i, _operation.getDestFilePath()));

                File newFile = new File(tempDir + File.separator + i);
                File destinationFile = new File(softwareDir + _operation.getDestFilePath());

                log.logPatch(PatchLogWriter.Action.START, i, operationType, backupFile.getAbsolutePath(), newFile.getAbsolutePath(), destinationFile.getAbsolutePath());

                if (destinationFile.exists() && destinationFile.length() == _operation.getNewFileLength() && CommonUtil.getSHA256String(destinationFile).equals(_operation.getNewFileChecksum())) {
                    // done
                } else {
                    if ((!backupFile.exists() && !destinationFile.renameTo(backupFile)) || (newFile.exists() && !newFile.renameTo(destinationFile))) {
                        // if patchedFile not exist, that means the checksum of oldFile not match with those in _operation.getOldFileChecksum() & _operation.getOldFileLength()
                        // but match _operation.getNewFileChecksum() & _operation.getNewFileLength()
                        replacementFailedList.add(new Replacement(operationType, destinationFile.getAbsolutePath(), newFile.getAbsolutePath(), backupFile.getAbsolutePath()));
                        replacementSucceed = false;
                    } else {
                        replacementSucceed = true;
                    }
                }
            }
            log.logPatch(replacementSucceed ? PatchLogWriter.Action.FINISH : PatchLogWriter.Action.FAILED, i, operationType);

            progress += progressStep;
        }

        return replacementFailedList;
    }

    /**
     * Apply the patch.
     * @param patchFile the patch file
     * @param patchId the patch id
     * @param aesKey the cipher key, null means no encryption used
     * @param tempFileForDecryption if {@code aesKey} is specified, this should be provided to store the temporary decrypted file
     * @param destinationReplacement a map that used to replace the destination path in {@code Operation}s in {@code patchFile}
     * @return a list containing those failed replacement
     * @throws IOException error occurred when doing patching
     */
    public List<Replacement> doPatch(File patchFile, int patchId, AESKey aesKey, File tempFileForDecryption, Map<String, String> destinationReplacement) throws IOException {
        if (patchFile == null) {
            throw new NullPointerException("argument 'patchFile' cannot be null");
        }
        if (aesKey != null && tempFileForDecryption == null) {
            throw new NullPointerException("argument 'tempFileForDecryption' cannot be null while argument 'aesKey' is not null");
        }

        if (!patchFile.exists() || patchFile.isDirectory()) {
            throw new IOException("patch file not exist or not a file");
        }

        List<Replacement> replacementFailedList = null;

        int startFromFileIndex = 0;
        if (logFile.exists()) {
            PatchLogReader logReader = new PatchLogReader(logFile);
            if (logReader.getStartFileIndex() == -1) {
                if (logReader.isLogEnded()) {
                    return replacementFailedList;
                }
            } else {
                startFromFileIndex = logReader.getStartFileIndex();
            }
        }

        float decryptProgress = 0;
        float prepareProgress = 5;
        float updateProgress = 60;
        float checkAccessibilityProgress = 0;
        float replaceFilesProgress = 5;
        float validateFilesProgress = 30;


        float stageMinimumProgress = 0;
        progress = stageMinimumProgress;


        // decrypt the patch
        File _patchFile = patchFile;
        if (aesKey != null) {
            decryptProgress = 25;
            updateProgress = 45;
            validateFilesProgress = 20;

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
                aesCryptor.decryptFile(patchFile, tempFileForDecryption);
            } catch (Exception ex) {
                throw new IOException(ex);
            } finally {
                aesCryptor = null;
            }

            _patchFile = tempFileForDecryption;
        }


        stageMinimumProgress += decryptProgress;
        progress = stageMinimumProgress;


        InputStream patchIn = null;
        log = new PatchLogWriter(logFile);
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
                if (debug) {
                    Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
                }
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

            // start log
            if (startFromFileIndex == 0) {
                log.logStart(patchId, patch.getVersionFrom(), patch.getVersionTo());
            } else {
                log.logResume();
            }


            stageMinimumProgress += prepareProgress;
            progress = stageMinimumProgress;


            listener.patchProgress((int) progress, "Updating ...");
            listener.patchEnableCancel(true);
            // start patch - patch files and store to temporary directory first
            float progressStep = updateProgress / (float) operations.size();
            progress += startFromFileIndex * progressStep;
            for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
                Operation _operation = operations.get(i);
                doOperation(_operation, decompressedPatchIn, new File(tempDir + File.separator + i));
                progress += progressStep;
            }


            stageMinimumProgress += updateProgress;
            progress = stageMinimumProgress;


            listener.patchProgress((int) progress, "Checking the accessibility of all files ...");
            // try acquire locks on all files
//            List<Operation> acquireLockFailedList = tryAcquireExclusiveLocks(operations, startFromFileIndex);


            stageMinimumProgress += checkAccessibilityProgress;
            progress = stageMinimumProgress;


            listener.patchProgress((int) progress, "Replacing old files with new files ...");
            listener.patchEnableCancel(false);
            // all files has patched to temporary directory, replace old files with the new one
            replacementFailedList = doReplacement(operations, startFromFileIndex, replaceFilesProgress);


            stageMinimumProgress += replaceFilesProgress;
            progress = stageMinimumProgress;


            listener.patchProgress((int) progress, "Validating files ...");
            // validate files
            progressStep = validateFilesProgress / (float) validations.size();
            for (ValidationFile _validationFile : validations) {
                listener.patchProgress((int) progress, String.format("Validating file: %1$s", _validationFile.getFilePath()));

                File _file = new File(softwareDir + _validationFile.getFilePath());

                // the checksum and length is checked on the new file when doOperation
                boolean fileNotReplaced = false;
                String _validationFileAbsPath = _file.getAbsolutePath();
                for (Replacement _replacement : replacementFailedList) {
                    if (new File(_replacement.getDestination()).getAbsolutePath().equals(_validationFileAbsPath)) {
                        fileNotReplaced = true;
                        break;
                    }
                }
                if (fileNotReplaced) {
                    continue;
                }

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


            stageMinimumProgress += validateFilesProgress;
            progress = stageMinimumProgress;


            listener.patchProgress(100, "Finished.");
            log.logEnd();
        } finally {
            CommonUtil.closeQuietly(log);
            log = null;
            CommonUtil.closeQuietly(patchIn);
        }

        return replacementFailedList;
    }

    /**
     * Revert the patching and restore to unpatched state.
     * @throws IOException read log failed, or error occurred when doing revert (replacing file)
     */
    public void revert() throws IOException {
        if (logFile.exists()) {
            PatchLogReader logReader = new PatchLogReader(logFile);

            PatchRecord unfinishedReplacement = logReader.getUnfinishedReplacement();
            revertFile(unfinishedReplacement);

            List<PatchRecord> revertList = logReader.getRevertList();
            for (PatchRecord patchRecord : revertList) {
                revertFile(patchRecord);
            }
        }
    }

    /**
     * Revert the replacement according to {@code patchRecord}.
     * @param patchRecord the patch record
     * @throws IOException error occurred when doing revert (replacing file)
     */
    protected void revertFile(PatchRecord patchRecord) throws IOException {
        File newFile = new File(patchRecord.getNewFilePath());
        File destFile = new File(patchRecord.getDestinationFilePath());
        File backupFile = new File(patchRecord.getBackupFilePath());
        if (!newFile.exists() && destFile.exists()) {
            if (!destFile.renameTo(newFile)) {
                throw new IOException(String.format("Failed to move %1$s to %2$s (dest->new)", patchRecord.getDestinationFilePath(), patchRecord.getBackupFilePath()));
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
     */
    public void clearBackup() {
        File[] files = tempDir.listFiles();
        for (File file : files) {
            if (file.getName().matches("old_[0-9]+")) {
                file.delete();
            }
        }
    }

    /**
     * The class that record failed replacement.
     */
    public static class Replacement {

        /**
         * The operation type.
         */
        protected OperationType operationType;
        /**
         * The path to move the new file to.
         */
        protected String destination;
        /**
         * The path where the new file locate.
         */
        protected String newFilePath;
        /**
         * The path where the backup file locate.
         */
        protected String backupFilePath;

        /**
         * Constructor.
         * @param operationType the operation type
         * @param destination the path to move the new file to
         * @param newFilePath the path where the new file locate
         * @param backupFilePath the path where the backup file locate
         */
        protected Replacement(OperationType operationType, String destination, String newFilePath, String backupFilePath) {
            if (operationType == null) {
                throw new NullPointerException("argument 'operationType' cannot be null");
            }
            if (destination == null) {
                throw new NullPointerException("argument 'destination' cannot be null");
            }
            if (newFilePath == null) {
                throw new NullPointerException("argument 'newFilePath' cannot be null");
            }
            if (backupFilePath == null) {
                throw new NullPointerException("argument 'backupFilePath' cannot be null");
            }
            this.operationType = operationType;
            this.destination = destination;
            this.newFilePath = newFilePath;
            this.backupFilePath = backupFilePath;
        }

        /**
         * Get operation type.
         * @return the operation type
         */
        public OperationType getOperationType() {
            return operationType;
        }

        /**
         * Get the path to move the new file to.
         * @return the path
         */
        public String getDestination() {
            return this.destination;
        }

        /**
         * Get the path where the new file locate.
         * @return the path
         */
        public String getNewFilePath() {
            return this.newFilePath;
        }

        /**
         * Get the path where the backup file locate.
         * @return the path
         */
        public String getBackupFilePath() {
            return backupFilePath;
        }
    }
}