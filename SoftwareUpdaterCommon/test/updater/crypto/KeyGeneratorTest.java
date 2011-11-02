package updater.crypto;

import watne.seis720.project.KeySize;
import watne.seis720.project.Mode;
import watne.seis720.project.Padding;
import watne.seis720.project.WatneAES_Implementer;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Random;
import updater.TestCommon;
import java.io.File;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import updater.util.CommonUtil;
import static org.junit.Assert.*;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class KeyGeneratorTest {

    protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public KeyGeneratorTest() {
    }

    protected static String getClassName() {
        return new Object() {
        }.getClass().getEnclosingClass().getName();
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("***** " + getClassName() + " *****");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        System.out.println("******************************\r\n");
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of generateRSA method, of class KeyGenerator.
     */
    @Test
    public void testGenerateRSA() throws Exception {
        System.out.println("+++++ testGenerateRSA +++++");

        File tmpFile = new File("KeyGeneratorTest_testGenerateRSA_mAl7d");
        tmpFile.deleteOnExit();

        int[] tests = new int[]{512, 2048};
        for (int bits : tests) {
            System.out.println("- test gen key " + bits + "-bits, encrypt and decrypt");

            KeyGenerator.generateRSA(bits, tmpFile);
            assertTrue(tmpFile.exists());
            assertTrue(tmpFile.length() > (bits / 8) * 2);

            byte[] testContent = CommonUtil.readFile(tmpFile);
            RSAKey rsaKey = RSAKey.read(testContent);
//            assertEquals(bits / 8, rsaKey.getPrivateExponent().length);

            BigInteger mod = new BigInteger(rsaKey.getModulus());
            RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(mod, new BigInteger(rsaKey.getPrivateExponent()));
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(mod, new BigInteger(rsaKey.getPublicExponent()));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

            byte[] encrypted = CommonUtil.rsaEncrypt(privateKey, bits / 8, (bits / 8) - 11, testContent);
            byte[] decrypted = CommonUtil.rsaDecrypt(publicKey, bits / 8, encrypted);
            assertArrayEquals("! decrypted data are not equal to original data", testContent, decrypted);
        }
    }

    /**
     * Test of generateAES method, of class KeyGenerator.
     */
    @Test
    public void testGenerateAES() throws Exception {
        System.out.println("+++++ testGenerateAES +++++");

        File tmpFile = new File("KeyGeneratorTest_testGenerateAES_mAl7d");
        File tmpEncFile = new File("KeyGeneratorTest_testGenerateAES_encrypted_mAl7d");
        File tmpDecFile = new File("KeyGeneratorTest_testGenerateAES_decrypted_mAl7d");
        tmpFile.deleteOnExit();
        tmpEncFile.deleteOnExit();
        tmpDecFile.deleteOnExit();

        int[] tests = new int[]{128, 256};
        for (int bits : tests) {
            System.out.println("- test gen key " + bits + "-bits, encrypt and decrypt");

            KeyGenerator.generateAES(bits, tmpFile);
            assertTrue(tmpFile.exists());
            assertTrue(tmpFile.length() > (bits / 8) * 2);

            byte[] testContent = CommonUtil.readFile(tmpFile);
            AESKey aesKey = AESKey.read(testContent);
            assertEquals(bits / 8, aesKey.getKey().length);

            WatneAES_Implementer aesCipher = new WatneAES_Implementer();
            aesCipher.setMode(Mode.CBC);
            aesCipher.setPadding(Padding.PKCS5PADDING);
            aesCipher.setKeySize(KeySize.BITS256);
            aesCipher.setKey(aesKey.getKey());
            aesCipher.setInitializationVector(aesKey.getIV());
            aesCipher.encryptFile(tmpFile, tmpEncFile);

            aesCipher = new WatneAES_Implementer();
            aesCipher.setMode(Mode.CBC);
            aesCipher.setPadding(Padding.PKCS5PADDING);
            aesCipher.setKeySize(KeySize.BITS256);
            aesCipher.setKey(aesKey.getKey());
            aesCipher.setInitializationVector(aesKey.getIV());
            aesCipher.decryptFile(tmpEncFile, tmpDecFile);

            assertArrayEquals("! decrypted data are not equal to original data", testContent, CommonUtil.readFile(tmpDecFile));
        }
    }

    /**
     * Test of renewAESIV method, of class KeyGenerator.
     */
    @Test
    public void testRenewAESIV() throws Exception {
        System.out.println("+++++ testRenewAESIV +++++");

        File tmpFile = new File("KeyGeneratorTest_testRenewAESIV_mAl7d");
        tmpFile.deleteOnExit();

        KeyGenerator.generateAES(256, tmpFile);
        AESKey aesKey = AESKey.read(CommonUtil.readFile(tmpFile));

        byte[] oldIV = new byte[aesKey.getIV().length];
        System.arraycopy(aesKey.getIV(), 0, oldIV, 0, aesKey.getIV().length);

        KeyGenerator.renewAESIV(tmpFile);
        aesKey = AESKey.read(CommonUtil.readFile(tmpFile));

        byte[] newIV = aesKey.getIV();

        assertNotNull(newIV);
        assertEquals(oldIV.length, newIV.length);
        assertFalse(Arrays.equals(oldIV, newIV));
    }

    /**
     * Test of generateRandom method, of class KeyGenerator.
     */
    @Test
    public void testGenerateRandom() {
        System.out.println("+++++ testGenerateRandom +++++");

        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            int numberOfBytes = random.nextInt(512) + 1;
            byte[] generated = KeyGenerator.generateRandom(numberOfBytes);
            assertNotNull(generated);
            assertEquals(numberOfBytes, generated.length);
        }
    }
}
