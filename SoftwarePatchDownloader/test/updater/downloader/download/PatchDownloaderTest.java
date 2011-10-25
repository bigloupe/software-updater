package updater.downloader.download;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import updater.TestCommon;
import updater.script.Catalog;
import updater.script.InvalidFormatException;
import updater.downloader.util.Util;
import updater.script.Patch;
import updater.util.CommonUtil.InvalidVersionException;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchDownloaderTest {

    protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public PatchDownloaderTest() {
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
     * Test of getSuitablePatches method, of class SoftwareUpdater.
     */
    @Test
    public void testGetPatches_Catalog_String() throws InvalidVersionException {
        System.out.println("+++++ testGetPatches_Catalog_String +++++");

        byte[] catalogData = null;
        try {
            catalogData = Util.readFile(new File(packagePath + "PatchDownloaderTest_getPatches.xml"));
        } catch (IOException ex) {
            Logger.getLogger(PatchDownloaderTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertNotNull(catalogData);
        assertTrue(catalogData.length != 0);

        Catalog catalog = null;
        try {
            catalog = Catalog.read(catalogData);
        } catch (InvalidFormatException ex) {
            Logger.getLogger(PatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
            fail("! Failed to read test file.");
        }


        List<Patch> result = PatchDownloader.getSuitablePatches(catalog, "1.0.0");
        assertEquals(4, result.size());

        int totalSize = 0;

        Patch update = result.get(0);
        totalSize += update.getDownloadLength();
        assertEquals("1.0.0", update.getVersionFrom());
        assertEquals("1.0.1", update.getVersionTo());
        update = result.get(1);
        totalSize += update.getDownloadLength();
        assertEquals("1.0.1", update.getVersionFrom());
        assertEquals("1.0.4", update.getVersionTo());
        update = result.get(2);
        totalSize += update.getDownloadLength();
        assertEquals("1.0.4", update.getVersionFrom());
        assertEquals("1.0.5", update.getVersionTo());
        update = result.get(3);
        totalSize += update.getDownloadLength();
        assertEquals("1.0.5", update.getVersionFrom());
        assertEquals("1.0.6", update.getVersionTo());

        assertEquals(82 + 13 + 7 + 14, totalSize);


        result = PatchDownloader.getSuitablePatches(catalog, "1.0.2");
        assertEquals(3, result.size());

        totalSize = 0;

        update = result.get(0);
        totalSize += update.getDownloadLength();
        assertEquals("1.0.2", update.getVersionFrom());
        assertEquals("1.0.3", update.getVersionTo());
        update = result.get(1);
        totalSize += update.getDownloadLength();
        assertEquals("1.0.3", update.getVersionFrom());
        assertEquals("1.0.5", update.getVersionTo());
        update = result.get(2);
        totalSize += update.getDownloadLength();
        assertEquals("1.0.5", update.getVersionFrom());
        assertEquals("1.0.6", update.getVersionTo());

        assertEquals(16 + 88 + 14, totalSize);
    }
}
