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
        assertArrayEquals(new String(clientScript.output(), "UTF-8"), client1Data, clientScript.output());

        clientScript = Client.read(client2Data);
        assertNotNull(clientScript);
        assertArrayEquals(new String(clientScript.output(), "UTF-8"), client2Data, clientScript.output());

        clientScript = Client.read(client3Data);
        assertNotNull(clientScript);
        assertArrayEquals(new String(clientScript.output(), "UTF-8"), client3Data, clientScript.output());

        clientScript = Client.read(client4Data);
        assertNotNull(clientScript);
        assertArrayEquals(new String(clientScript.output(), "UTF-8"), client4Data, clientScript.output());
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
        assertArrayEquals(new String(catalogScript.output(), "UTF-8"), catalog1Data, catalogScript.output());

        catalogScript = Catalog.read(catalog2Data);
        assertNotNull(catalogScript);
        assertArrayEquals(new String(catalogScript.output(), "UTF-8"), catalog2Data, catalogScript.output());
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
        assertArrayEquals(new String(patchScript.output(), "UTF-8"), patch1Data, patchScript.output());

        patchScript = Patch.read(patch2Data);
        assertNotNull(patchScript);
        assertArrayEquals(new String(patchScript.output(), "UTF-8"), patch2Data, patchScript.output());

        patchScript = Patch.read(patch3Data);
        assertNotNull(patchScript);
        assertArrayEquals(new String(patchScript.output(), "UTF-8"), patch3Data, patchScript.output());

        patchScript = Patch.read(patch4Data);
        assertNotNull(patchScript);
        assertArrayEquals(new String(patchScript.output(), "UTF-8"), patch4Data, patchScript.output());
    }
}
