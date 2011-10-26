package updater.downloader.download;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import updater.TestCommon;
import updater.downloader.download.RemoteContent.GetCatalogResult;
import updater.downloader.download.RemoteContent.GetPatchListener;
import updater.downloader.download.RemoteContent.GetPatchResult;
import updater.downloader.download.RemoteContent.RSAPublicKey;
import updater.downloader.util.Util;
import updater.util.CommonUtil.ObjectReference;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class RemoteContentTest {

    protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public RemoteContentTest() {
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
     * Test of getCatalog method, of class RemoteContent.
     * This test depends on some functions in /updater/util/Util.java.
     */
    @Test
    public void testGetCatalog() throws IOException {
        System.out.println("+++++ testGetCatalog +++++");

        String xmlFileName = "RemoteContentTest_getCatalog.xml";
        String manipulatedXmlFileName = "RemoteContentTest_getCatalog_manipulated.xml";
        File originalFile = new File(packagePath + xmlFileName);
        String originalFileString = null;
        try {
            originalFileString = new String(Util.readFile(originalFile), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(RemoteContentTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertTrue(originalFileString != null);

        //<editor-fold defaultstate="collapsed" desc="test normal request">
        System.out.println("- test normal request");

        String url = TestCommon.urlRoot + manipulatedXmlFileName;
        long lastUpdateDate = 0L;
        RSAPublicKey key = new RSAPublicKey(new BigInteger(TestCommon.modulusString, 16), new BigInteger(TestCommon.publicExponentString, 16));
        GetCatalogResult result = RemoteContent.getCatalog(url, lastUpdateDate, key);

        assertNotNull(result);
        assertFalse(result.isNotModified());
        assertNotNull(result.getCatalog());
        try {
            assertEquals(originalFileString, result.getCatalog().output());
        } catch (TransformerException ex) {
            Logger.getLogger(RemoteContentTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("! Invalid output format.");
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test If-Modified-Since header">
        System.out.println("- test If-Modified-Since header");

        url = TestCommon.urlRoot + manipulatedXmlFileName;
        lastUpdateDate = System.currentTimeMillis() - 2000;
        key = new RSAPublicKey(new BigInteger(TestCommon.modulusString, 16), new BigInteger(TestCommon.publicExponentString, 16));
        result = RemoteContent.getCatalog(url, lastUpdateDate, key);

        assertNotNull(result);
        assertTrue(result.isNotModified());
        assertNull(result.getCatalog());
        //</editor-fold>
    }

    /**
     * Test of getPatch method, of class RemoteContent.
     * This test depends on some functions in /updater/util/Util.java.
     */
    @Test
    public void testGetPatch() throws IOException {
        System.out.println("+++++ testGetPatch +++++");

        String originalFileName = "RemoteContentTest_getPatch_original.png";
        String partFileName = "RemoteContentTest_getPatch_part.png";
        // the file is fully downloaded and some downloaded bytes are incorrect
        String fullBrokenFileName = "RemoteContentTest_getPatch_full_broken.png";
        // the file is partly downloaded and some downloaded bytes are incorrect
        String partBrokenFileName = "RemoteContentTest_getPatch_part_broken.png";
        String largerFileName = "RemoteContentTest_getPatch_larger.png";

        File originalFile = new File(packagePath + originalFileName);
        File partFile = new File(packagePath + partFileName);
        File fullBrokenFile = new File(packagePath + fullBrokenFileName);
        File partBrokenFile = new File(packagePath + partBrokenFileName);
        File largerFile = new File(packagePath + largerFileName);

        assertTrue(originalFile.exists());
        assertTrue(partFile.exists());
        assertTrue(fullBrokenFile.exists());
        assertTrue(partBrokenFile.exists());
        assertTrue(largerFile.exists());

        File tempFile = new File(originalFileName + ".kh6am");
        tempFile.deleteOnExit();
        final ObjectReference<Long> startingPosition = new ObjectReference<Long>(0L);
        final ObjectReference<Integer> cumulativeByteDownloaded = new ObjectReference<Integer>(0);


        //<editor-fold defaultstate="collapsed" desc="test fresh download">
        System.out.println("- test fresh download");

        startingPosition.setObj(0L);
        cumulativeByteDownloaded.setObj(0);

        GetPatchListener listener = new GetPatchListener() {

            @Override
            public boolean downloadInterrupted() {
                return true;
            }

            @Override
            public void byteStart(long pos) {
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.setObj(cumulativeByteDownloaded.getObj() + numberOfBytes);
            }
        };
        String url = TestCommon.urlRoot + originalFileName;
        File saveToFile = tempFile;
        String fileSHA1 = Util.getSHA256String(originalFile);
        int expectedLength = (int) originalFile.length();

        GetPatchResult result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertTrue(result.getResult());
        assertEquals(0, (long) startingPosition.getObj());
        assertEquals(originalFile.length(), (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256String(originalFile), Util.getSHA256String(saveToFile));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test resume download">
        System.out.println("- test resume download");

        Util.copyFile(partFile, tempFile);
        int initFileSize = (int) tempFile.length();
        startingPosition.setObj(0L);
        cumulativeByteDownloaded.setObj(0);

        listener = new GetPatchListener() {

            @Override
            public boolean downloadInterrupted() {
                return true;
            }

            @Override
            public void byteStart(long pos) {
                startingPosition.setObj(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.setObj(cumulativeByteDownloaded.getObj() + numberOfBytes);
            }
        };
        url = TestCommon.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256String(originalFile);
        expectedLength = (int) originalFile.length();

        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertTrue(result.getResult());
        assertEquals(initFileSize, (long) startingPosition.getObj());
        assertEquals(originalFile.length() - initFileSize, (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256String(originalFile), Util.getSHA256String(saveToFile));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test resume download but some downloaded bytes in the file are broken">
        System.out.println("- test resume download but some downloaded bytes in the file are broken");

        Util.copyFile(partBrokenFile, tempFile);
        initFileSize = (int) tempFile.length();
        startingPosition.setObj(0L);
        cumulativeByteDownloaded.setObj(0);

        listener = new GetPatchListener() {

            @Override
            public boolean downloadInterrupted() {
                return true;
            }

            @Override
            public void byteStart(long pos) {
                startingPosition.setObj(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.setObj(cumulativeByteDownloaded.getObj() + numberOfBytes);
            }
        };
        url = TestCommon.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256String(originalFile);
        expectedLength = (int) originalFile.length();

        TestCommon.suppressErrorOutput();
        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);
        TestCommon.restoreErrorOutput();

        assertFalse(result.getResult());
        assertEquals(initFileSize, (long) startingPosition.getObj());
        assertEquals(originalFile.length() - initFileSize, (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertFalse(Util.getSHA256String(originalFile).equals(Util.getSHA256String(saveToFile)));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test download when file is fully downloaded">
        System.out.println("- test download when file is fully downloaded");

        Util.copyFile(originalFile, tempFile);
        initFileSize = (int) tempFile.length();
        startingPosition.setObj(0L);
        cumulativeByteDownloaded.setObj(0);

        listener = new GetPatchListener() {

            @Override
            public boolean downloadInterrupted() {
                return true;
            }

            @Override
            public void byteStart(long pos) {
                startingPosition.setObj(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.setObj(cumulativeByteDownloaded.getObj() + numberOfBytes);
            }
        };
        url = TestCommon.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256String(originalFile);
        expectedLength = (int) originalFile.length();

        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertTrue(result.getResult());
        assertEquals(7007, (long) startingPosition.getObj());
        assertEquals(originalFile.length() - initFileSize, (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256String(originalFile), Util.getSHA256String(saveToFile));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test download when file is fully downloaded but some downloaded bytes in the file are broken">
        System.out.println("- test download when file is fully downloaded but some downloaded bytes in the file are broken");

        Util.copyFile(fullBrokenFile, tempFile);
        startingPosition.setObj(0L);
        cumulativeByteDownloaded.setObj(0);

        listener = new GetPatchListener() {

            @Override
            public boolean downloadInterrupted() {
                return true;
            }

            @Override
            public void byteStart(long pos) {
                startingPosition.setObj(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.setObj(cumulativeByteDownloaded.getObj() + numberOfBytes);
            }
        };
        url = TestCommon.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256String(originalFile);
        expectedLength = (int) originalFile.length();

        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertTrue(result.getResult());
        assertEquals(0, (long) startingPosition.getObj());
        assertEquals(originalFile.length(), (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256String(originalFile), Util.getSHA256String(saveToFile));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test download when the downloaded file is larger">
        System.out.println("- test download when the downloaded file is larger");

        Util.copyFile(largerFile, tempFile);
        startingPosition.setObj(0L);
        cumulativeByteDownloaded.setObj(0);

        listener = new GetPatchListener() {

            @Override
            public boolean downloadInterrupted() {
                return true;
            }

            @Override
            public void byteStart(long pos) {
                startingPosition.setObj(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.setObj(cumulativeByteDownloaded.getObj() + numberOfBytes);
            }
        };
        url = TestCommon.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256String(originalFile);
        expectedLength = (int) originalFile.length();

        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertTrue(result.getResult());
        assertEquals(0, (long) startingPosition.getObj());
        assertEquals(originalFile.length(), (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256String(originalFile), Util.getSHA256String(saveToFile));
        //</editor-fold>

        // test thread interrupt


        tempFile.delete();
    }
}
