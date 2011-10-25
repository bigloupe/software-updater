package updater.builder.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import updater.util.CommonUtil;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Util extends CommonUtil {

    protected Util() {
    }

    public static byte[] rsaEncrypt(PrivateKey key, int blockSize, int contentBlockSize, byte[] b) throws IOException {
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
            System.err.println(ex);
        } catch (NoSuchPaddingException ex) {
            System.err.println(ex);
        } catch (InvalidKeyException ex) {
            System.err.println(ex);
        } catch (IllegalBlockSizeException ex) {
            System.err.println(ex);
        } catch (BadPaddingException ex) {
            System.err.println(ex);
        }

        return null;
    }

    public static byte[] rsaDecrypt(PublicKey key, int blockSize, byte[] b) throws IOException {
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

            return bout.toByteArray();
        } catch (NoSuchAlgorithmException ex) {
            System.err.println(ex);
        } catch (NoSuchPaddingException ex) {
            System.err.println(ex);
        } catch (IllegalBlockSizeException ex) {
            System.err.println(ex);
        } catch (InvalidKeyException ex) {
            System.err.println(ex);
        } catch (BadPaddingException ex) {
            throw new IOException(ex);
        }
        return null;
    }
}
