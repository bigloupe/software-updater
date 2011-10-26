package updater.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileLock;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
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
        return file.isDirectory() ? file.getAbsolutePath() : getFileDirectory(file.getAbsolutePath());
    }

    /**
     * Assume the filePath is a file path not a directory path.
     * @param filePath the file path
     * @return the file parent path
     */
    public static String getFileDirectory(String filePath) {
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
        writeFile(file, content.getBytes("UTF-8"));
    }

    /**
     * Write the byte array into the file.
     * @param file the file to write to
     * @param content the content to write into the file
     * @throws IOException error occurred when writing the content into the file
     */
    public static void writeFile(File file, byte[] content) throws IOException {
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
        FileInputStream fromFileStream = null;
        FileOutputStream toFileStream = null;
        try {
            long fromFileLength = fromFile.length();

            fromFileStream = new FileInputStream(fromFile);
            toFileStream = new FileOutputStream(toFile);

            int byteRead = 0, cumulateByteRead = 0;
            byte[] buf = new byte[32768];
            while ((byteRead = fromFileStream.read(buf)) != -1) {
                toFileStream.write(buf, 0, byteRead);
                cumulateByteRead += byteRead;
                if (cumulateByteRead >= fromFileLength) {
                    break;
                }
            }

            if (cumulateByteRead != fromFileLength) {
                throw new IOException("The total number of bytes read does not match the file size. Actual file size: " + fromFileLength + ", bytes read: " + cumulateByteRead + ", path: " + fromFile.getAbsolutePath());
            }
        } finally {
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
     * Compare <code>version1</code> and <code>version2</code>. Format: [0-9]{1,3}(\.[0-9]{1,3})*
     * @param version1 version string
     * @param version2 version string to compare to
     * @return 0 if two version are equal, > 0 if <code>version1</code> is larger than <code>version2</code>, < 0 if <code>version1</code> is smaller than <code>version2</code>
     * @throws updater.util.CommonUtil.InvalidVersionException version string is not a valid format
     */
    public static long compareVersion(String version1, String version2) throws InvalidVersionException {
        String[] version1Parted = version1.split("\\.");
        String[] version2Parted = version2.split("\\.");

        long returnValue = 0;

        for (int i = 0, iEnd = Math.min(version1Parted.length, version2Parted.length); i < iEnd; i++) {
            try {
                returnValue += (Integer.parseInt(version1Parted[i]) - Integer.parseInt(version2Parted[i])) * Math.pow(10000, iEnd - i);
            } catch (NumberFormatException ex) {
                throw new InvalidVersionException("Valid version number should be [0-9]{1,3}(\\.[0-9]{1,3})*, found: " + version1 + ", " + version2);
            }
        }

        return returnValue;
    }

    /**
     * Exception for {@link #compareVersion(java.lang.String, java.lang.String)}.
     */
    public static class InvalidVersionException extends Exception {

        private static final long serialVersionUID = 1L;

        public InvalidVersionException() {
            super();
        }

        public InvalidVersionException(String message) {
            super(message);
        }

        public InvalidVersionException(String message, Throwable cause) {
            super(message, cause);
        }

        public InvalidVersionException(Throwable cause) {
            super(cause);
        }
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
                        try {
                            long compareVersionResult = compareVersion(newConfigClientScript.getVersion(), clientScript.getVersion());
                            if (compareVersionResult > 0) {
                                configFile.delete();
                                newConfigFile.renameTo(configFile);

                                clientScript = newConfigClientScript;
                                clientScriptPath = newConfigFile.getAbsolutePath();
                            }
                        } catch (InvalidVersionException ex) {
                            throw new InvalidFormatException(ex);
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
        File clientScriptTemp = new File(getFileDirectory(clientScriptFile) + File.separator + clientScriptFile.getName() + ".new");
        writeFile(clientScriptTemp, clientScript.output());
        if (!clientScriptFile.delete() || !clientScriptTemp.renameTo(clientScriptFile)) {
            throw new IOException("Failed to save to script to path: " + clientScriptFile.getAbsolutePath());
        }
    }

    public static boolean truncateFolder(File directory) {
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

    protected static boolean truncateFolderRecursively(File directory) {
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

    public static byte[] rsaEncrypt(RSAPrivateKey key, int blockSize, int contentBlockSize, byte[] b) throws IOException {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream(((b.length / contentBlockSize) * blockSize) + (b.length % contentBlockSize == 0 ? 0 : blockSize));

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            for (int i = 0, iEnd = b.length; i < iEnd; i += contentBlockSize) {
                int byteToRead = i + contentBlockSize > iEnd ? iEnd - i : contentBlockSize;
                bout.write(cipher.doFinal(b, i, byteToRead));
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

    public static byte[] rsaDecrypt(RSAPublicKey key, int blockSize, byte[] b) throws IOException {
        byte[] returnResult = null;

        try {
            if (b.length % blockSize != 0) {
                throw new IOException("Data length is not a multiple of RSA block size. Data length: " + b.length + ", RSA block size: " + blockSize + ", data length % RSA block size: " + b.length % blockSize);
            }

            ByteArrayOutputStream bout = new ByteArrayOutputStream(b.length);

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, key);

            for (int i = 0, iEnd = b.length; i < iEnd; i += blockSize) {
                bout.write(cipher.doFinal(b, i, blockSize));
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
        } catch (BadPaddingException ex) {
            throw new IOException(ex);
        }

        return returnResult;
    }

    public static boolean tryLock(File file) {
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

    /**
     * Simple wrapper class to do 'primitive data passing by reference'.
     */
    public static class ObjectReference<T> {

        protected T obj;

        public ObjectReference(T obj) {
            this.obj = obj;
        }

        public T getObj() {
            return obj;
        }

        public void setObj(T obj) {
            this.obj = obj;
        }
    }
}
