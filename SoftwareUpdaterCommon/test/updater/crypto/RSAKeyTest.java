package updater.crypto;

import updater.TestCommon;
import java.io.File;
import java.io.IOException;
import javax.xml.transform.TransformerException;
import updater.script.InvalidFormatException;
import updater.util.CommonUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class RSAKeyTest {

    protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public RSAKeyTest() {
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

    @Test
    public void test() throws IOException, InvalidFormatException, TransformerException {
        System.out.println("+++++ test +++++");

        byte[] rsaKey1Data = CommonUtil.readFile(new File(packagePath + "RSAKeyTest_1.xml"));
        byte[] rsaKey2Data = CommonUtil.readFile(new File(packagePath + "RSAKeyTest_2.xml"));
        assertNotNull(rsaKey1Data);
        assertNotNull(rsaKey2Data);

        System.out.println("+ read & write test");
        RSAKey rsakey = RSAKey.read(rsaKey1Data);
        assertNotNull(rsakey);
        assertArrayEquals(new String(rsakey.output(), "UTF-8"), rsaKey1Data, rsakey.output());

        System.out.println("+ getter & setter");
        byte[] modulus = KeyGenerator.generateRandom(128);
        rsakey.setModulus(modulus);
        assertArrayEquals("! modulus byte array not equal", modulus, rsakey.getModulus());
        byte[] publicExponent = KeyGenerator.generateRandom(16);
        rsakey.setPublicExponent(publicExponent);
        assertArrayEquals("! key byte array not equal", publicExponent, rsakey.getPublicExponent());
        byte[] privateExponent = KeyGenerator.generateRandom(128);
        rsakey.setPrivateExponent(privateExponent);
        assertArrayEquals("! key byte array not equal", privateExponent, rsakey.getPrivateExponent());

        System.out.println("+ read corrupted test");
        boolean exceptionCaught = false;
        try {
            rsakey = RSAKey.read(rsaKey2Data);
        } catch (Exception ex) {
            exceptionCaught = true;
        }
        if (!exceptionCaught) {
            fail("! Failed to recognize the AES key XML file is incorrect.");
        }
    }
}