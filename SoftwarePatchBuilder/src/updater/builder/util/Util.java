package updater.builder.util;

import java.io.ByteArrayOutputStream;
import java.security.PrivateKey;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import updater.util.CommonUtil;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Util extends CommonUtil {

    protected Util() {
    }

    public static byte[] rsaEncrypt(PrivateKey key, int blockSize, int contentBlockSize, byte[] b) {
        byte[] returnResult = null;

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream(((b.length / contentBlockSize) * blockSize) + (b.length % contentBlockSize == 0 ? 0 : blockSize));

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            for (int i = 0, iEnd = b.length; i < iEnd; i += contentBlockSize) {
                int byteToRead = i + contentBlockSize > iEnd ? iEnd - i : contentBlockSize;
                bout.write(cipher.doFinal(b, i, byteToRead));
            }

            returnResult = bout.toByteArray();
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.WARNING, null, ex);
        }

        return returnResult;
    }
}
