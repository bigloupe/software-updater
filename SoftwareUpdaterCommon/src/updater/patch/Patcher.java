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
import java.util.logging.Level;
import java.util.logging.Logger;
import updater.crypto.AESKey;
import updater.script.InvalidFormatException;
import updater.script.Patch;
import updater.script.Patch.Operation;
import updater.script.Patch.ValidationFile;
import updater.util.CommonUtil;
import updater.util.InterruptibleInputStream;
import updater.util.InterruptibleOutputStream;
import updater.util.SeekableFile;
import watne.seis720.project.AESForFile;
import watne.seis720.project.AESForFileListener;
import watne.seis720.project.KeySize;
import watne.seis720.project.Mode;
import watne.seis720.project.Padding;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Patcher {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }
    protected PatcherListener listener;
    protected PatchLogWriter log;
    protected File tempDir;
    protected String softwareDir;
    //
    private byte[] buf;
    protected float progress;
    //
    protected InterruptibleOutputStream tempNewFileOut;
    protected InterruptibleInputStream interruptiblePatchIn;
    protected SeekableFile seekableRandomAccessOldFile;
    protected AESForFile aesCipher;

    public Patcher(PatcherListener listener, PatchLogWriter log, File softwareDir, File tempDir) throws IOException {
        if (listener == null) {
            throw new NullPointerException("argument 'listener' cannot be null");
        }
        if (log == null) {
            throw new NullPointerException("argument 'log' cannot be null");
        }
        if (softwareDir == null) {
            throw new NullPointerException("argument 'softwareDir' cannot be null");
        }
        if (tempDir == null) {
            throw new NullPointerException("argument 'tempDir' cannot be null");
        }

        this.listener = listener;
        this.log = log;

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
        aesCipher = null;
    }

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
            if (aesCipher != null) {
                aesCipher.pause(pause);
            }
        }
    }

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

        if (operation.getType().equals("remove")) {
            // doOperation will not change/remove all existing 'old files'
            return;
        } else if (operation.getType().equals("new") || operation.getType().equals("force")) {
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
            oldFile = new File(softwareDir + operation.getOldFilePath());
            if (!oldFile.exists()) {
                throw new IOException("Old file not exist: " + softwareDir + operation.getOldFilePath());
            }
            if (!CommonUtil.getSHA256String(oldFile).equals(operation.getOldFileChecksum()) || oldFile.length() != operation.getOldFileLength()) {
                throw new IOException("Checksum or length does not match (old file): " + softwareDir + operation.getOldFilePath());
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

        tempNewFileOut = null;
        interruptiblePatchIn = null;
        RandomAccessFile randomAccessOldFile = null;
        seekableRandomAccessOldFile = null;
        try {
            tempNewFileOut = new InterruptibleOutputStream(new BufferedOutputStream(new FileOutputStream(tempNewFile)));
            interruptiblePatchIn = new InterruptibleInputStream(patchIn, operation.getPatchLength());

            if (operation.getType().equals("patch")) {
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
        } finally {
            CommonUtil.closeQuietly(randomAccessOldFile);
            try {
                if (interruptiblePatchIn != null) {
                    long byteSkipped = patchIn.skip(interruptiblePatchIn.remaining());
                    if (byteSkipped != interruptiblePatchIn.remaining()) {
                        throw new IOException("Failed to skip remaining bytes in 'interruptiblePatchIn'.");
                    }
                }
            } catch (IOException ex) {
                if (debug) {
                    Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            CommonUtil.closeQuietly(tempNewFileOut);
            tempNewFileOut = null;
            interruptiblePatchIn = null;
            seekableRandomAccessOldFile = null;
        }

        // check new file checksum and length
        if (!operation.getType().equals("new")) {
            String tempNewFileSHA256 = CommonUtil.getSHA256String(tempNewFile);
            if (!tempNewFileSHA256.equals(operation.getNewFileChecksum()) || tempNewFile.length() != operation.getNewFileLength()) {
                throw new IOException("Checksum or length does not match (new file): " + tempNewFile.getAbsolutePath() + ", old file path: " + softwareDir + operation.getOldFilePath() + ", expected checksum: " + operation.getNewFileChecksum() + ", actual checksum: " + tempNewFileSHA256 + ", expected length: " + operation.getNewFileLength() + ", actual length: " + tempNewFile.length());
            }
        }
    }

    protected void tryAcquireExclusiveLocks(List<Operation> operations, int startFromFileIndex) throws IOException {
        if (operations == null) {
            throw new NullPointerException("argument 'operations' cannot be null");
        }
        if (startFromFileIndex < 0) {
            throw new IllegalArgumentException("argument 'startFromFileIndex' should >= 0");
        }

        for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
            Operation _operation = operations.get(i);

            if (!_operation.getFileType().equals("folder") && _operation.getOldFilePath() != null) {
                if (!CommonUtil.tryLock(new File(softwareDir + _operation.getOldFilePath()))) {
                    throw new IOException("Failed to acquire lock on (old file): " + softwareDir + _operation.getOldFilePath());
                }
            }
        }
    }

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

        List<Replacement> replacementFailedList = new ArrayList<Replacement>();

        float progressStep = progressOccupied / (float) operations.size();
        progress += startFromFileIndex * progressStep;

        for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
            Operation _operation = operations.get(i);

            log.logPatch(PatchLogWriter.Action.START, i, PatchLogWriter.OperationType.get(_operation.getType()), _operation.getOldFilePath(), _operation.getNewFilePath());

            if (_operation.getType().equals("remove")) {
                listener.patchProgress((int) progress, "Removing " + _operation.getOldFilePath() + " ...");
                new File(softwareDir + _operation.getOldFilePath()).delete();
            } else if (_operation.getType().equals("new") || _operation.getType().equals("force")) {
                listener.patchProgress((int) progress, "Copying new file to " + _operation.getNewFilePath() + " ...");
                if (_operation.getFileType().equals("folder")) {
                    File newFolder = new File(softwareDir + _operation.getNewFilePath());
                    if (!newFolder.isDirectory() && !newFolder.mkdirs()) {
                        throw new IOException("Create folder failed: " + softwareDir + _operation.getNewFilePath());
                    }
                } else {
                    File newFile = new File(softwareDir + _operation.getNewFilePath());
                    new File(CommonUtil.getFileDirectory(newFile)).mkdirs();
                    newFile.delete();

                    if (!new File(tempDir + File.separator + i).renameTo(newFile)) {
                        replacementFailedList.add(new Replacement(softwareDir + _operation.getNewFilePath(), tempDir + File.separator + i));
                    }
                }
            } else {
                // patch or replace
                listener.patchProgress((int) progress, "Copying from " + _operation.getOldFilePath() + " to " + _operation.getNewFilePath() + " ...");
                new File(softwareDir + _operation.getNewFilePath()).delete();
                if (!new File(tempDir + File.separator + i).renameTo(new File(softwareDir + _operation.getNewFilePath()))) {
                    replacementFailedList.add(new Replacement(softwareDir + _operation.getNewFilePath(), tempDir + File.separator + i));
                }
            }

            log.logPatch(PatchLogWriter.Action.FINISH, i, PatchLogWriter.OperationType.get(_operation.getType()), _operation.getOldFilePath(), _operation.getNewFilePath());
            progress += progressStep;
        }

        return replacementFailedList;
    }

    public List<Replacement> doPatch(File patchFile, int patchId, int startFromFileIndex, AESKey aesKey, File tempFileForDecryption) throws IOException {
        if (patchFile == null) {
            throw new NullPointerException("argument 'patchFile' cannot be null");
        }
        if (startFromFileIndex < 0) {
            throw new IllegalArgumentException("argument 'startFromFileIndex' should >= 0");
        }
        if (aesKey != null && tempFileForDecryption == null) {
            throw new NullPointerException("argument 'tempFileForDecryption' cannot be null while argument 'aesKey' is not null");
        }

        if (!patchFile.exists() || patchFile.isDirectory()) {
            throw new IOException("patch file not exist or not a file");
        }

        List<Replacement> replacementFailedList = null;

        float decryptProgress = 0;
        float prepareProgress = 5;
        float updateProgress = 60;
        float checkAccessibilityProgress = 0;
        float replaceFilesProgress = 5;
        float validateFilesProgress = 30;


        float stageMinimumProgress = 0;
        progress = stageMinimumProgress;


        File _patchFile = patchFile;
        if (aesKey != null) {
            decryptProgress = 25;
            updateProgress = 40;
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

                aesCipher = new AESForFile();
                aesCipher.setListener(aesForFileListener);
                aesCipher.setMode(Mode.CBC);
                aesCipher.setPadding(Padding.PKCS5PADDING);
                aesCipher.setKeySize(KeySize.BITS256);
                aesCipher.setKey(aesKey.getKey());
                aesCipher.setInitializationVector(aesKey.getIV());
                aesCipher.decryptFile(patchFile, tempFileForDecryption);
            } catch (Exception ex) {
                throw new IOException(ex);
            } finally {
                aesCipher = null;
            }

            _patchFile = tempFileForDecryption;
        }


        stageMinimumProgress += decryptProgress;
        progress = stageMinimumProgress;


        InputStream patchIn = null;
        try {
            patchIn = new BufferedInputStream(new FileInputStream(_patchFile));


            listener.patchProgress((int) progress, "Preparing new patch ...");
            listener.patchEnableCancel(false);
            // header
            PatchReadUtil.readHeader(patchIn);
            InputStream decompressedPatchIn = PatchReadUtil.readCompressionMethod(patchIn);
            Patch patch = PatchReadUtil.readXML(decompressedPatchIn);

            List<Operation> operations = patch.getOperations();
            List<ValidationFile> validations = patch.getValidations();

            // start log
            log.logStart(patchId, patch.getVersionFrom(), patch.getVersionTo());


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
//            tryAcquireExclusiveLocks(operations, startFromFileIndex);


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
                listener.patchProgress((int) progress, "Validating file: " + _validationFile.getFilePath());

                File _file = new File(softwareDir + _validationFile.getFilePath());
                String _validationFileAbsPath = _file.getAbsolutePath();
                for (Replacement _replacement : replacementFailedList) {
                    if (new File(_replacement.getDestination()).getAbsolutePath().equals(_validationFileAbsPath)) {
                        _file = new File(_replacement.getNewFilePath());
                    }
                }

                if (_validationFile.getFileLength() == -1) {
                    if (!_file.isDirectory()) {
                        throw new IOException("Folder missed: " + softwareDir + _validationFile.getFilePath());
                    }
                } else {
                    if (!_file.exists()) {
                        throw new IOException("File missed: " + softwareDir + _validationFile.getFilePath());
                    }
                    if (_file.length() != _validationFile.getFileLength()) {
                        throw new IOException("File length not matched, file: " + softwareDir + _validationFile.getFilePath() + ", expected: " + _validationFile.getFileLength() + ", found: " + _file.length());
                    }
                    if (!CommonUtil.getSHA256String(_file).equals(_validationFile.getFileChecksum())) {
                        throw new IOException("File checksum incorrect: " + softwareDir + _validationFile.getFilePath());
                    }
                }

                progress += progressStep;
            }


            stageMinimumProgress += validateFilesProgress;
            progress = stageMinimumProgress;


            listener.patchProgress(100, "Finished.");
            log.logEnd();
        } catch (InvalidFormatException ex) {
            if (debug) {
                Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
            CommonUtil.closeQuietly(patchIn);
        }

        listener.patchFinished();
        return replacementFailedList;
    }

    public static class Replacement {

        protected String destination;
        protected String newFilePath;

        protected Replacement(String destination, String newFilePath) {
            if (destination == null) {
                throw new NullPointerException("argument 'destination' cannot be null");
            }
            if (newFilePath == null) {
                throw new NullPointerException("argument 'newFilePath' cannot be null");
            }
            this.destination = destination;
            this.newFilePath = newFilePath;
        }

        public String getDestination() {
            return this.destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public String getNewFilePath() {
            return this.newFilePath;
        }

        public void setNewFilePath(String newFilePath) {
            this.newFilePath = newFilePath;
        }
    }
}