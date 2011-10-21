package updater.downloader.download;

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
import updater.TestSuite;
import updater.script.Catalog;
import updater.script.Catalog.Update;
import updater.script.InvalidFormatException;
import updater.downloader.util.Util;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchDownloaderTest {

    public PatchDownloaderTest() {
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
     * Test of getPatches method, of class SoftwareUpdater.
     */
    @Test
    public void testGetPatches_Catalog_String() {
        System.out.println("testGetPatches_Catalog_String");

        byte[] catalogData = Util.readFile(new File(TestSuite.pathToTestPackage + getClass().getPackage().getName().replace('.', '/') + "/PatchDownloaderTest/PatchDownloaderTest_getPatches.xml"));
        assertTrue(catalogData != null && catalogData.length != 0);

        Catalog catalog = null;
        try {
            catalog = Catalog.read(catalogData);
        } catch (InvalidFormatException ex) {
            Logger.getLogger(PatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
            fail("Failed to read test file.");
        }


        List<Update> result = PatchDownloader.getPatches(catalog, "1.0.0");
        assertEquals(4, result.size());

        int totalSize = 0;

        Update update = result.get(0);
        totalSize += update.getPatchLength();
        assertEquals("1.0.0", update.getVersionFrom());
        assertEquals("1.0.1", update.getVersionTo());
        update = result.get(1);
        totalSize += update.getPatchLength();
        assertEquals("1.0.1", update.getVersionFrom());
        assertEquals("1.0.4", update.getVersionTo());
        update = result.get(2);
        totalSize += update.getPatchLength();
        assertEquals("1.0.4", update.getVersionFrom());
        assertEquals("1.0.5", update.getVersionTo());
        update = result.get(3);
        totalSize += update.getPatchLength();
        assertEquals("1.0.5", update.getVersionFrom());
        assertEquals("1.0.6", update.getVersionTo());

        assertEquals(82 + 13 + 7 + 14, totalSize);


        result = PatchDownloader.getPatches(catalog, "1.0.2");
        assertEquals(3, result.size());

        totalSize = 0;

        update = result.get(0);
        totalSize += update.getPatchLength();
        assertEquals("1.0.2", update.getVersionFrom());
        assertEquals("1.0.3", update.getVersionTo());
        update = result.get(1);
        totalSize += update.getPatchLength();
        assertEquals("1.0.3", update.getVersionFrom());
        assertEquals("1.0.5", update.getVersionTo());
        update = result.get(2);
        totalSize += update.getPatchLength();
        assertEquals("1.0.5", update.getVersionFrom());
        assertEquals("1.0.6", update.getVersionTo());

        assertEquals(16 + 88 + 14, totalSize);
    }
}
