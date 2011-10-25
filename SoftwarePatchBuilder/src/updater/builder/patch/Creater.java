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
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;
import updater.script.Patch;
import updater.script.Patch.Operation;
import updater.script.Patch.ValidationFile;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Creater {

    protected Creater() {
    }

    public static void createFullPatch(File softwareDirectory, File tempDir, File patch, int patchId, String fromVersion, String fromSubsequentVersion, String toVersion) {
    }

    public static void createPatch(File oldVersion, File newVersion, File tempDir, File patch, int patchId, String fromVersion, String toVersion) throws IOException {
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

        // add validations list first
        for (String _filePath : newVersionFiles.keySet()) {
            File _newFile = newVersionFiles.get(_filePath);
            ValidationFile validationFile;
            if (_newFile.isDirectory()) {
                validationFile = new ValidationFile(_filePath, "", -1);
            } else {
                validationFile = new ValidationFile(_filePath, Util.getSHA256String(_newFile), (int) _newFile.length());
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
                fileSHA256 = Util.getSHA256String(_newFile);

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
                patchPatchFileList.add(diffFile);
                _operation = new Operation("patch", pos, fileLength, "file", _oldFile.getAbsolutePath().replace(oldVersionPath, "").replace(File.separator, "/"), Util.getSHA256String(_oldFile), (int) _oldFile.length(), _newFile.getAbsolutePath().replace(newVersionPath, "").replace(File.separator, "/"), Util.getSHA256String(_newFile), newFileLength);
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

            patchReplaceFileList.add(_newFile);

            Operation _operation = new Operation("replace", pos, fileLength, "file", _oldFile.getAbsolutePath().replace(oldVersionPath, "").replace(File.separator, "/"), Util.getSHA256String(_oldFile), (int) _oldFile.length(), _newFile.getAbsolutePath().replace(newVersionPath, "").replace(File.separator, "/"), Util.getSHA256String(_newFile), newFileLength);
            operations.add(_operation);

            pos += fileLength;
        }

//        if (operations.isEmpty()) {
//            return false;
//        }
        patchScript.setOperations(operations);


//        System.out.println(patchScript.output());
        byte[] patchScriptOutput = null;
        try {
            patchScriptOutput = patchScript.output().getBytes("UTF-8");
        } catch (TransformerException ex) {
            System.err.println(ex);
        }
        int patchScriptOutputLength = patchScriptOutput.length;

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(patch);

            // header
            fout.write('P');
            fout.write('A');
            fout.write('T');
            fout.write('C');
            fout.write('H');

            // compression method: LAMA2/XZ
            fout.write(1);

            XZOutputStream xzOut = new XZOutputStream(fout, new LZMA2Options());

            // XML size, max
            xzOut.write((patchScriptOutputLength >> 16) & 0xff);
            xzOut.write((patchScriptOutputLength >> 8) & 0xff);
            xzOut.write(patchScriptOutputLength & 0xff);

            // XML content, max 16MiB
            xzOut.write(patchScriptOutput);

            // patch content
            for (File _file : patchNewFileList) {
                outputFileToStream(_file, xzOut);
            }
            for (File _file : patchPatchFileList) {
                outputFileToStream(_file, xzOut);
                _file.delete();
            }
            for (File _file : patchReplaceFileList) {
                outputFileToStream(_file, xzOut);
            }

            xzOut.finish();
        } finally {
            if (fout != null) {
                fout.close();
            }
        }
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

    protected static void outputFileToStream(File fromFile, OutputStream toStream) throws IOException {
        FileInputStream fin = null;
        try {
            long fileLength = fromFile.length();

            fin = new FileInputStream(fromFile);

            byte[] b = new byte[8096];
            int byteRead, cumulativeByteRead = 0;
            while ((byteRead = fin.read(b)) != -1) {
                toStream.write(b, 0, byteRead);
                cumulativeByteRead += byteRead;

                if (cumulativeByteRead >= fileLength) {
                    break;
                }
            }

            if (cumulativeByteRead >= fileLength) {
                throw new IOException("Number of bytes read not equals to the cumulative number of bytes read.");
            }

            fin.close();
        } finally {
            if (fin != null) {
                fin.close();
            }
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
        PrivateKey privateKey = null;
        try {
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(mod, privateExp);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException ex) {
            System.err.println(ex);
        } catch (InvalidKeySpecException ex) {
            System.err.println(ex);
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
        PublicKey publicKey = null;
        try {
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(mod, publicExp);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException ex) {
            System.err.println(ex);
        } catch (InvalidKeySpecException ex) {
            System.err.println(ex);
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
