package updater.builder.patch;

import javax.xml.transform.TransformerException;
import updater.builder.util.Util;
import com.nothome.delta.Delta;
import com.nothome.delta.DiffWriter;
import com.nothome.delta.GDiffWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.tukaani.xz.XZOutputStream;
import updater.builder.util.AESKey;
import updater.patch.PatchReadUtil;
import updater.patch.PatchWriteUtil;
import updater.script.InvalidFormatException;
import updater.script.Patch;
import updater.script.Patch.Operation;
import updater.script.Patch.ValidationFile;
import watne.seis720.project.KeySize;
import watne.seis720.project.Mode;
import watne.seis720.project.Padding;
import watne.seis720.project.WatneAES_Implementer;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Creator {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }

    protected Creator() {
    }

    public static void extractXMLFromPatch(File patchFile, File saveToFile, AESKey aesKey, File tempFileForDecryption) throws IOException, InvalidFormatException {
        File _patchFile = patchFile;
        boolean deletePatch = false;

        if (aesKey != null) {
            tempFileForDecryption.delete();

            try {
                WatneAES_Implementer aesCipher = new WatneAES_Implementer();
                aesCipher.setMode(Mode.CBC);
                aesCipher.setPadding(Padding.PKCS5PADDING);
                aesCipher.setKeySize(KeySize.BITS256);
                aesCipher.setKey(aesKey.getKey());
                aesCipher.setInitializationVector(aesKey.getIV());
                aesCipher.decryptFile(patchFile, tempFileForDecryption);
            } catch (Exception ex) {
                throw new IOException("Error occurred when encrypting the patch: " + ex.getMessage());
            }

            _patchFile = tempFileForDecryption;
            _patchFile.deleteOnExit();
            deletePatch = true;
        }

        FileInputStream in = null;
        try {
            in = new FileInputStream(_patchFile);

            PatchReadUtil.readHeader(in);
            InputStream decompressedIn = PatchReadUtil.readCompressionMethod(in);
            Patch patchXML = PatchReadUtil.readXML(decompressedIn);

            Util.writeFile(saveToFile, patchXML.output().getBytes("UTF-8"));
        } catch (TransformerException ex) {
            if (debug) {
                Logger.getLogger(Creator.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (deletePatch) {
                _patchFile.delete();
            }
        }
    }

    public static void createFullPatch(File softwareDirectory, File tempDir, File patch, int patchId, String fromVersion, String fromSubsequentVersion, String toVersion, AESKey aesKey, File tempFileForEncryption) throws IOException {
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
                String sha256 = Util.getSHA256String(_newFile);
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


        int pos = 0;
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
                    fileSHA256 = Util.getSHA256String(_forceFile);
                }

                patchForceFileList.add(_forceFile);
            }

            Operation _operation = new Operation("force", pos, fileLength, fileType, null, null, -1, _forceFile.getAbsolutePath().replace(softwarePath, "").replace(File.separator, "/"), fileSHA256, fileLength);
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
            patchScriptOutput = patchScript.output().getBytes("UTF-8");
        } catch (TransformerException ex) {
            if (debug) {
                Logger.getLogger(Creator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

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
            tempFileForEncryption.delete();

            try {
                WatneAES_Implementer aesCipher = new WatneAES_Implementer();
                aesCipher.setMode(Mode.CBC);
                aesCipher.setPadding(Padding.PKCS5PADDING);
                aesCipher.setKeySize(KeySize.BITS256);
                aesCipher.setKey(aesKey.getKey());
                aesCipher.setInitializationVector(aesKey.getIV());
                aesCipher.encryptFile(patch, tempFileForEncryption);
            } catch (Exception ex) {
                throw new IOException("Error occurred when encrypting the patch: " + ex.getMessage());
            }

            patch.delete();
            tempFileForEncryption.renameTo(patch);
        }
    }

    public static void createPatch(File oldVersion, File newVersion, File tempDir, File patch, int patchId, String fromVersion, String toVersion, AESKey aesKey, File tempFileForEncryption) throws IOException {
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
                String sha256 = Util.getSHA256String(_newFile);
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


        int pos = 0;
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
                fileSHA256 = Util.getSHA256String(_oldFile);
            }

            Operation _operation = new Operation("remove", 0, 0, fileType, _oldFile.getAbsolutePath().replace(oldVersionPath, "").replace(File.separator, "/"), fileSHA256, fileLength, null, null, -1);
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
                    fileSHA256 = Util.getSHA256String(_newFile);
                }

                patchNewFileList.add(_newFile);
            }

            Operation _operation = new Operation("new", pos, fileLength, fileType, null, null, -1, _newFile.getAbsolutePath().replace(newVersionPath, "").replace(File.separator, "/"), fileSHA256, fileLength);
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
                    newFileSHA256 = Util.getSHA256String(_newFile);
                }
                patchPatchFileList.add(diffFile);
                _operation = new Operation("patch", pos, fileLength, "file", _oldFile.getAbsolutePath().replace(oldVersionPath, "").replace(File.separator, "/"), Util.getSHA256String(_oldFile), (int) _oldFile.length(), _newFile.getAbsolutePath().replace(newVersionPath, "").replace(File.separator, "/"), newFileSHA256, newFileLength);
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
                newFileSHA256 = Util.getSHA256String(_newFile);
            }

            patchReplaceFileList.add(_newFile);

            Operation _operation = new Operation("replace", pos, fileLength, "file", _oldFile.getAbsolutePath().replace(oldVersionPath, "").replace(File.separator, "/"), Util.getSHA256String(_oldFile), (int) _oldFile.length(), _newFile.getAbsolutePath().replace(newVersionPath, "").replace(File.separator, "/"), newFileSHA256, newFileLength);
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
            patchScriptOutput = patchScript.output().getBytes("UTF-8");
        } catch (TransformerException ex) {
            if (debug) {
                Logger.getLogger(Creator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

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
            tempFileForEncryption.delete();

            try {
                WatneAES_Implementer aesCipher = new WatneAES_Implementer();
                aesCipher.setMode(Mode.CBC);
                aesCipher.setPadding(Padding.PKCS5PADDING);
                aesCipher.setKeySize(KeySize.BITS256);
                aesCipher.setKey(aesKey.getKey());
                aesCipher.setInitializationVector(aesKey.getIV());
                aesCipher.encryptFile(patch, tempFileForEncryption);
            } catch (Exception ex) {
                throw new IOException("Error occurred when encrypting the patch: " + ex.getMessage());
            }

            patch.delete();
            tempFileForEncryption.renameTo(patch);
        }
    }

    protected static void sortNewFileList(List<OperationRecord> list) {
        Collections.sort(list, new Comparator<OperationRecord>() {

            @Override
            public int compare(OperationRecord o1, OperationRecord o2) {
                return o1.getNewFile().getAbsolutePath().compareTo(o2.getNewFile().getAbsolutePath());
            }
        });
    }

    protected static void sortRemoveFileList(List<OperationRecord> list) {
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

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File _file : files) {
                if (_file.isHidden()) {
                    continue;
                }
                if (_file.isDirectory()) {
                    returnResult.putAll(getAllFiles(_file, rootPath));
                } else {
                    returnResult.put(_file.getAbsolutePath().replace(rootPath, "").replace(File.separator, "/"), _file);
                }
            }
        }
        returnResult.put(file.getAbsolutePath().replace(rootPath, "").replace(File.separator, "/"), file);

        return returnResult;
    }

    protected static boolean compareFile(File oldFile, File newFile) throws IOException {
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

    public static void encryptCatalog(File in, File out, BigInteger mod, BigInteger privateExp) throws IOException {
        RSAPrivateKey privateKey = null;
        try {
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(mod, privateExp);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException ex) {
            if (debug) {
                Logger.getLogger(Creator.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (InvalidKeySpecException ex) {
            if (debug) {
                Logger.getLogger(Creator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // compress
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        GZIPOutputStream gout = new GZIPOutputStream(bout);
        gout.write(Util.readFile(in));
        gout.finish();
        byte[] compressedData = bout.toByteArray();

        // encrypt
        int blockSize = mod.bitLength() / 8;
        byte[] encrypted = Util.rsaEncrypt(privateKey, blockSize, blockSize - 11, compressedData);

        // write to file
        Util.writeFile(out, encrypted);
    }

    public static void decryptCatalog(File in, File out, BigInteger mod, BigInteger publicExp) throws IOException {
        RSAPublicKey publicKey = null;
        try {
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(mod, publicExp);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException ex) {
            if (debug) {
                Logger.getLogger(Creator.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (InvalidKeySpecException ex) {
            if (debug) {
                Logger.getLogger(Creator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // decrypt
        int blockSize = mod.bitLength() / 8;
        byte[] decrypted = Util.rsaDecrypt(publicKey, blockSize, Util.readFile(in));

        // decompress
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayInputStream bin = new ByteArrayInputStream(decrypted);
        GZIPInputStream gin = new GZIPInputStream(bin);

        int byteRead;
        byte[] b = new byte[1024];
        while ((byteRead = gin.read(b)) != -1) {
            bout.write(b, 0, byteRead);
        }
        byte[] decompressedData = bout.toByteArray();

        // write to file
        Util.writeFile(out, decompressedData);
    }
}
