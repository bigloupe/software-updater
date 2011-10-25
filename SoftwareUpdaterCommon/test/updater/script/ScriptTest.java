package updater.script;

import java.io.IOException;
import javax.xml.transform.TransformerException;
import updater.TestCommon;
import java.io.File;
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
public class ScriptTest {

    protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public ScriptTest() {
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
    public void clientTest() throws IOException, InvalidFormatException, TransformerException {
        System.out.println("+++++ clientTest +++++");

        byte[] client1Data = CommonUtil.readFile(new File(packagePath + "ScriptTest_client1.xml"));
        byte[] client2Data = CommonUtil.readFile(new File(packagePath + "ScriptTest_client2.xml"));
        byte[] client3Data = CommonUtil.readFile(new File(packagePath + "ScriptTest_client3.xml"));
        byte[] client4Data = CommonUtil.readFile(new File(packagePath + "ScriptTest_client4.xml"));
        assertNotNull(client1Data);
        assertNotNull(client2Data);
        assertNotNull(client3Data);
        assertNotNull(client4Data);

        Client clientScript = Client.read(client1Data);
        assertNotNull(clientScript);
        assertArrayEquals(clientScript.output(), client1Data, clientScript.output().getBytes("UTF-8"));

        clientScript = Client.read(client2Data);
        assertNotNull(clientScript);
        assertArrayEquals(clientScript.output(), client2Data, clientScript.output().getBytes("UTF-8"));

        clientScript = Client.read(client3Data);
        assertNotNull(clientScript);
        assertArrayEquals(clientScript.output(), client3Data, clientScript.output().getBytes("UTF-8"));

        clientScript = Client.read(client4Data);
        assertNotNull(clientScript);
        assertArrayEquals(clientScript.output(), client4Data, clientScript.output().getBytes("UTF-8"));
    }

    @Test
    public void catalogTest() throws IOException, InvalidFormatException, TransformerException {
        System.out.println("+++++ catalogTest +++++");

        byte[] catalog1Data = CommonUtil.readFile(new File(packagePath + "ScriptTest_catalog1.xml"));
        byte[] catalog2Data = CommonUtil.readFile(new File(packagePath + "ScriptTest_catalog2.xml"));
        assertNotNull(catalog1Data);
        assertNotNull(catalog2Data);

        Catalog catalogScript = Catalog.read(catalog1Data);
        assertNotNull(catalogScript);
        assertArrayEquals(catalogScript.output(), catalog1Data, catalogScript.output().getBytes("UTF-8"));

        catalogScript = Catalog.read(catalog2Data);
        assertNotNull(catalogScript);
        assertArrayEquals(catalogScript.output(), catalog2Data, catalogScript.output().getBytes("UTF-8"));
    }

    @Test
    public void patchTest() throws IOException, InvalidFormatException, TransformerException {
        System.out.println("+++++ patchTest +++++");

        byte[] patch1Data = CommonUtil.readFile(new File(packagePath + "ScriptTest_patch1.xml"));
        byte[] patch2Data = CommonUtil.readFile(new File(packagePath + "ScriptTest_patch2.xml"));
        byte[] patch3Data = CommonUtil.readFile(new File(packagePath + "ScriptTest_patch3.xml"));
        byte[] patch4Data = CommonUtil.readFile(new File(packagePath + "ScriptTest_patch4.xml"));
        assertNotNull(patch1Data);
        assertNotNull(patch2Data);
        assertNotNull(patch3Data);
        assertNotNull(patch4Data);

        Patch patchScript = Patch.read(patch1Data);
        assertNotNull(patchScript);
        assertArrayEquals(patchScript.output(), patch1Data, patchScript.output().getBytes("UTF-8"));

        patchScript = Patch.read(patch2Data);
        assertNotNull(patchScript);
        assertArrayEquals(patchScript.output(), patch2Data, patchScript.output().getBytes("UTF-8"));

        patchScript = Patch.read(patch3Data);
        assertNotNull(patchScript);
        assertArrayEquals(patchScript.output(), patch3Data, patchScript.output().getBytes("UTF-8"));

        patchScript = Patch.read(patch4Data);
        assertNotNull(patchScript);
        assertArrayEquals(patchScript.output(), patch4Data, patchScript.output().getBytes("UTF-8"));
    }
}
