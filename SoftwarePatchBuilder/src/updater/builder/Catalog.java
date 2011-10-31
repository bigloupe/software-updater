package updater.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import updater.patch.PatchCreator;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Catalog {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }

    protected Catalog() {
    }

    public static void encrypt(File in, File out, BigInteger mod, BigInteger privateExp) throws IOException {
        RSAPrivateKey privateKey = null;
        try {
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(mod, privateExp);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException ex) {
            if (debug) {
                Logger.getLogger(PatchCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (InvalidKeySpecException ex) {
            if (debug) {
                Logger.getLogger(PatchCreator.class.getName()).log(Level.SEVERE, null, ex);
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

    public static void decrypt(File in, File out, BigInteger mod, BigInteger publicExp) throws IOException {
        RSAPublicKey publicKey = null;
        try {
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(mod, publicExp);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException ex) {
            if (debug) {
                Logger.getLogger(PatchCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (InvalidKeySpecException ex) {
            if (debug) {
                Logger.getLogger(PatchCreator.class.getName()).log(Level.SEVERE, null, ex);
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
