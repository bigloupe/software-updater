package updater.launcher.patch;

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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.tukaani.xz.XZInputStream;
import updater.launcher.util.SeekableFile;
import updater.launcher.util.Util;
import updater.script.InvalidFormatException;
import updater.script.Patch;
import updater.script.Patch.Operation;
import updater.script.Patch.ValidationFile;
import updater.util.InterruptibleInputStream;
import updater.util.InterruptibleOutputStream;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Patcher {

    protected PatcherListener listener;
    protected PatchLogWriter log;
    protected File patchFile;
    protected File tempDir;
    private byte[] buf;
    protected float progress;

    public Patcher(PatcherListener listener, PatchLogWriter log, File patchFile, File tempDir) throws IOException {
        this.listener = listener;
        this.log = log;

        this.patchFile = patchFile;
        if (!patchFile.exists() || patchFile.isDirectory()) {
            throw new IOException("patch file not exist or not a file");
        }

        this.tempDir = tempDir;
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            throw new IOException("temporary directory not exist or not a directory");
        }

        buf = new byte[32768];
        progress = 0;
    }

    public void close() {
        if (log != null) {
            try {
                log.close();
            } catch (IOException ex) {
                Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    protected void checkHeader(InputStream in) throws IOException {
        if (in.read(buf, 0, 5) != 5) {
            throw new IOException("Reach the end of stream.");
        }
        if (buf[0] != 'P' || buf[1] != 'A' || buf[2] != 'T' || buf[3] != 'C' || buf[4] != 'H') {
            throw new IOException("Invalid patch header.");
        }
    }

    protected InputStream getCompressionMethod(InputStream in) throws IOException {
        if (in.read(buf, 0, 1) != 1) {
            throw new IOException("Reach the end of stream.");
        }
        int compressionMode = buf[0] & 0xff;
        switch (compressionMode) {
            case 0: //gzip
                return new GZIPInputStream(in);
            case 1: // XZ/LZMA2
                return new XZInputStream(in);
            default:
                throw new IOException("Compression method not supported/not exist");
        }
    }

    protected Patch getXML(InputStream in) throws IOException, InvalidFormatException {
        if (in.read(buf, 0, 3) != 3) {
            throw new IOException("Reach the end of stream.");
        }
        int xmlLength = ((buf[0] & 0xff) << 16) | ((buf[1] & 0xff) << 8) | (buf[2] & 0xff);
        byte[] xmlData = new byte[xmlLength];
        if (in.read(xmlData) != xmlLength) {
            throw new IOException("Reach the end of stream.");
        }
        return Patch.read(xmlData);
    }

    protected void doOperation(Operation operation, InputStream patchIn, File tempNewFile) throws Exception {
        if (operation.getType().equals("remove")) {
            // doOperation will not change/remove all existing 'old files'
            return;
        } else if (operation.getType().equals("new")) {
            if (operation.getFileType().equals("folder")) {
                return;
            }
            listener.patchProgress((int) progress, "Creating new file " + operation.getNewFilePath() + " ...");
        } else {
            // replace or patch
            listener.patchProgress((int) progress, "Patching " + operation.getOldFilePath() + " ...");
        }

        File oldFile = null;
        if (operation.getOldFilePath() != null) {
            // check old file checksum and length
            oldFile = new File(operation.getOldFilePath());
            if (!oldFile.exists()) {
                throw new Exception("Old file not exist: " + operation.getOldFilePath());
            }
            if (!Util.getSHA256String(oldFile).equals(operation.getOldFileChecksum()) || oldFile.length() != operation.getOldFileLength()) {
                throw new Exception("Checksum or length does not match (old file): " + operation.getOldFilePath());
            }
        }

        // check if it is patched and waiting for move already
        if (tempNewFile.exists() && Util.getSHA256String(tempNewFile).equals(operation.getNewFileChecksum()) && tempNewFile.length() == operation.getNewFileLength()) {
            return;
        }

        InterruptibleOutputStream tempNewFileOut = null;
        InterruptibleInputStream interruptiblePatchIn = null;
        RandomAccessFile randomAccessOldFile = null;
        try {
            tempNewFileOut = new InterruptibleOutputStream(new BufferedOutputStream(new FileOutputStream(tempNewFile)));
            interruptiblePatchIn = new InterruptibleInputStream(patchIn, operation.getPatchLength());

            if (operation.getType().equals("patch")) {
                GDiffPatcher diffPatcher = new GDiffPatcher();
                randomAccessOldFile = new RandomAccessFile(oldFile, "r");
                SeekableFile seekableRandomAccessOldFile = new SeekableFile(randomAccessOldFile);

                //<editor-fold defaultstate="collapsed" desc="add interrupted tasks">
                final OutputStream _tempNewFileOut = tempNewFileOut;
                final InterruptibleInputStream _interruptiblePatchIn = interruptiblePatchIn;
                final RandomAccessFile _randomAccessOldFile = randomAccessOldFile;
                Runnable interruptedTask = new Runnable() {

                    @Override
                    public void run() {
                        try {
                            _tempNewFileOut.close();
                            _interruptiblePatchIn.close();
                            _randomAccessOldFile.close();
                        } catch (IOException ex) {
                            Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
                        }
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
                        try {
                            _tempNewFileOut.close();
                            _interruptiblePatchIn.close();
                        } catch (IOException ex) {
                            Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                };
                tempNewFileOut.addInterruptedTask(interruptedTask);
                interruptiblePatchIn.addInterruptedTask(interruptedTask);
                //</editor-fold>

                // replace or new
                int byteRead, remaining = operation.getPatchLength();
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
        } catch (Exception ex) {
            Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (randomAccessOldFile != null) {
                    randomAccessOldFile.close();
                }
                if (interruptiblePatchIn != null) {
                    long byteSkipped = patchIn.skip(interruptiblePatchIn.remaining());
                    if (byteSkipped != interruptiblePatchIn.remaining()) {
                        throw new Exception("Failed to skip remaining bytes in 'interruptiblePatchIn'.");
                    }
                }
                if (tempNewFileOut != null) {
                    tempNewFileOut.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // check new file checksum and length
        if (!operation.getType().equals("new")) {
            String tempNewFileSHA256 = Util.getSHA256String(tempNewFile);
            if (!tempNewFileSHA256.equals(operation.getNewFileChecksum()) || tempNewFile.length() != operation.getNewFileLength()) {
                throw new Exception("Checksum or length does not match (new file): " + tempNewFile.getAbsolutePath() + ", old file path: " + operation.getOldFilePath() + ", expected checksum: " + operation.getNewFileChecksum() + ", actual checksum: " + tempNewFileSHA256 + ", expected length: " + operation.getNewFileLength() + ", actual length: " + tempNewFile.length());
            }
        }
    }

    protected void tryAcquireExclusiveLocks(List<Operation> operations, int startFromFileIndex) throws IOException {
        for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
            Operation _operation = operations.get(i);

            if (_operation.getOldFilePath() != null) {
                if (!Util.tryLock(new File(_operation.getOldFilePath()))) {
                    throw new IOException("Failed to acquire lock on (old file): " + _operation.getOldFilePath());
                }
            }

//            if (_operation.getNewFilePath() != null) {
//                if (!Util.tryLock(new File(_operation.getNewFilePath()))) {
//                    throw new IOException("Failed to acquire lock on (new file): " + _operation.getNewFilePath());
//                }
//            }
        }
    }

    protected void doReplacement(List<Operation> operations, int startFromFileIndex, String patchFileAbsolutePath, Patch patch, float progressOccupied) throws Exception {
        float progressStep = progressOccupied / (float) operations.size();
        progress += startFromFileIndex * progressStep;

        for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
            Operation _operation = operations.get(i);

            log.logPatch(PatchLogWriter.Action.START, i, PatchLogWriter.OperationType.get(_operation.getType()), _operation.getOldFilePath(), _operation.getNewFilePath());

            if (_operation.getType().equals("remove")) {
                listener.patchProgress((int) progress, "Removing " + _operation.getOldFilePath() + " ...");
                new File(_operation.getOldFilePath()).delete();
            } else if (_operation.getType().equals("new")) {
                listener.patchProgress((int) progress, "Copying new file to " + _operation.getNewFilePath() + " ...");
                if (_operation.getFileType().equals("folder")) {
                    File newFolder = new File(_operation.getNewFilePath());
                    if (!newFolder.isDirectory() && !newFolder.mkdirs()) {
                        throw new IOException("Create folder failed: " + _operation.getNewFilePath());
                    }
                } else {
                    File newFile = new File(_operation.getNewFilePath());
                    new File(Util.getFileDirectory(newFile)).mkdirs();
                    newFile.delete();
                    new File(tempDir + File.separator + i).renameTo(newFile);
                }
            } else {
                // patch or replace
                listener.patchProgress((int) progress, "Copying from " + _operation.getOldFilePath() + " to " + _operation.getNewFilePath() + " ...");
                new File(_operation.getNewFilePath()).delete();
                new File(tempDir + File.separator + i).renameTo(new File(_operation.getNewFilePath()));
            }

            log.logPatch(PatchLogWriter.Action.FINISH, i, PatchLogWriter.OperationType.get(_operation.getType()), _operation.getOldFilePath(), _operation.getNewFilePath());
            progress += progressStep;
        }
    }

    public boolean doPatch(int startFromFileIndex) {
        boolean returnResult = true;

        InputStream patchIn = null;
        try {
            String patchFileAbsolutePath = patchFile.getAbsolutePath();
            patchIn = new BufferedInputStream(new FileInputStream(patchFile));

            progress = 0;
            listener.patchProgress((int) progress, "Preparing new patch ...");
            listener.patchEnableCancel(false);
            // header
            checkHeader(patchIn); // 'P' 'A' 'T' 'C' 'H'
            InputStream decompressedPatchIn = getCompressionMethod(patchIn); // compression method
            Patch patch = getXML(decompressedPatchIn); // xml

            List<Operation> operations = patch.getOperations();
            List<ValidationFile> validations = patch.getValidations();

            // start log
            log.logStart(patch.getId(), patch.getVersionFrom(), patch.getVersionTo());

            progress = 5;
            listener.patchProgress((int) progress, "Updating ...");
            listener.patchEnableCancel(true);
            // start patch - patch files and store to temporary directory first
            float progressStep = 70.0F / (float) operations.size();
            progress += startFromFileIndex * progressStep;
            for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
                Operation _operation = operations.get(i);
                doOperation(_operation, decompressedPatchIn, new File(tempDir + File.separator + i));
                progress += progressStep;
            }

            progress = 75;
            listener.patchProgress((int) progress, "Checking the accessibility of all files ...");
            // try acquire locks on all files
            tryAcquireExclusiveLocks(operations, startFromFileIndex);

            progress = 76;
            listener.patchProgress((int) progress, "Replacing old files with new files ...");
            listener.patchEnableCancel(false);
            // all files has patched to temporary directory, replace old files with the new one
            doReplacement(operations, startFromFileIndex, patchFileAbsolutePath, patch, 4.0F);

            progress = 80;
            listener.patchProgress((int) progress, "Validating files ...");
            // validate files
            progressStep = 20.0F / (float) validations.size();
            for (ValidationFile _validationFile : validations) {
                listener.patchProgress((int) progress, "Validating file: " + _validationFile.getFilePath());

                File _file = new File(_validationFile.getFilePath());
                if (_validationFile.getFileLength() == -1) {
                    if (!_file.isDirectory()) {
                        throw new Exception("Folder missed: " + _validationFile.getFilePath());
                    }
                } else {
                    if (!_file.exists()) {
                        throw new Exception("File missed: " + _validationFile.getFilePath());
                    }
                    if (_file.length() != _validationFile.getFileLength()) {
                        throw new Exception("File length not matched, file: " + _validationFile.getFilePath() + ", expected: " + _validationFile.getFileLength() + ", found: " + _file.length());
                    }
                    if (!Util.getSHA256String(_file).equals(_validationFile.getFileChecksum())) {
                        throw new Exception("File checksum incorrect: " + _validationFile.getFilePath());
                    }
                }

                progress += progressStep;
            }

            log.logEnd();
        } catch (Exception ex) {
            returnResult = false;
            Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (patchIn != null) {
                    patchIn.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        listener.patchFinished(returnResult);

        return returnResult;
    }

    public static void main(String[] args) throws IOException {
        PatchLogWriter patchActionLogWriter = new PatchLogWriter(new File("update/action.log"));
        Patcher _patcher = new Patcher(new PatcherListener() {

            @Override
            public void patchProgress(int percentage, String message) {
                System.out.println(percentage + " : " + message);
            }

            @Override
            public void patchFinished(boolean succeed) {
            }

            @Override
            public void patchEnableCancel(boolean enable) {
            }
        }, patchActionLogWriter, new File("1.patch"), new File("update/"));

        boolean patchResult = _patcher.doPatch(0);
        System.out.println(patchResult);

        _patcher.close();
    }
}
