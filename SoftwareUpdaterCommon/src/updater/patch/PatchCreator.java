package updater.patch;

import javax.xml.transform.TransformerException;
import com.nothome.delta.Delta;
import com.nothome.delta.DiffWriter;
import com.nothome.delta.GDiffWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tukaani.xz.XZOutputStream;
import updater.crypto.AESKey;
import updater.script.Patch;
import updater.script.Patch.Operation;
import updater.script.Patch.ValidationFile;
import updater.util.CommonUtil;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchCreator {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }

    protected PatchCreator() {
    }

    public static void createFullPatch(File softwareDirectory, File tempDir, File patch, int patchId, String fromVersion, String fromSubsequentVersion, String toVersion, AESKey aesKey, File tempFileForEncryption) throws IOException {
        if (softwareDirectory == null) {
            throw new NullPointerException("argument 'softwareDirectory' cannot be null");
        }
        if (tempDir == null) {
            throw new NullPointerException("argument 'tempDir' cannot be null");
        }
        if (patch == null) {
            throw new NullPointerException("argument 'patch' cannot be null");
        }
        if (aesKey != null && tempFileForEncryption == null) {
            throw new NullPointerException("argument 'tempFileForEncryption' cannot be null while argument 'aesKey' is not null");
        }

        if (!softwareDirectory.exists() || !softwareDirectory.isDirectory()) {
            throw new IOException("Directory not exist or not a directory.");
        }

        List<Operation> operations = new ArrayList<Operation>();
        List<ValidationFile> validations = new ArrayList<ValidationFile>();

        Patch patchScript = new Patch(patchId,
                fromVersion, fromSubsequentVersion, toVersion,
                null, null, -1,
                null, null, null,
                operations, validations);

        List<OperationRecord> forceFileList = new ArrayList<OperationRecord>();


        String softwarePath = softwareDirectory.getAbsolutePath();
        if (!softwarePath.endsWith(File.separator)) {
            softwarePath += File.separator;
        }


        Map<String, File> softwareFiles = getAllFiles(softwareDirectory, softwarePath);
        softwareFiles.remove(softwareDirectory.getAbsolutePath().replace(File.separator, "/"));

        // to prevent generate checksum repeatedly
        Map<String, String> softwareFilesChecksumMap = new HashMap<String, String>();

        // add validations list first
        for (String _filePath : softwareFiles.keySet()) {
            File _newFile = softwareFiles.get(_filePath);
            ValidationFile validationFile;
            if (_newFile.isDirectory()) {
                validationFile = new ValidationFile(_filePath, "", -1);
            } else {
                String sha256 = CommonUtil.getSHA256String(_newFile);
                softwareFilesChecksumMap.put(_filePath, sha256);
                validationFile = new ValidationFile(_filePath, sha256, (int) _newFile.length());
            }
            validations.add(validationFile);
        }
        patchScript.setValidations(validations);

        // process operations list
        Iterator<String> iterator = softwareFiles.keySet().iterator();
        while (iterator.hasNext()) {
            String _filePath = iterator.next();
            File _forceFile = softwareFiles.get(_filePath);

            forceFileList.add(new OperationRecord(null, _forceFile));

            iterator.remove();
        }


        sortNewFileList(forceFileList);


        int pos = 0, operationIdCounter = 1;
        // record those file with their content needed to put into the patch
        List<File> patchForceFileList = new ArrayList<File>();

        for (OperationRecord record : forceFileList) {
            File _forceFile = record.getNewFile();

            int fileLength = 0;
            String fileType = "folder";
            String fileSHA256 = "";
            if (!_forceFile.isDirectory()) {
                fileLength = (int) _forceFile.length();
                fileType = "file";
                fileSHA256 = softwareFilesChecksumMap.get(_forceFile.getAbsolutePath());
                if (fileSHA256 == null) {
                    fileSHA256 = CommonUtil.getSHA256String(_forceFile);
                }

                patchForceFileList.add(_forceFile);
            }

            Operation _operation = new Operation(operationIdCounter, "force", pos, fileLength, fileType, null, null, -1, _forceFile.getAbsolutePath().replace(softwarePath, "").replace(File.separator, "/"), fileSHA256, fileLength);
            operationIdCounter++;
            operations.add(_operation);

            pos += fileLength;
        }

        patchScript.setOperations(operations);


//        try {
//            System.out.println(patchScript.output());
//        } catch (TransformerException ex) {
//            Logger.getLogger(Creator.class.getName()).log(Level.SEVERE, null, ex);
//        }
        byte[] patchScriptOutput = null;
        try {
            patchScriptOutput = patchScript.output();
        } catch (TransformerException ex) {
            if (debug) {
                Logger.getLogger(PatchCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // why not use PatchPacker here?
        // here will not copy the new file to another folder for packing but instead directly read the new file to the patch
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(patch);

            PatchWriteUtil.writeHeader(fout);
            XZOutputStream xzOut = (XZOutputStream) PatchWriteUtil.writeCompressionMethod(fout, PatchWriteUtil.Compression.LZMA2);
            PatchWriteUtil.writeXML(xzOut, patchScriptOutput);

            // patch content
            for (File _file : patchForceFileList) {
                PatchWriteUtil.writePatch(_file, xzOut);
            }

            xzOut.finish();
        } finally {
            if (fout != null) {
                fout.close();
            }
        }

        if (aesKey != null) {
            PatchWriteUtil.encrypt(aesKey, patch, tempFileForEncryption);

            patch.delete();
            tempFileForEncryption.renameTo(patch);
        }
    }

    public static void createPatch(File oldVersion, File newVersion, File tempDir, File patch, int patchId, String fromVersion, String toVersion, AESKey aesKey, File tempFileForEncryption) throws IOException {
        if (oldVersion == null) {
            throw new NullPointerException("argument 'oldVersion' cannot be null");
        }
        if (newVersion == null) {
            throw new NullPointerException("argument 'newVersion' cannot be null");
        }
        if (tempDir == null) {
            throw new NullPointerException("argument 'tempDir' cannot be null");
        }
        if (patch == null) {
            throw new NullPointerException("argument 'patch' cannot be null");
        }
        if (aesKey != null && tempFileForEncryption == null) {
            throw new NullPointerException("argument 'tempFileForEncryption' cannot be null while argument 'aesKey' is not null");
        }

        if (!oldVersion.exists() || !oldVersion.isDirectory()) {
            throw new IOException("Directory of old verison not exist or not a directory.");
        }
        if (!newVersion.exists() || !newVersion.isDirectory()) {
            throw new IOException("Directory of new verison not exist or not a directory.");
        }

        List<Operation> operations = new ArrayList<Operation>();
        List<ValidationFile> validations = new ArrayList<ValidationFile>();

        Patch patchScript = new Patch(patchId,
                fromVersion, null, toVersion,
                null, null, -1,
                null, null, null,
                operations, validations);

        List<OperationRecord> newFileList = new ArrayList<OperationRecord>();
        List<OperationRecord> removeFileList = new ArrayList<OperationRecord>();
        List<OperationRecord> patchFileList = new ArrayList<OperationRecord>();
        List<OperationRecord> replaceFileList = new ArrayList<OperationRecord>();


        String oldVersionPath = oldVersion.getAbsolutePath();
        String newVersionPath = newVersion.getAbsolutePath();
        if (!oldVersionPath.endsWith(File.separator)) {
            oldVersionPath += File.separator;
        }
        if (!newVersionPath.endsWith(File.separator)) {
            newVersionPath += File.separator;
        }


        Map<String, File> oldVersionFiles = getAllFiles(oldVersion, oldVersionPath);
        Map<String, File> newVersionFiles = getAllFiles(newVersion, newVersionPath);
        oldVersionFiles.remove(oldVersion.getAbsolutePath().replace(File.separator, "/"));
        newVersionFiles.remove(newVersion.getAbsolutePath().replace(File.separator, "/"));

        // to prevent generate checksum repeatedly
        Map<String, String> newVersionFilesChecksumMap = new HashMap<String, String>();

        // add validations list first
        for (String _filePath : newVersionFiles.keySet()) {
            File _newFile = newVersionFiles.get(_filePath);
            ValidationFile validationFile;
            if (_newFile.isDirectory()) {
                validationFile = new ValidationFile(_filePath, "", -1);
            } else {
                String sha256 = CommonUtil.getSHA256String(_newFile);
                newVersionFilesChecksumMap.put(_filePath, sha256);
                validationFile = new ValidationFile(_filePath, sha256, (int) _newFile.length());
            }
            validations.add(validationFile);
        }
        patchScript.setValidations(validations);

        // process operations list
        Iterator<String> iterator = newVersionFiles.keySet().iterator();
        while (iterator.hasNext()) {
            String _filePath = iterator.next();
            File _newFile = newVersionFiles.get(_filePath);
            File _oldFile = oldVersionFiles.get(_filePath);

            // if no old file found, then it is new file
            if (_oldFile == null) {
                newFileList.add(new OperationRecord(null, _newFile));
            } else {
                boolean oldFileIsDirectory = _oldFile.isDirectory();
                boolean newFileIsDirectory = _newFile.isDirectory();
                if (oldFileIsDirectory == newFileIsDirectory) {
                    // only patch if it is not a directory
                    if (!_newFile.isDirectory()) {
                        patchFileList.add(new OperationRecord(_oldFile, _newFile));
                    }
                } else {
                    // one is file and one is directory, remove the old and add back the new
                    removeFileList.add(new OperationRecord(_oldFile, null));
                    newFileList.add(new OperationRecord(null, _newFile));
                }
                oldVersionFiles.remove(_filePath);
            }

            iterator.remove();
        }

        // at this stage, newVersionFiles should be empty, left files in oldVersionFiles waiting for remove
        iterator = oldVersionFiles.keySet().iterator();
        while (iterator.hasNext()) {
            String _filePath = iterator.next();
            File _oldFile = oldVersionFiles.get(_filePath);

            removeFileList.add(new OperationRecord(_oldFile, null));

            iterator.remove();
        }


        sortNewFileList(newFileList);
        sortRemoveFileList(removeFileList);


        int pos = 0, operationIdCounter = 1;
        // three list that record those file with their content needed to put into the patch
        List<File> patchNewFileList = new ArrayList<File>();
        List<File> patchPatchFileList = new ArrayList<File>();
        List<File> patchReplaceFileList = new ArrayList<File>();

        for (OperationRecord record : removeFileList) {
            File _oldFile = record.getOldFile();

            int fileLength = 0;
            String fileType = "folder";
            String fileSHA256 = "";
            if (!_oldFile.isDirectory()) {
                fileLength = (int) _oldFile.length();
                fileType = "file";
                fileSHA256 = CommonUtil.getSHA256String(_oldFile);
            }

            Operation _operation = new Operation(operationIdCounter, "remove", 0, 0, fileType, _oldFile.getAbsolutePath().replace(oldVersionPath, "").replace(File.separator, "/"), fileSHA256, fileLength, null, null, -1);
            operationIdCounter++;
            operations.add(_operation);
        }

        for (OperationRecord record : newFileList) {
            File _newFile = record.getNewFile();

            int fileLength = 0;
            String fileType = "folder";
            String fileSHA256 = "";
            if (!_newFile.isDirectory()) {
                fileLength = (int) _newFile.length();
                fileType = "file";
                fileSHA256 = newVersionFilesChecksumMap.get(_newFile.getAbsolutePath());
                if (fileSHA256 == null) {
                    fileSHA256 = CommonUtil.getSHA256String(_newFile);
                }

                patchNewFileList.add(_newFile);
            }

            Operation _operation = new Operation(operationIdCounter, "new", pos, fileLength, fileType, null, null, -1, _newFile.getAbsolutePath().replace(newVersionPath, "").replace(File.separator, "/"), fileSHA256, fileLength);
            operationIdCounter++;
            operations.add(_operation);

            pos += fileLength;
        }

        int count = 0;
        Delta delta = new Delta();
        for (OperationRecord record : patchFileList) {
            File _oldFile = record.getOldFile();
            File _newFile = record.getNewFile();

            // two file are identical
            if (compareFile(_oldFile, _newFile)) {
                continue;
            }

            // get delta/diff
            File diffFile = new File(tempDir + File.separator + Integer.toString(count));
            diffFile.deleteOnExit();
            FileOutputStream fout = new FileOutputStream(diffFile);
            DiffWriter diffOut = new GDiffWriter(fout);
            delta.compute(_oldFile, _newFile, diffOut);
            fout.close();

            int fileLength = (int) diffFile.length();
            int newFileLength = (int) _newFile.length();

            Operation _operation;
            if (fileLength > newFileLength) {
                // if the patched file is larger than the new file (very rare), don't patch it, use replace instead
                replaceFileList.add(record);
                continue;
            } else {
                String newFileSHA256 = newVersionFilesChecksumMap.get(_newFile.getAbsolutePath());
                if (newFileSHA256 == null) {
                    newFileSHA256 = CommonUtil.getSHA256String(_newFile);
                }
                patchPatchFileList.add(diffFile);
                _operation = new Operation(operationIdCounter, "patch", pos, fileLength, "file", _oldFile.getAbsolutePath().replace(oldVersionPath, "").replace(File.separator, "/"), CommonUtil.getSHA256String(_oldFile), (int) _oldFile.length(), _newFile.getAbsolutePath().replace(newVersionPath, "").replace(File.separator, "/"), newFileSHA256, newFileLength);
                operationIdCounter++;
            }
            operations.add(_operation);

            pos += fileLength;
            count++;
        }

        for (OperationRecord record : replaceFileList) {
            File _oldFile = record.getOldFile();
            File _newFile = record.getNewFile();

            int newFileLength = (int) _newFile.length();
            int fileLength = newFileLength;
            String newFileSHA256 = newVersionFilesChecksumMap.get(_newFile.getAbsolutePath());
            if (newFileSHA256 == null) {
                newFileSHA256 = CommonUtil.getSHA256String(_newFile);
            }

            patchReplaceFileList.add(_newFile);

            Operation _operation = new Operation(operationIdCounter, "replace", pos, fileLength, "file", _oldFile.getAbsolutePath().replace(oldVersionPath, "").replace(File.separator, "/"), CommonUtil.getSHA256String(_oldFile), (int) _oldFile.length(), _newFile.getAbsolutePath().replace(newVersionPath, "").replace(File.separator, "/"), newFileSHA256, newFileLength);
            operationIdCounter++;
            operations.add(_operation);

            pos += fileLength;
        }

//        if (operations.isEmpty()) {
//            return false;
//        }
        patchScript.setOperations(operations);


//        try {
//            System.out.println(patchScript.output());
//        } catch (TransformerException ex) {
//            Logger.getLogger(Creator.class.getName()).log(Level.SEVERE, null, ex);
//        }
        byte[] patchScriptOutput = null;
        try {
            patchScriptOutput = patchScript.output();
        } catch (TransformerException ex) {
            if (debug) {
                Logger.getLogger(PatchCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // why not use PatchPacker here?
        // here will not copy the new file to another folder for packing but instead directly read the new file to the patch
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(patch);

            PatchWriteUtil.writeHeader(fout);
            XZOutputStream xzOut = (XZOutputStream) PatchWriteUtil.writeCompressionMethod(fout, PatchWriteUtil.Compression.LZMA2);
            PatchWriteUtil.writeXML(xzOut, patchScriptOutput);

            // patch content
            for (File _file : patchNewFileList) {
                PatchWriteUtil.writePatch(_file, xzOut);
            }
            for (File _file : patchPatchFileList) {
                PatchWriteUtil.writePatch(_file, xzOut);
                _file.delete();
            }
            for (File _file : patchReplaceFileList) {
                PatchWriteUtil.writePatch(_file, xzOut);
            }

            xzOut.finish();
        } finally {
            if (fout != null) {
                fout.close();
            }
        }

        if (aesKey != null) {
            PatchWriteUtil.encrypt(aesKey, patch, tempFileForEncryption);

            patch.delete();
            tempFileForEncryption.renameTo(patch);
        }
    }

    protected static void sortNewFileList(List<OperationRecord> list) {
        if (list == null) {
            return;
        }
        Collections.sort(list, new Comparator<OperationRecord>() {

            @Override
            public int compare(OperationRecord o1, OperationRecord o2) {
                return o1.getNewFile().getAbsolutePath().compareTo(o2.getNewFile().getAbsolutePath());
            }
        });
    }

    protected static void sortRemoveFileList(List<OperationRecord> list) {
        if (list == null) {
            return;
        }
        Collections.sort(list, new Comparator<OperationRecord>() {

            @Override
            public int compare(OperationRecord o1, OperationRecord o2) {
                return o2.getOldFile().getAbsolutePath().compareTo(o1.getOldFile().getAbsolutePath());
            }
        });
    }

    protected static class OperationRecord {

        protected File oldFile;
        protected File newFile;

        protected OperationRecord(File oldFile, File newFile) {
            this.oldFile = oldFile;
            this.newFile = newFile;
        }

        public File getOldFile() {
            return oldFile;
        }

        public File getNewFile() {
            return newFile;
        }
    }

    protected static Map<String, File> getAllFiles(File file, String rootPath) {
        Map<String, File> returnResult = new HashMap<String, File>();

        if (file == null) {
            return returnResult;
        }
        String fileRootPath = rootPath == null ? file.getAbsolutePath() : rootPath;

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File _file : files) {
                if (_file.isHidden()) {
                    continue;
                }
                if (_file.isDirectory()) {
                    returnResult.putAll(getAllFiles(_file, fileRootPath));
                } else {
                    returnResult.put(_file.getAbsolutePath().replace(fileRootPath, "").replace(File.separator, "/"), _file);
                }
            }
        }
        returnResult.put(file.getAbsolutePath().replace(fileRootPath, "").replace(File.separator, "/"), file);

        return returnResult;
    }

    protected static boolean compareFile(File oldFile, File newFile) throws IOException {
        if (oldFile == null && newFile == null) {
            return true;
        }

        if (oldFile == null) {
            throw new NullPointerException("argument 'oldFile' cannot be null");
        }
        if (newFile == null) {
            throw new NullPointerException("argument 'newFile' cannot be null");
        }

        long oldFileLength = oldFile.length();
        long newFileLength = newFile.length();

        if (oldFileLength != newFileLength) {
            return false;
        }

        FileInputStream oldFin = null;
        FileInputStream newFin = null;
        try {
            oldFin = new FileInputStream(oldFile);
            newFin = new FileInputStream(newFile);

            byte[] ob = new byte[8192];
            byte[] nb = new byte[8192];

            int oldFinRead, newFinRead, cumulativeByteRead = 0;
            while ((oldFinRead = oldFin.read(ob)) != -1 && (newFinRead = newFin.read(nb)) != -1 && oldFinRead == newFinRead) {
                for (int i = 0; i < oldFinRead; i++) {
                    if (ob[i] != nb[i]) {
                        return false;
                    }
                }
                cumulativeByteRead += oldFinRead;

                if (cumulativeByteRead >= oldFileLength) {
                    break;
                }
            }

            if (cumulativeByteRead != oldFileLength) {
                return false;
            }
        } finally {
            oldFin.close();
            newFin.close();
        }

        return true;
    }
}
