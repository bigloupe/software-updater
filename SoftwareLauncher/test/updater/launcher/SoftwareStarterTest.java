package updater.launcher;

import updater.TestCommon;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareStarterTest {

    protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public SoftwareStarterTest() {
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
     * Test of startSoftware method, of class SoftwareStarter.
     */
    @Test
    public void testStartSoftware() {
        System.out.println("+++++ testStartSoftware +++++");

        File testFile = new File("testLaunch_A9fD6");
        testFile.delete();
        assertFalse(testFile.exists());
        try {
            SoftwareStarter.startSoftware(packagePath + "SoftwareStarterLaunchTest.jar", "softwarestarterlaunchtest.SoftwareStarterLaunchTest", new String[]{"testLaunch_A9fD6"});
        } catch (LaunchFailedException ex) {
            fail("! Launch failed.");
            Logger.getLogger(SoftwareStarterTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertTrue(testFile.exists());

        testFile.delete();
        assertFalse(testFile.exists());
    }
}
