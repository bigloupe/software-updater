package updater.crypto;

import java.io.File;
import java.io.IOException;
import javax.xml.transform.TransformerException;
import updater.script.InvalidFormatException;
import updater.util.CommonUtil;
import updater.TestCommon;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class AESKeyTest {

    protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public AESKeyTest() {
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

        byte[] aesKey1Data = CommonUtil.readFile(new File(packagePath + "AESKeyTest_1.xml"));
        byte[] aesKey2Data = CommonUtil.readFile(new File(packagePath + "AESKeyTest_2.xml"));
        assertNotNull(aesKey1Data);
        assertNotNull(aesKey2Data);

        System.out.println("+ read & write test");
        AESKey aesKey = AESKey.read(aesKey1Data);
        assertNotNull(aesKey);
        assertArrayEquals(new String(aesKey.output(), "UTF-8"), aesKey1Data, aesKey.output());

        System.out.println("+ getter & setter");
        byte[] key = KeyGenerator.generateRandom(32);
        aesKey.setKey(key);
        assertArrayEquals("! key byte array not equal", key, aesKey.getKey());
        byte[] IV = KeyGenerator.generateRandom(16);
        aesKey.setIV(IV);
        assertArrayEquals("! key byte array not equal", IV, aesKey.getIV());

        System.out.println("+ read corrupted test");
        boolean exceptionCaught = false;
        try {
            aesKey = AESKey.read(aesKey2Data);
        } catch (Exception ex) {
            exceptionCaught = true;
        }
        if (!exceptionCaught) {
            fail("! Failed to recognize the AES key XML file is incorrect.");
        }
    }
}
