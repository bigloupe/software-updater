package updater.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
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
 * Common Utilities.
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
            throw new NullPointerException("argument 'raw' cannot be null");
        }

        byte[] hexCharTable = {
            (byte) '0', (byte) '1', (byte) '2', (byte) '3',
            (byte) '4', (byte) '5', (byte) '6', (byte) '7',
            (byte) '8', (byte) '9', (byte) 'a', (byte) 'b',
            (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'
        };

        int i = 0, byteInt = 0;
        byte[] hexBytes = new byte[2 * raw.length];
        for (byte b : raw) {
            byteInt = b & 0xFF;

            hexBytes[i] = hexCharTable[byteInt >>> 4];
            i++;
            hexBytes[i] = hexCharTable[byteInt & 0xF];
            i++;
        }

        String result = null;
        try {
            result = new String(hexBytes, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            // US-ASCII should always exist
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return result;
    }

    /**
     * Convert the hex string to a byte array. The length of the {@code hexString} should be multiple of 2.
     * @param hexString the hex string to convert
     * @return the byte array
     * @throws IllegalArgumentException length of {@code hexString} is not multiple of 2
     */
    public static byte[] hexStringToByteArray(String hexString) {
        if (hexString == null) {
            throw new NullPointerException("argument 'hexString' cannot be null");
        }

        int len = hexString.length();
        if ((len & 0x1) != 0) {
            throw new IllegalArgumentException("length of argument 'hexString' should be a multiple of 2");
        }

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
            throw new NullPointerException("argument 'file' cannot be null");
        }

        InputStream fin = null;
        try {
            long fileLength = file.length();
            fin = new FileInputStream(file);

            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            int byteRead, cumulateByteRead = 0;
            byte[] b = new byte[32768];
            while ((byteRead = fin.read(b)) != -1) {
                messageDigest.update(b, 0, byteRead);

                cumulateByteRead += byteRead;
                if (cumulateByteRead >= fileLength) {
                    break;
                }
            }

            if (cumulateByteRead != fileLength) {
                throw new IOException(String.format("The total number of bytes read does not match the file size. Actual file size: %1$d, bytes read: %2$d, path: %3$s",
                        fileLength, cumulateByteRead, file.getAbsolutePath()));
            }

            return messageDigest.digest();
        } catch (NoSuchAlgorithmException ex) {
            // should have SHA-256
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
            closeQuietly(fin);
        }

        return null;
    }

    /**
     * Set UI look & feel to system look & feel.
     * @throws Exception error occurred when setting look & feel
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
            throw new NullPointerException("argument 'file' cannot be null");
        }
        if (file.isDirectory()) {
            return file.getAbsolutePath();
        } else {
            String filePath = file.getAbsolutePath();
            int pos = filePath.replace(File.separator, "/").lastIndexOf('/');
            return pos != -1 ? filePath.substring(0, pos) : filePath;
        }
    }

    /**
     * Write the string into the file.
     * @param file the file to write to
     * @param content the content to write into the file
     * @throws IOException error occurred when writing the content into the file
     */
    public static void writeFile(File file, String content) throws IOException {
        if (file == null) {
            throw new NullPointerException("argument 'file' cannot be null");
        }
        if (content == null) {
            throw new NullPointerException("argument 'content' cannot be null");
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
        if (file == null) {
            throw new NullPointerException("argument 'file' cannot be null");
        }
        if (content == null) {
            throw new NullPointerException("argument 'content' cannot be null");
        }
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            fout.write(content);
        } finally {
            closeQuietly(fout);
        }
    }

    /**
     * Copy a file to another location.
     * @param fromFile the file to copy form
     * @param toFile the file/location to copy to
     * @throws IOException error occurred when reading/writing the content from/into the file
     */
    public static void copyFile(File fromFile, File toFile) throws IOException {
        if (fromFile == null) {
            throw new NullPointerException("argument 'fromFile' cannot be null");
        }
        if (toFile == null) {
            throw new NullPointerException("argument 'toFile' cannot be null");
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

            if (cumulateByteRead != fromFileLength) {
                throw new IOException(String.format("The total number of bytes read does not match the file size. Actual file size: %1$d, bytes read: %2$d, path: %3$s",
                        fromFileLength, cumulateByteRead, fromFile.getAbsolutePath()));
            }
        } finally {
            closeQuietly(fromFileChannel);
            closeQuietly(toFileChannel);
            closeQuietly(fromFileStream);
            closeQuietly(toFileStream);
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
            throw new NullPointerException("argument 'file' cannot be null");
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
                throw new IOException(String.format("The total number of bytes read does not match the file size. Actual file size: %1$d, bytes read: %2$d, path: %3$s",
                        fileLength, cumulateByteRead, file.getAbsolutePath()));
            }
        } finally {
            closeQuietly(fin);
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
            throw new NullPointerException("argument 'path' cannot be null");
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        InputStream in = null;
        try {
            in = CommonUtil.class.getResourceAsStream(path);
            if (in == null) {
                throw new IOException("Resources not found: " + path);
            }

            int byteRead = 0;
            byte[] b = new byte[8096];

            while ((byteRead = in.read(b)) != -1) {
                bout.write(b, 0, byteRead);
            }
        } finally {
            closeQuietly(in);
        }

        return bout.toByteArray();
    }

    /**
     * Validate a version string. Format: [0-9]+(\\.[0-9]+)*
     * @param version the version to check
     * @return true if it is a valid version number, false if not
     */
    protected static boolean validateVersion(String version) {
        if (version == null) {
            throw new NullPointerException("argument 'version' cannot be null");
        }
        return version.matches("[0-9]+(\\.[0-9]+)*");
    }

    /**
     * Compare {@code version1} and {@code version2}. Format: [0-9]{1,3}(\.[0-9]{1,3})*
     * @param version1 version string
     * @param version2 version string to compare to
     * @return 0 if two version are equal, > 0 if {@code version1} is larger than {@code version2}, < 0 if {@code version1} is smaller than {@code version2}
     * @throws IllegalArgumentException version string is not a valid format
     */
    public static long compareVersion(String version1, String version2) {
        if (!validateVersion(version1) || !validateVersion(version2)) {
            throw new IllegalArgumentException(String.format("Valid version number should be [0-9]+(\\.[0-9]+)*, found: %1$d, %2$d", version1, version2));
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
     * @param inputPath the default path, null means not specified
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

        /**
         * The client script.
         */
        protected Client clientScript;
        /**
         * The file path of the {@link #clientScript}.
         */
        protected String clientScriptPath;

        /**
         * Constructor.
         * @param clientScript the client script.
         * @param clientScriptPath the file path of the {@code clientScript}.
         */
        protected GetClientScriptResult(Client clientScript, String clientScriptPath) {
            this.clientScript = clientScript;
            this.clientScriptPath = clientScriptPath;
        }

        /**
         * Get the client script.
         * @return the client script
         */
        public Client getClientScript() {
            return clientScript;
        }

        /**
         * Get the path of the client script, {@link #clientScript}.
         * @return the file path
         */
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
        if (clientScriptFile == null) {
            throw new NullPointerException("argument 'clientScriptFile' cannot be null");
        }
        if (clientScript == null) {
            throw new NullPointerException("argument 'clientScript' cannot be null");
        }

        File clientScriptTemp = new File(getFileDirectory(clientScriptFile) + File.separator + clientScriptFile.getName() + ".new");
        writeFile(clientScriptTemp, clientScript.output());
        clientScriptFile.delete();
        if (!clientScriptTemp.renameTo(clientScriptFile)) {
            throw new IOException("Failed to save to script to path: " + clientScriptFile.getAbsolutePath());
        }
    }

    /**
     * Remove all folders and files in the {@code directory}. (The {@code directory} will not be removed)
     * @param directory the directory to truncate
     * @return true if all folders and files has been removed successfully, false if failed to remove any
     */
    public static boolean truncateFolder(File directory) {
        if (directory == null) {
            throw new NullPointerException("argument 'directory' cannot be null");
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return false;
        }
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
     * Remove all folders and files in the {@code directory}. (The {@code directory} will be removed)
     * It is used by {@link #truncateFolder(java.io.File)}.
     * @param directory the directory to truncate
     * @return true if all folders and files has been removed successfully, false if failed to remove any
     */
    protected static boolean truncateFolderRecursively(File directory) {
        if (directory == null) {
            throw new NullPointerException("argument 'directory' cannot be null");
        }

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files == null) {
                return false;
            }
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
     */
    public static byte[] rsaEncrypt(RSAPrivateKey key, int blockSize, int contentBlockSize, byte[] b) {
        if (key == null) {
            throw new NullPointerException("argument 'key' cannot be null");
        }
        if (b == null) {
            throw new NullPointerException("argument 'b' cannot be null");
        }
        if (blockSize <= 0) {
            throw new IllegalArgumentException("argument 'blockSize' should > 0");
        }
        if (contentBlockSize <= 0) {
            throw new IllegalArgumentException("argument 'contentBlockSize' should > 0");
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream(((b.length / contentBlockSize) * blockSize) + (b.length % contentBlockSize == 0 ? 0 : blockSize));
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            for (int i = 0, iEnd = b.length; i < iEnd; i += contentBlockSize) {
                int byteToRead = i + contentBlockSize > iEnd ? iEnd - i : contentBlockSize;
                try {
                    bout.write(cipher.doFinal(b, i, byteToRead));
                } catch (IOException ex) {
                    // should not catch any for ByteArrayOutputStream
                }
            }
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

        return bout.toByteArray();
    }

    /**
     * Do RSA decryption and return the decrypted data.
     * @param key the RSA public key
     * @param blockSize the block size, it should be key size (in bits) divided by 8
     * @param b the data to decrypt
     * @return the decrypted data
     * @throws BadPaddingException {@code b} is not a valid RSA encrypted data with the {@code key}
     */
    public static byte[] rsaDecrypt(RSAPublicKey key, int blockSize, byte[] b) throws BadPaddingException {
        if (key == null) {
            throw new NullPointerException("argument 'key' cannot be null");
        }
        if (b == null) {
            throw new NullPointerException("argument 'b' cannot be null");
        }
        if (blockSize <= 0) {
            throw new IllegalArgumentException("argument 'blockSize' should > 0");
        }

        if (b.length % blockSize != 0) {
            throw new BadPaddingException(String.format("Data length is not a multiple of RSA block size. Data length: %1$d, RSA block size: %2$d, data length % RSA block size: %3$d",
                    b.length, blockSize, b.length % blockSize));
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream(b.length);
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, key);

            for (int i = 0, iEnd = b.length; i < iEnd; i += blockSize) {
                try {
                    bout.write(cipher.doFinal(b, i, blockSize));
                } catch (IOException ex) {
                    // should not catch any for ByteArrayOutputStream
                }
            }
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

        return bout.toByteArray();
    }

    /**
     * Get the {@link java.security.interfaces.RSAPublicKey} by the modulus and public exponent.
     * @param modulus the modulus
     * @param publicExponent the public exponent
     * @return the key
     * @throws InvalidKeySpecException the modulus or exponent is invalid
     */
    public static RSAPublicKey getPublicKey(BigInteger modulus, BigInteger publicExponent) throws InvalidKeySpecException {
        if (modulus == null) {
            throw new NullPointerException("argument 'modulus' cannot be null");
        }
        if (publicExponent == null) {
            throw new NullPointerException("argument 'publicExponent' cannot be null");
        }

        RSAPublicKey returnKey = null;
        try {
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, publicExponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            returnKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException ex) {
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return returnKey;
    }

    /**
     * Get the {@link java.security.interfaces.RSAPrivateKey} by the modulus and private exponent.
     * @param modulus the modulus
     * @param privateExponent the private exponent
     * @return the key
     * @throws InvalidKeySpecException the modulus or exponent is invalid
     */
    public static RSAPrivateKey getPrivateKey(BigInteger modulus, BigInteger privateExponent) throws InvalidKeySpecException {
        if (modulus == null) {
            throw new NullPointerException("argument 'modulus' cannot be null");
        }
        if (privateExponent == null) {
            throw new NullPointerException("argument 'privateExponent' cannot be null");
        }

        RSAPrivateKey returnKey = null;
        try {
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(modulus, privateExponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            returnKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException ex) {
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return returnKey;
    }

    /**
     * Try to acquire exclusive lock on the file.
     * @param file the file to try to acquire lock
     * @return true if acquire succeed, false if not
     */
    public static boolean tryLock(File file) {
        if (file == null) {
            throw new NullPointerException("argument 'file' cannot be null");
        }

        FileOutputStream fout = null;
        FileLock lock = null;
        try {
            fout = new FileOutputStream(file, true);
            try {
                lock = fout.getChannel().tryLock();
            } catch (OverlappingFileLockException ex) {
                throw new IOException(ex);
            }
            return lock != null;
        } catch (IOException ex) {
            // if any IOException caught, consider it as failure and no exception is thrown
            if (debug) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
            releaseLockQuietly(lock);
            closeQuietly(fout);
        }

        return false;
    }

    /**
     * Search through the directory and find all sub-folders, files and those in its sub-folders recursively. 
     * This function will skip when there is not enough right to access the folder.
     * @param directory the directory to search through to get files
     * @param rootPath the path to use to replace the map key in return result
     * @return a map with the key be the relative path of the file to the {@code directory} (start with "/") and value be the file
     */
    public static Map<String, File> getAllFiles(File directory, String rootPath) {
        if (directory == null) {
            throw new NullPointerException("argument 'directory' cannot be null");
        }
        if (rootPath == null) {
            throw new NullPointerException("argument 'rootPath' cannot be null");
        }

        Map<String, File> returnResult = new HashMap<String, File>();

        String fileRootPath = rootPath != null ? rootPath : directory.getAbsolutePath();

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
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
        }
        returnResult.put(directory.getAbsolutePath().replace(fileRootPath, "").replace(File.separator, "/"), directory);

        return returnResult;
    }

    /**
     * Compare the content of two files byte by byte.
     * @param file1 the first file
     * @param file2 the second file
     * @return true if the content of two files are identical, false if not
     * @throws IOException error occurred when reading the {@code file1} or {@code file2}
     */
    public static boolean compareFile(File file1, File file2) throws IOException {
        if (file1 == null) {
            throw new NullPointerException("argument 'file1' cannot be null");
        }
        if (file2 == null) {
            throw new NullPointerException("argument 'file2' cannot be null");
        }

        long oldFileLength = file1.length();
        long newFileLength = file2.length();

        if (oldFileLength != newFileLength) {
            return false;
        }

        FileInputStream fin1 = null;
        FileInputStream fin2 = null;
        try {
            fin1 = new FileInputStream(file1);
            fin2 = new FileInputStream(file2);

            byte[] b1 = new byte[32768];
            byte[] b2 = new byte[32768];

            int byteToRead, cumulateByteRead = 0;
            while (cumulateByteRead < oldFileLength) {
                byteToRead = (int) (oldFileLength - cumulateByteRead > 32768 ? 32768 : oldFileLength - cumulateByteRead);

                fillBuffer(fin1, b1, byteToRead);
                fillBuffer(fin2, b2, byteToRead);
                for (int i = 0; i < byteToRead; i++) {
                    if (b1[i] != b2[i]) {
                        return false;
                    }
                }

                cumulateByteRead += byteToRead;
                if (cumulateByteRead >= oldFileLength) {
                    break;
                }
            }

            if (cumulateByteRead != oldFileLength) {
                throw new IOException(String.format("The total number of bytes read does not match the file size. Actual file size: %1$d, bytes read: %2$d, path: %3$s & %4$s",
                        oldFileLength, cumulateByteRead, file1.getAbsolutePath(), file2.getAbsolutePath()));
            }
        } finally {
            closeQuietly(fin1);
            closeQuietly(fin2);
        }

        return true;
    }

    /**
     * Fill the buffer {@code b} with specified {@code length} from the stream {@code in}.
     * @param in the stream to read from
     * @param b the buffer to fill-in
     * @param length the length to fill-in the buffer {@code b}
     * @throws IOException error occurred when read from stream or reach the end of stream but the {@code length} not fulfilled
     */
    protected static void fillBuffer(InputStream in, byte[] b, int length) throws IOException {
        if (in == null) {
            throw new NullPointerException("argument 'in' cannot be null");
        }
        if (b == null) {
            throw new NullPointerException("argument 'b' cannot be null");
        }
        if (length > b.length) {
            throw new IllegalArgumentException("argument 'length' is greater than the length of argument 'b'");
        }
        if (length <= 0) {
            return;
        }

        int byteRead, cumulateByteRead = 0;
        while (true) {
            byteRead = in.read(b, cumulateByteRead, length - cumulateByteRead);
            if (byteRead == -1) {
                if (length != cumulateByteRead) {
                    throw new IOException(String.format("Reach the end of stream but the total number of bytes read do not meet the requirement. Expected: %1$d, bytes read: %2$d", length, cumulateByteRead));
                }
                return;
            }

            cumulateByteRead += byteRead;
            if (cumulateByteRead >= length) {
                break;
            }
        }

        if (length != cumulateByteRead) {
            throw new IOException(String.format("The total number of bytes read does not match the requirement. Expected: %1$d, bytes read: %2$d", length, cumulateByteRead));
        }
    }

    /**
     * Close the stream quietly without throwing any IO exception.
     * @param closeable the stream to close, accept null
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ex) {
                if (debug) {
                    Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Release the file lock quietly without throwing any IO exception.
     * @param fileLock the file lock
     */
    public static void releaseLockQuietly(FileLock fileLock) {
        if (fileLock != null) {
            try {
                fileLock.release();
            } catch (IOException ex) {
                if (debug) {
                    Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Truncate a file to zero size.
     * @param file the file to truncate
     * @return true if the file exist and truncated, false if error occurred 
     */
    public static boolean truncateFile(File file) {
        if (file == null) {
            throw new NullPointerException("argument 'file' cannot be null");
        }

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
        } catch (FileNotFoundException ex) {
            return false;
        } finally {
            closeQuietly(fout);
        }

        return true;
    }
}
