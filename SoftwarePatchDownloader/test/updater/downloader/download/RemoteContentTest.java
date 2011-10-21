package updater.downloader.download;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import updater.TestSuite;
import static org.junit.Assert.*;
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

    public RemoteContentTest() {
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
     * Test of getCatalog method, of class RemoteContent.
     * This test depends on some functions in /updater/util/Util.java.
     */
    @Test
    public void testGetCatalog() {
        System.out.println("getCatalog");

        String xmlFileName = "RemoteContentTest_getCatalog.xml";
        String manipulatedXmlFileName = "RemoteContentTest_getCatalog_manipulated.xml";
        File originalFile = new File(TestSuite.pathToTestPackage + RemoteContentTest.class.getPackage().getName().replace('.', '/') + "/RemoteContentTest/" + xmlFileName);
        String originalFileString = null;
        try {
            originalFileString = new String(Util.readFile(originalFile), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(RemoteContentTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertTrue(originalFileString != null);

        //<editor-fold defaultstate="collapsed" desc="test normal request">
        System.out.println("getCatalog - test normal request");

        System.out.println(TestSuite.urlRoot + manipulatedXmlFileName);
        String url = TestSuite.urlRoot + manipulatedXmlFileName;
        long lastUpdateDate = 0L;
        RSAPublicKey key = new RSAPublicKey(new BigInteger(TestSuite.modulesString, 16), new BigInteger(TestSuite.publicExponentString, 16));
        GetCatalogResult result = RemoteContent.getCatalog(url, lastUpdateDate, key);

        assertTrue(result != null);
        assertTrue(!result.isNotModified());
        assertTrue(result.getCatalog() != null);
        assertTrue(result.getCatalog().output().equals(originalFileString));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test If-Modified-Since header">
        System.out.println("getCatalog - test If-Modified-Since header");

        url = TestSuite.urlRoot + manipulatedXmlFileName;
        lastUpdateDate = System.currentTimeMillis() - 2000;
        key = new RSAPublicKey(new BigInteger(TestSuite.modulesString, 16), new BigInteger(TestSuite.publicExponentString, 16));
        result = RemoteContent.getCatalog(url, lastUpdateDate, key);

        assertTrue(result != null);
        assertTrue(result.isNotModified());
        assertTrue(result.getCatalog() == null);
        //</editor-fold>
    }

    /**
     * Test of getPatch method, of class RemoteContent.
     * This test depends on some functions in /updater/util/Util.java.
     */
    @Test
    public void testGetPatch() {
        System.out.println("getPatch");

        String originalFileName = "RemoteContentTest_getPatch_original.png";
        String partFileName = "RemoteContentTest_getPatch_part.png";
        // the file is fully downloaded and some downloaded bytes are incorrect
        String fullBrokenFileName = "RemoteContentTest_getPatch_full_broken.png";
        // the file is partly downloaded and some downloaded bytes are incorrect
        String partBrokenFileName = "RemoteContentTest_getPatch_part_broken.png";
        String largerFileName = "RemoteContentTest_getPatch_larger.png";

        String FilePathPrefix = TestSuite.pathToTestPackage + getClass().getPackage().getName().replace('.', '/') + "/RemoteContentTest/";
        File originalFile = new File(FilePathPrefix + originalFileName);
        File partFile = new File(FilePathPrefix + partFileName);
        File fullBrokenFile = new File(FilePathPrefix + fullBrokenFileName);
        File partBrokenFile = new File(FilePathPrefix + partBrokenFileName);
        File largerFile = new File(FilePathPrefix + largerFileName);

        assertTrue(originalFile.exists());
        assertTrue(partFile.exists());
        assertTrue(fullBrokenFile.exists());
        assertTrue(partBrokenFile.exists());
        assertTrue(largerFile.exists());

        File tempFile = new File(originalFileName + ".kh6am");
        final ObjectReference<Long> startingPosition = new ObjectReference<Long>(0L);
        final ObjectReference<Integer> cumulativeByteDownloaded = new ObjectReference<Integer>(0);


        //<editor-fold defaultstate="collapsed" desc="test fresh download">
        System.out.println("getPatch - test fresh download");

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
        String url = TestSuite.urlRoot + originalFileName;
        File saveToFile = tempFile;
        String fileSHA1 = Util.getSHA256(originalFile);
        int expectedLength = (int) originalFile.length();

        GetPatchResult result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertEquals(true, result.getResult());
        assertEquals(0, (long) startingPosition.getObj());
        assertEquals(originalFile.length(), (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256(originalFile), Util.getSHA256(saveToFile));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test resume download">
        System.out.println("getPatch - test resume download");

        boolean copyResult = Util.copyFile(partFile, tempFile);
        assertEquals(true, copyResult);
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
        url = TestSuite.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256(originalFile);
        expectedLength = (int) originalFile.length();

        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertEquals(true, result.getResult());
        assertEquals(initFileSize, (long) startingPosition.getObj());
        assertEquals(originalFile.length() - initFileSize, (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256(originalFile), Util.getSHA256(saveToFile));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test resume download but some downloaded bytes in the file are broken">
        System.out.println("getPatch - test resume download but some downloaded bytes in the file are broken");

        copyResult = Util.copyFile(partBrokenFile, tempFile);
        assertEquals(true, copyResult);
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
        url = TestSuite.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256(originalFile);
        expectedLength = (int) originalFile.length();

        PrintStream ps = System.err;
        System.setErr(new PrintStream(new OutputStream() {

            @Override
            public void write(int b) throws IOException {
            }
        }));
        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);
        System.setErr(ps);

        assertEquals(false, result.getResult());
        assertEquals(initFileSize, (long) startingPosition.getObj());
        assertEquals(originalFile.length() - initFileSize, (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertTrue(!Util.getSHA256(originalFile).equals(Util.getSHA256(saveToFile)));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test download when file is fully downloaded">
        System.out.println("getPatch - test download when file is fully downloaded");

        copyResult = Util.copyFile(originalFile, tempFile);
        assertEquals(true, copyResult);
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
        url = TestSuite.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256(originalFile);
        expectedLength = (int) originalFile.length();

        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertEquals(true, result.getResult());
        assertEquals(0, (long) startingPosition.getObj());
        assertEquals(originalFile.length() - initFileSize, (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256(originalFile), Util.getSHA256(saveToFile));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test download when file is fully downloaded but some downloaded bytes in the file are broken">
        System.out.println("getPatch - test download when file is fully downloaded but some downloaded bytes in the file are broken");

        copyResult = Util.copyFile(fullBrokenFile, tempFile);
        assertEquals(true, copyResult);
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
        url = TestSuite.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256(originalFile);
        expectedLength = (int) originalFile.length();

        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertEquals(true, result.getResult());
        assertEquals(0, (long) startingPosition.getObj());
        assertEquals(originalFile.length(), (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256(originalFile), Util.getSHA256(saveToFile));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test download when the downloaded file is larger">
        System.out.println("getPatch - test download when the downloaded file is larger");

        copyResult = Util.copyFile(largerFile, tempFile);
        assertEquals(true, copyResult);
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
        url = TestSuite.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256(originalFile);
        expectedLength = (int) originalFile.length();

        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertEquals(true, result.getResult());
        assertEquals(0, (long) startingPosition.getObj());
        assertEquals(originalFile.length(), (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256(originalFile), Util.getSHA256(saveToFile));
        //</editor-fold>

        // test thread interrupt


        tempFile.delete();
    }
}
