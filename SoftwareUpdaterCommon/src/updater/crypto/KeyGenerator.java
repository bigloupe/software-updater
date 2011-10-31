package updater.crypto;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import updater.script.InvalidFormatException;
import updater.util.CommonUtil;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class KeyGenerator {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }

    protected KeyGenerator() {
    }

    public static void generateRSA(int keySize, File saveTo) throws IOException {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize);
            KeyPair keyPair = keyPairGenerator.genKeyPair();

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec publicKeySpec = keyFactory.getKeySpec(keyPair.getPublic(), RSAPublicKeySpec.class);
            RSAPrivateKeySpec privateKeySpec = keyFactory.getKeySpec(keyPair.getPrivate(), RSAPrivateKeySpec.class);

            RSAKey rsaKey = new RSAKey(privateKeySpec.getModulus().toByteArray(), publicKeySpec.getPublicExponent().toByteArray(), privateKeySpec.getPrivateExponent().toByteArray());

            CommonUtil.writeFile(saveTo, rsaKey.output());
        } catch (InvalidKeySpecException ex) {
            if (debug) {
                Logger.getLogger(KeyGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (NoSuchAlgorithmException ex) {
            if (debug) {
                Logger.getLogger(KeyGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (UnsupportedEncodingException ex) {
            if (debug) {
                Logger.getLogger(KeyGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (TransformerException ex) {
            if (debug) {
                Logger.getLogger(KeyGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void generateAES(int keySize, File saveTo) throws IOException {
        Random random = new Random();

        byte[] key = new byte[keySize / 8];
        random.nextBytes(key);

        byte[] IV = new byte[16];
        random.nextBytes(IV);

        AESKey aesKey = new AESKey(key, IV);
        try {
            CommonUtil.writeFile(saveTo, aesKey.output());
        } catch (TransformerException ex) {
            if (debug) {
                Logger.getLogger(KeyGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void renewAESIV(File file) throws IOException, InvalidFormatException {
        Random random = new Random();

        byte[] IV = new byte[16];
        random.nextBytes(IV);

        AESKey aesKey = AESKey.read(CommonUtil.readFile(file));
        aesKey.setIV(IV);

        try {
            CommonUtil.writeFile(file, aesKey.output());
        } catch (TransformerException ex) {
            if (debug) {
                Logger.getLogger(KeyGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
