/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package updater.util;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import updater.TestCommon;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class CommonUtilTest {
    
    protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public CommonUtilTest() {
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
     * Test of rsaEncrypt & rsaDecrypt method, of class Util.
     */
    @Test
    public void testRsaEnDecrypt() {
        System.out.println("+++++ testRsaEnDecrypt +++++");
        try {
            BigInteger mod = new BigInteger(TestCommon.modulusString, 16);

            RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(mod, new BigInteger(TestCommon.privateExponentString, 16));
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(mod, new BigInteger(TestCommon.publicExponentString, 16));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            File testFile = new File(packagePath + "UtilTest_rsaEnDecrypt.ico");
            byte[] testData = CommonUtil.readFile(testFile);
            assertNotNull(testData);
            assertTrue(testData.length > 0);

            // encrypt
            int blockSize = mod.bitLength() / 8;
            byte[] encrypted = CommonUtil.rsaEncrypt(privateKey, blockSize, blockSize - 11, testData);
            assertNotNull(encrypted);
            assertEquals(1280, encrypted.length);

            // decrypt
            byte[] decrypted = CommonUtil.rsaDecrypt(publicKey, blockSize, encrypted);
            assertNotNull(decrypted);
            assertEquals(1150, decrypted.length);

            assertArrayEquals(testData, decrypted);
        } catch (Exception ex) {
            fail("! Exception caught.");
            Logger.getLogger(CommonUtilTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
