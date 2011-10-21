package updater.launcher;

import updater.launcher.SoftwareStarter;
import updater.launcher.LaunchFailedException;
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

    public SoftwareStarterTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
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
        System.out.println("startSoftware");

        File testFile = new File("testLaunch_A9fD6");
        testFile.delete();
        assertEquals(false, testFile.exists());
        try {
            SoftwareStarter.startSoftware(TestSuite.pathToTestPackage + SoftwareStarterTest.class.getPackage().getName().replace('.', '/') + "/SoftwareStarterTest/SoftwareStarterLaunchTest.jar", "softwarestarterlaunchtest.SoftwareStarterLaunchTest", new String[]{"testLaunch_A9fD6"});
        } catch (LaunchFailedException ex) {
            fail("Launch failed");
            Logger.getLogger(SoftwareStarterTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertEquals(true, testFile.exists());

        testFile.delete();
        assertEquals(false, testFile.exists());
    }
}
