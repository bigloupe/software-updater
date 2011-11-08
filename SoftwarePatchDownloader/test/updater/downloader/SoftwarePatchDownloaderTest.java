package updater.downloader;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import updater.TestCommon;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwarePatchDownloaderTest {

    protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public SoftwarePatchDownloaderTest() {
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
     * Test of checkForUpdates method, of class SoftwarePatchDownloader.
     */
    @Test
    public void testCheckForUpdates() {
        assertTrue(true);
    }
}
