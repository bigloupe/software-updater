package updater.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.UIManager;
import javax.xml.transform.TransformerException;
import updater.script.Client;
import updater.script.InvalidFormatException;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class CommonUtil {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }

    protected CommonUtil() {
    }

    /**
     * Convert byte array to hex string representation. In the output, a to f are in lowercase.
     * @param raw the byte array to convert
     * @return the hex string
     */
    public static String byteArrayToHexString(byte[] raw) {
        if (raw == null) {
            return null;
        }
        byte[] hexCharTable = {
            (byte) '0', (byte) '1', (byte) '2', (byte) '3',
            (byte) '4', (byte) '5', (byte) '6', (byte) '7',
            (byte) '8', (byte) '9', (byte) 'a', (byte) 'b',
            (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'
        };

        byte[] hex = new byte[2 * raw.length];
        int index = 0;

        for (byte b : raw) {
            int v = b & 0xFF;
            hex[index] = hexCharTable[v >>> 4];
            index++;
            hex[index] = hexCharTable[v & 0xF];
            index++;
        }

        String result = null;
        try {
            result = new String(hex, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            // US-ASCII should always exist
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return result;
    }

    /**
     * Convert the hex string to a byte array. This function will not check the validity of the <code>hexString</code>.
     * @param hexString the hex string to convert
     * @return the byte array
     */
    public static byte[] hexStringToByteArray(String hexString) {
        if (hexString == null) {
            return new byte[0];
        }
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Get the SHA-256 digest of a file.
     * @param file the file to digest
     * @return the SHA-256 in hex string representation or null if SHA-256 algorithm not found
     * @throws IOException error occurred when reading the file
     */
    public static String getSHA256String(File file) throws IOException {
        return byteArrayToHexString(getSHA256(file));
    }

    /**
     * Get the SHA-256 digest of a file.
     * @param file the file to digest
     * @return the SHA-256 digest or null if SHA-256 algorithm not found
     * @throws IOException error occurred when reading the file
     */
    public static byte[] getSHA256(File file) throws IOException {
        if (file == null) {
            throw new IOException("argument 'file' cannot be null");
        }
        InputStream fin = null;
        try {
            long fileLength = file.length();
            fin = new FileInputStream(file);

            MessageDigest messageDigest;
            messageDigest = MessageDigest.getInstance("SHA-256");

            int byteRead, cumulateByteRead = 0;
            byte[] b = new byte[8192];
            while ((byteRead = fin.read(b)) != -1) {
                messageDigest.update(b, 0, byteRead);
                cumulateByteRead += byteRead;
                if (cumulateByteRead >= fileLength) {
                    break;
                }
            }

            if (cumulateByteRead != fileLength) {
                throw new IOException("The total number of bytes read does not match the file size. Actual file size: " + fileLength + ", bytes read: " + cumulateByteRead + ", path: " + file.getAbsolutePath());
            }

            return messageDigest.digest();
        } catch (NoSuchAlgorithmException ex) {
            // should have SHA-256
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
            if (fin != null) {
                fin.close();
            }
        }

        return null;
    }

    /**
     * Set UI look & feel to system look & feel.
     */
    public static void setLookAndFeel() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }

    /**
     * If the file is a directory, return the directory path; if the file is not a directory, return the directory path that contain the file.
     * @param file the file
     * @return the file parent path
     */
    public static String getFileDirectory(File file) {
        if (file == null) {
            return null;
        }
        return file.isDirectory() ? file.getAbsolutePath() : getFileDirectory(file.getAbsolutePath());
    }

    /**
     * Assume the filePath is a file path not a directory path.
     * @param filePath the file path
     * @return the file parent path
     */
    public static String getFileDirectory(String filePath) {
        if (filePath == null) {
            return null;
        }
        int pos = filePath.replace(File.separator, "/").lastIndexOf('/');
        return pos != -1 ? filePath.substring(0, pos) : filePath;
    }

    /**
     * Write the string into the file.
     * @param file the file to write to
     * @param content the content to write into the file
     * @throws IOException error occurred when writing the content into the file
     */
    public static void writeFile(File file, String content) throws IOException {
        if (content == null) {
            return;
        }
        if (file == null) {
            throw new IOException("argument 'file' cannot be null");
        }
        writeFile(file, content.getBytes("UTF-8"));
    }

    /**
     * Write the byte array into the file.
     * @param file the file to write to
     * @param content the content to write into the file
     * @throws IOException error occurred when writing the content into the file
     */
    public static void writeFile(File file, byte[] content) throws IOException {
        if (content == null) {
            return;
        }
        if (file == null) {
            throw new IOException("argument 'file' cannot be null");
        }
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            fout.write(content);
        } finally {
            if (fout != null) {
                fout.close();
            }
        }
    }

    /**
     * Copy a file to another location.
     * @param fromFile the file to copy form
     * @param toFile the file/location to copy to
     * @throws IOException error occurred when reading/writing the content from/into the file
     */
    public static void copyFile(File fromFile, File toFile) throws IOException {
        if (fromFile == null || toFile == null) {
            return;
        }
        FileInputStream fromFileStream = null;
        FileOutputStream toFileStream = null;
        FileChannel fromFileChannel = null;
        FileChannel toFileChannel = null;
        try {
            long fromFileLength = fromFile.length();

            fromFileStream = new FileInputStream(fromFile);
            toFileStream = new FileOutputStream(toFile);

            fromFileChannel = fromFileStream.getChannel();
            toFileChannel = toFileStream.getChannel();

            long byteToRead = 0, cumulateByteRead = 0;
            while (cumulateByteRead < fromFileLength) {
                byteToRead = (fromFileLength - cumulateByteRead) > 32768 ? 32768 : (fromFileLength - cumulateByteRead);
                cumulateByteRead += toFileChannel.transferFrom(fromFileChannel, cumulateByteRead, byteToRead);
            }

//            byte[] buf = new byte[32768];
//            while ((byteRead = fromFileStream.read(buf)) != -1) {
//                toFileStream.write(buf, 0, byteRead);
//                cumulateByteRead += byteRead;
//                if (cumulateByteRead >= fromFileLength) {
//                    break;
//                }
//            }

            if (cumulateByteRead != fromFileLength) {
                throw new IOException("The total number of bytes read does not match the file size. Actual file size: " + fromFileLength + ", bytes read: " + cumulateByteRead + ", path: " + fromFile.getAbsolutePath());
            }
        } finally {
            if (fromFileChannel != null) {
                fromFileChannel.close();
            }
            if (toFileChannel != null) {
                toFileChannel.close();
            }
            if (fromFileStream != null) {
                fromFileStream.close();
            }
            if (toFileStream != null) {
                toFileStream.close();
            }
        }
    }

    /**
     * Read the whole file and return the content in byte array.
     * @param file the file to read
     * @return the content of the file in byte array
     * @throws IOException error occurred when reading the content from the file
     */
    public static byte[] readFile(File file) throws IOException {
        if (file == null) {
            return null;
        }
        long fileLength = file.length();
        byte[] content = new byte[(int) fileLength];

        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);

            int byteRead = 0, cumulateByteRead = 0;
            while ((byteRead = fin.read(content, cumulateByteRead, content.length - cumulateByteRead)) != -1) {
                cumulateByteRead += byteRead;
                if (cumulateByteRead >= fileLength) {
                    break;
                }
            }

            if (cumulateByteRead != fileLength) {
                throw new IOException("The total number of bytes read does not match the file size. Actual file size: " + fileLength + ", bytes read: " + cumulateByteRead + ", path: " + file.getAbsolutePath());
            }
        } finally {
            if (fin != null) {
                fin.close();
            }
        }

        return content;
    }

    /**
     * Read the resource file from the jar.
     * @param path the resource path
     * @return the content of the resource file in byte array
     * @throws IOException error occurred when reading the content from the file
     */
    public static byte[] readResourceFile(String path) throws IOException {
        if (path == null) {
            return null;
        }
        byte[] returnResult = null;

        int byteRead = 0;
        byte[] b = new byte[256];
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        InputStream in = null;

        try {
            in = CommonUtil.class.getResourceAsStream(path);
            if (in == null) {
                throw new IOException("Resources not found: " + path);
            }

            while ((byteRead = in.read(b)) != -1) {
                bout.write(b, 0, byteRead);
            }

            returnResult = bout.toByteArray();
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return returnResult;
    }

    /**
     * Validate a version string. Format: [0-9]+(\\.[0-9]+)*
     * @param version the version to check
     * @return true if it is a valid version number, false if not
     */
    protected static boolean validateVersion(String version) {
        if (version == null) {
            return false;
        }
        return version.matches("[0-9]+(\\.[0-9]+)*");
    }

    /**
     * Compare <code>version1</code> and <code>version2</code>. Format: [0-9]{1,3}(\.[0-9]{1,3})*
     * @param version1 version string
     * @param version2 version string to compare to
     * @return 0 if two version are equal, > 0 if <code>version1</code> is larger than <code>version2</code>, < 0 if <code>version1</code> is smaller than <code>version2</code>
     * @throws updater.util.CommonUtil.InvalidVersionException version string is not a valid format
     */
    public static long compareVersion(String version1, String version2) {
        if (!validateVersion(version1) || !validateVersion(version2)) {
            throw new IllegalArgumentException("Valid version number should be [0-9]+(\\.[0-9]+)*, found: " + version1 + ", " + version2);
        }
        String[] version1Parted = version1.split("\\.");
        String[] version2Parted = version2.split("\\.");

        long returnValue = 0;

        for (int i = 0, iEnd = Math.min(version1Parted.length, version2Parted.length); i < iEnd; i++) {
            returnValue += (Integer.parseInt(version1Parted[i]) - Integer.parseInt(version2Parted[i])) * Math.pow(10000, iEnd - i);
        }

        return returnValue;
    }

    /**
     * Get the client script.
     * @param inputPath the default path, can be null
     * @return the result
     * @throws InvalidFormatException the format of the client script or the version number is invalid
     * @throws IOException error occurred when reading the content from the file
     */
    public static GetClientScriptResult getClientScript(String inputPath) throws InvalidFormatException, IOException {
        Client clientScript = null;
        String clientScriptPath = null;

        if (inputPath != null) {
            File inputFile = new File(inputPath);
            if (inputFile.exists()) {
                return new GetClientScriptResult(Client.read(readFile(inputFile)), inputFile.getAbsolutePath());
            } else {
                throw new IOException("Client script file not found: " + inputPath);
            }
        }

        byte[] configPathByte = null;
        try {
            configPathByte = readResourceFile("/config");
        } catch (IOException ex) {
            throw new IOException("File '/config' not found in the jar.");
        }
        String configPath = new String(configPathByte, "US-ASCII").replace("{home}", System.getProperty("user.home") + File.separator).replace("{tmp}", System.getProperty("java.io.tmpdir") + File.separator);

        File configFile = new File(configPath);
        File newConfigFile = new File(getFileDirectory(configFile) + File.separator + configFile.getName() + ".new");

        if (configFile.exists()) {
            try {
                clientScript = Client.read(readFile(configFile));
            } catch (InvalidFormatException ex) {
                // allow this file be incorrect at this stage
            }
            clientScriptPath = configFile.getAbsolutePath();

            if (newConfigFile.exists()) {
                if (clientScript != null) {
                    Client newConfigClientScript = null;

                    try {
                        newConfigClientScript = Client.read(readFile(newConfigFile));
                    } catch (InvalidFormatException ex) {
                        // allow this file be incorrect at this stage
                    }

                    if (newConfigClientScript != null) {
                        long compareVersionResult = compareVersion(newConfigClientScript.getVersion(), clientScript.getVersion());
                        if (compareVersionResult > 0) {
                            configFile.delete();
                            newConfigFile.renameTo(configFile);

                            clientScript = newConfigClientScript;
                            clientScriptPath = newConfigFile.getAbsolutePath();
                        }
                    }
                } else {
                    configFile.delete();
                    newConfigFile.renameTo(configFile);
                    clientScript = Client.read(readFile(configFile));
                }
            }
        } else {
            if (newConfigFile.exists()) {
                newConfigFile.renameTo(configFile);
                clientScript = Client.read(readFile(configFile));
            }
        }

        if (clientScript == null) {
            throw new IOException("Config file not found according to the path stated in '/config'.");
        }

        return new GetClientScriptResult(clientScript, clientScriptPath);
    }

    /**
     * Return result for {@link #getClientScript(java.lang.String)}.
     */
    public static class GetClientScriptResult {

        protected Client clientScript;
        protected String clientScriptPath;

        protected GetClientScriptResult(Client clientScript, String clientScriptPath) {
            this.clientScript = clientScript;
            this.clientScriptPath = clientScriptPath;
        }

        public Client getClientScript() {
            return clientScript;
        }

        public String getClientScriptPath() {
            return clientScriptPath;
        }
    }

    /**
     * Save the client script.
     * @param clientScriptFile the file to save the client script into
     * @param clientScript the client script to save
     * @throws IOException error occurred when writing the content into the file
     * @throws TransformerException the format of the client is invalid
     */
    public static void saveClientScript(File clientScriptFile, Client clientScript) throws IOException, TransformerException {
        if (clientScript == null) {
            return;
        }
        if (clientScriptFile == null) {
            throw new NullPointerException("argument 'clientScriptFile' cannot be null");
        }
        File clientScriptTemp = new File(getFileDirectory(clientScriptFile) + File.separator + clientScriptFile.getName() + ".new");
        writeFile(clientScriptTemp, clientScript.output());
        if (!clientScriptFile.delete() || !clientScriptTemp.renameTo(clientScriptFile)) {
            throw new IOException("Failed to save to script to path: " + clientScriptFile.getAbsolutePath());
        }
    }

    /**
     * Remove all folders and files in the <code>directory</code>. (The <code>directory</code> will not be removed)
     * @param directory the directory to truncate
     * @return true if all folders and files has been removed successfully, false if failed to remove any
     */
    public static boolean truncateFolder(File directory) {
        if (directory == null) {
            return true;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if (!truncateFolderRecursively(file)) {
                    return false;
                }
            } else {
                if (!file.delete()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Remove all folders and files in the <code>directory</code>. (The <code>directory</code> will be removed)
     * It is used by {@link #truncateFolder(java.io.File)}.
     * @param directory the directory to truncate
     * @return true if all folders and files has been removed successfully, false if failed to remove any
     */
    protected static boolean truncateFolderRecursively(File directory) {
        if (directory == null) {
            return true;
        }
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    if (!truncateFolderRecursively(file)) {
                        return false;
                    }
                } else {
                    if (!file.delete()) {
                        return false;
                    }
                }
            }
        }
        return directory.delete();
    }

    /**
     * Do RSA encryption and return the encrypted data.
     * @param key the RSA private key
     * @param blockSize the block size, it should be key size (in bits) divided by 8
     * @param contentBlockSize  the content block size, it should be key size (in bits) divided by 8, then minus 11
     * @param b the data to encrypt
     * @return the encrypted data
     * @throws IOException error occurred when writing to 
     */
    public static byte[] rsaEncrypt(RSAPrivateKey key, int blockSize, int contentBlockSize, byte[] b) {
        if (b == null) {
            return null;
        }
        if (key == null) {
            throw new NullPointerException("argument 'key' cannot be null");
        }
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream(((b.length / contentBlockSize) * blockSize) + (b.length % contentBlockSize == 0 ? 0 : blockSize));

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            for (int i = 0, iEnd = b.length; i < iEnd; i += contentBlockSize) {
                int byteToRead = i + contentBlockSize > iEnd ? iEnd - i : contentBlockSize;
                try {
                    bout.write(cipher.doFinal(b, i, byteToRead));
                } catch (IOException ex) {
                    // should not caught any for ByteArrayOutputStream
                }
            }

            return bout.toByteArray();
        } catch (NoSuchAlgorithmException ex) {
            // it should be included in JCE
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (NoSuchPaddingException ex) {
            // no special padding is specified
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (InvalidKeyException ex) {
            // the key is RSAPrivateKey
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IllegalBlockSizeException ex) {
            // it is handled
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (BadPaddingException ex) {
            // encryption should not have this problem
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return null;
    }

    /**
     * Do RSA decryption and return the decrypted data.
     * @param key the RSA public key
     * @param blockSize the block size, it should be key size (in bits) divided by 8
     * @param b the data to decrypt
     * @return the decrypted data
     * @throws BadPaddingException <code>b</code> is not a valid RSA encrypted data with the <code>key</code>
     */
    public static byte[] rsaDecrypt(RSAPublicKey key, int blockSize, byte[] b) throws BadPaddingException {
        if (b == null) {
            return null;
        }
        if (key == null) {
            throw new NullPointerException("argument 'key' cannot be null");
        }
        byte[] returnResult = null;

        try {
            if (b.length % blockSize != 0) {
                throw new BadPaddingException("Data length is not a multiple of RSA block size. Data length: " + b.length + ", RSA block size: " + blockSize + ", data length % RSA block size: " + b.length % blockSize);
            }

            ByteArrayOutputStream bout = new ByteArrayOutputStream(b.length);

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, key);

            for (int i = 0, iEnd = b.length; i < iEnd; i += blockSize) {
                try {
                    bout.write(cipher.doFinal(b, i, blockSize));
                } catch (IOException ex) {
                    // should not caught any for ByteArrayOutputStream
                }
            }

            returnResult = bout.toByteArray();
        } catch (NoSuchAlgorithmException ex) {
            // it should be included in JCE
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (NoSuchPaddingException ex) {
            // no special padding is specified
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IllegalBlockSizeException ex) {
            // it is checked
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (InvalidKeyException ex) {
            // the key is RSAPublicKey
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    /**
     * Try to acquire exclusive lock on the file.
     * @param file the file to try to acquire lock
     * @return true if acquire succeed, false if not
     */
    public static boolean tryLock(File file) {
        if (file == null) {
            return true;
        }
        FileOutputStream fout = null;
        FileLock lock = null;
        try {
            fout = new FileOutputStream(file, true);
            lock = fout.getChannel().tryLock();
            return lock != null;
        } catch (IOException ex) {
            // if any IOException caught, consider it as failure and no exception is thrown
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
            try {
                if (lock != null) {
                    lock.release();
                }
                if (fout != null) {
                    fout.close();
                }
            } catch (IOException ex) {
                if (debug) {
                    Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return false;
    }

    public static Map<String, File> getAllFiles(File file, String rootPath) {
        Map<String, File> returnResult = new HashMap<String, File>();

        if (file == null) {
            return returnResult;
        }
        String fileRootPath = rootPath != null ? rootPath : file.getAbsolutePath();

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

    public static boolean compareFile(File oldFile, File newFile) throws IOException {
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

            byte[] ob = new byte[32768];
            byte[] nb = new byte[32768];

            int byteToRead, cumulateByteRead = 0;
            while (cumulateByteRead < oldFileLength) {
                byteToRead = (int) (oldFileLength - cumulateByteRead > 32768 ? 32768 : oldFileLength - cumulateByteRead);

                fillBuffer(oldFin, ob, byteToRead);
                fillBuffer(newFin, nb, byteToRead);
                for (int i = 0; i < byteToRead; i++) {
                    if (ob[i] != nb[i]) {
                        return false;
                    }
                }

                cumulateByteRead += byteToRead;
                if (cumulateByteRead >= oldFileLength) {
                    break;
                }
            }

            if (cumulateByteRead != oldFileLength) {
                throw new IOException("The total number of bytes read does not match the file size. Actual file size: " + oldFileLength + ", bytes read: " + cumulateByteRead + ", path: " + oldFile.getAbsolutePath() + " & " + newFile.getAbsolutePath());
            }
        } finally {
            oldFin.close();
            newFin.close();
        }

        return true;
    }

    protected static void fillBuffer(InputStream in, byte[] b, int length) throws IOException {
        if (length > b.length) {
            throw new IllegalArgumentException("argument 'length' is greater than the length of argument 'b'");
        }

        int byteRead, cumulateByteRead = 0;
        while (true) {
            byteRead = in.read(b, cumulateByteRead, length - cumulateByteRead);
            if (byteRead == -1) {
                if (length != cumulateByteRead) {
                    throw new IOException("Reach the end of stream but the total number of bytes read do not meet the requirement. Expected: " + length + ", bytes read: " + cumulateByteRead);
                }
                return;
            }
            cumulateByteRead += byteRead;
            if (cumulateByteRead >= length) {
                break;
            }
        }

        if (length != cumulateByteRead) {
            throw new IOException("The total number of bytes read does not match the requirement. Expected: " + length + ", bytes read: " + cumulateByteRead);
        }
    }
}
