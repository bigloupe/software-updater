package updater.builder.patch;

import updater.builder.util.Util;
import com.nothome.delta.Delta;
import com.nothome.delta.DiffWriter;
import com.nothome.delta.GDiffWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public static boolean createPatch(File oldVersion, File newVersion, File tempDir, File patch, int patchId, String fromVersion, String toVersion) throws Exception {
        if (!oldVersion.exists() || !oldVersion.isDirectory()) {
            throw new Exception("Directory for old verison not exist or not a directory.");
        }
        if (!newVersion.exists() || !newVersion.isDirectory()) {
            throw new Exception("Directory for new verison not exist or not a directory.");
        }

        List<Operation> operations = new ArrayList<Operation>();
        List<ValidationFile> validations = new ArrayList<ValidationFile>();

        Patch patchScript = new Patch(patchId, fromVersion, toVersion, operations, validations);

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

        for (String _filePath : newVersionFiles.keySet()) {
            File _newFile = newVersionFiles.get(_filePath);
            ValidationFile validationFile;
            if (_newFile.isDirectory()) {
                validationFile = new ValidationFile(_filePath, "", -1);
            } else {
                validationFile = new ValidationFile(_filePath, Util.getSHA256(_newFile), (int) _newFile.length());
            }
            validations.add(validationFile);
        }
        patchScript.setValidations(validations);

        Iterator<String> iterator = newVersionFiles.keySet().iterator();
        while (iterator.hasNext()) {
            String _filePath = iterator.next();
            File _newFile = newVersionFiles.get(_filePath);
            File _oldFile = oldVersionFiles.get(_filePath);

            if (_oldFile == null) {
                newFileList.add(new OperationRecord(null, _newFile));
            } else {
                if (!_newFile.isDirectory()) {
                    patchFileList.add(new OperationRecord(_oldFile, _newFile));
                }
                oldVersionFiles.remove(_filePath);
            }

            iterator.remove();
        }

        iterator = oldVersionFiles.keySet().iterator();
        while (iterator.hasNext()) {
            String _filePath = iterator.next();
            File _oldFile = oldVersionFiles.get(_filePath);

            removeFileList.add(new OperationRecord(_oldFile, null));

            iterator.remove();
        }


        int pos = 0;
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
                fileSHA256 = Util.getSHA256(_oldFile);
            }

            Operation _operation = new Operation("remove", 0, 0, fileType, _oldFile.getAbsolutePath().replace((CharSequence) oldVersionPath, (CharSequence) "").replace(File.separator, "/"), fileSHA256, fileLength, null, null, -1);
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
                fileSHA256 = Util.getSHA256(_newFile);

                patchNewFileList.add(_newFile);
            }

            Operation _operation = new Operation("new", pos, fileLength, fileType, null, null, -1, _newFile.getAbsolutePath().replace((CharSequence) newVersionPath, (CharSequence) "").replace(File.separator, "/"), fileSHA256, fileLength);
            operations.add(_operation);

            pos += fileLength;
        }

        int count = 0;
        Delta delta = new Delta();
        for (OperationRecord record : patchFileList) {
            File _oldFile = record.getOldFile();
            File _newFile = record.getNewFile();

            if (compareFile(_oldFile, _newFile)) {
                continue;
            }

            File diffFile = new File(tempDir + File.separator + Integer.toString(count));
            FileOutputStream fout = new FileOutputStream(diffFile);
            DiffWriter diffOut = new GDiffWriter(fout);
            delta.compute(_oldFile, _newFile, diffOut);
            fout.close();

            int fileLength = (int) diffFile.length();
            int newFileLength = (int) _newFile.length();

            Operation _operation;
            if (fileLength > newFileLength) {
                replaceFileList.add(record);
                continue;
            } else {
                patchPatchFileList.add(diffFile);
                _operation = new Operation("patch", pos, fileLength, "file", _oldFile.getAbsolutePath().replace((CharSequence) oldVersionPath, (CharSequence) "").replace(File.separator, "/"), Util.getSHA256(_oldFile), (int) _oldFile.length(), _newFile.getAbsolutePath().replace(newVersionPath, "").replace(File.separator, "/"), Util.getSHA256(_newFile), newFileLength);
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

            Operation _operation = new Operation("replace", pos, fileLength, "file", _oldFile.getAbsolutePath().replace((CharSequence) oldVersionPath, (CharSequence) "").replace(File.separator, "/"), Util.getSHA256(_oldFile), (int) _oldFile.length(), _newFile.getAbsolutePath().replace(newVersionPath, "").replace(File.separator, "/"), Util.getSHA256(_newFile), newFileLength);
            operations.add(_operation);

            pos += fileLength;
        }

        if (operations.isEmpty()) {
            return false;
        }
        patchScript.setOperations(operations);


        System.out.println(patchScript.output());
        byte[] patchScriptOutput = patchScript.output().getBytes("UTF-8");
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

            // XML size
            xzOut.write((patchScriptOutputLength >> 16) & 0xff);
            xzOut.write((patchScriptOutputLength >> 8) & 0xff);
            xzOut.write(patchScriptOutputLength & 0xff);

            // XML content
            xzOut.write(patchScriptOutput);

            // patch content
            for (File _file : patchNewFileList) {
                if (!outputFileToStream(_file, xzOut)) {
                    throw new Exception("Error occurred when creating the patch.");
                }
            }
            for (File _file : patchPatchFileList) {
                if (!outputFileToStream(_file, xzOut)) {
                    throw new Exception("Error occurred when creating the patch.");
                }
                _file.delete();
            }
            for (File _file : patchReplaceFileList) {
                if (!outputFileToStream(_file, xzOut)) {
                    throw new Exception("Error occurred when creating the patch.");
                }
            }

            xzOut.finish();
        } finally {
            if (fout != null) {
                fout.close();
            }
        }

        return true;
    }

    public static class OperationRecord {

        protected File oldFile;
        protected File newFile;

        public OperationRecord(File oldFile, File newFile) {
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

    protected static boolean outputFileToStream(File fromFile, OutputStream toStream) {
        boolean returnResult = false;

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

            fin.close();

            returnResult = true;
        } catch (Exception ex) {
            returnResult = false;
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException ex1) {
                Logger.getLogger(Creater.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

        return returnResult;
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

    public static String createRSAKey(int keyLength) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keyLength);
            KeyPair keyPair = keyPairGenerator.genKeyPair();

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec publicKeySpec = keyFactory.getKeySpec(keyPair.getPublic(), RSAPublicKeySpec.class);
            RSAPrivateKeySpec privateKeySpec = keyFactory.getKeySpec(keyPair.getPrivate(), RSAPrivateKeySpec.class);

            return Util.byteArrayToHexString(privateKeySpec.getModulus().toByteArray()) + ";" + Util.byteArrayToHexString(privateKeySpec.getPrivateExponent().toByteArray()) + ";" + Util.byteArrayToHexString(publicKeySpec.getPublicExponent().toByteArray());
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(Creater.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Creater.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static boolean makeXMLForGetCatalogTest(File in, File out, BigInteger mod, BigInteger privateExp) {
        try {
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(mod, privateExp);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

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
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
//        String rsaKey = createRSAKey(2048);
//        System.out.println(rsaKey);

//        try {
//            System.out.println(createPatch(new File("old/"), new File("new/"), new File("patch_temp/"), new File("1.patch"), 1, "0.0.5", "0.0.6"));
//        } catch (Exception ex) {
//            Logger.getLogger(Creater.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        System.out.println(Util.getSHA256(new File("1.patch")));


//        String modulesString = "0092b826d59bad50af9bcbca27fc6a6b74c63068790906484ecc9b5520e0246e5fe853b842a49d3e95d448497d28e616d1924ca48e7ccb7e9e1a27f9cba4c0767d566dcd24c7e25338570a6e2ab19a934d0f903556bd26d897e4bf16735fed32e6dc904eef438a133053eb93a081c338e64a8a84622906901806e4571bbb830c9d0c993f527f1004e99abc8fd96e1376235754a9289d534f6cc678f5b643e29f7c39fe64c9ccd31d43c1624d452c3fa2662d255c48502457a517e670f542ca3be46f50faa24b976779f4bc222cd01a6e8713a9b45b57dd117f1952bb31c886c84aa0961aff24eac140fc08a9338c37109ea4dc82f1b74d00a3fc9cbae78bc68caf";
//        String publicExponentString = "010001";
//        String privateExponentString = "0090641d0be1ae9a96887bf192928e64fc4243c7bd3e0d69c1eb08ffa9600d5a76969d35dc98468c1e4611720973e3a51750a48eda0fa4f11245698c23471b8640e97b1c0613950013954d9587fccbc42575a89565acb77b37590e59d8e7d1f7634e33d30b136be26090666a1def36a25bb98642ac9bf5727fc2e09b7d96776d43ee27eda1dbc465ed23148842f425c89814b69b19fd4b344d7160bae32383ba08d3a88b3a7f7356a543eb56156ee6098e62be8be54d121a1967e2676f493e4ad1adeec409cacb5d37bade5a58b80b6ac2136e756c9eba9a421864a2dfd112f289455424bd5a965ed91e8b10421984dc534199e2a1129c5808ed19ca1bd269f1c9";
        String modulesString = "0080ac742891f8ba0d59dcc96b464e2245e53a9b29f8219aa0b683ad10007247ced6d74b7bef2a6b0555ec22735827b2b9dfe94664d492a723ad78d6d97d1c9b19ade1225edc060eaced684436ce221659c7e8320bc2bf5ddcdbe6751b0f476066437ccc50ea0e5afafb6a59581df509145d34aa4d0541f500f09868686f5681a509bf58feda73b35326f816b60205550783d628e5e61b24e37198349e416f09ef7579f6f25b5725d54df44017e256b1c7060f0c5ba5f3dd162e26fc5fbfcf4294ee261124737b1cdc3024dc2be62c8ebd89c8766bfaf3606a9e7aefa4fd41758498441fe69a967005c66df3ac0551d7b04910c6a9fa272aa6d081defbc2db174f";
        String publicExponentString = "010001";
        String privateExponentString = "45fa8429d4494b161bbb21a7bfd29a7d1ccfa4b74c852a0d2175b7572e86f85a9b28f79a6d55ca625a7a53ba1b456bc3feec65264d1d7cdcc069299f9a95461ccf1dd38d7767abef8c25da835bd3da07f5da67ed517ab5d779987a33bf397849e58627b011bac0ec227392278413515ecbd9ea8c7cc1843780a1c296998698769825cd7ac298f5a468af873e2e30eb94cf867086742d0b8d1fd9ab7efc7ce3f07a855fe280e8714c963c8436a20fbaf81f874a6714da4699a75cb5c7e2fa0546038f8a8134661a25ce30ff37d73bd94dee33e7bdc6425729e2fd71bdb938a2f5cd7caf56eca8f7ccb8ea320b20610ffeae7f5c8380da62dca4d7964ded34b731";

        System.out.println(makeXMLForGetCatalogTest(new File("RemoteContentTest_getCatalog.xml"), new File("RemoteContentTest_getCatalog_manipulated.xml"), new BigInteger(modulesString, 16), new BigInteger(privateExponentString, 16)));

    }
}
