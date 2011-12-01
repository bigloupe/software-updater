package updater.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import updater.TestCommon;
import updater.util.HTTPDownloader.DownloadResult;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class HTTPDownloaderTest {

    protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public HTTPDownloaderTest() {
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
    public void test() throws IOException {
        System.out.println("+++++ test +++++");

        String originalFileName = "HTTPDownloaderTest_getPatch_original.png";
        String partFileName = "HTTPDownloaderTest_getPatch_part.png";
        // the file is fully downloaded and some downloaded bytes are incorrect
        String fullBrokenFileName = "HTTPDownloaderTest_getPatch_full_broken.png";
        // the file is partly downloaded and some downloaded bytes are incorrect
        String partBrokenFileName = "HTTPDownloaderTest_getPatch_part_broken.png";
        String largerFileName = "HTTPDownloaderTest_getPatch_larger.png";

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
        final AtomicLong startingPosition = new AtomicLong(0L);
        final AtomicInteger cumulativeByteDownloaded = new AtomicInteger(0);


        //<editor-fold defaultstate="collapsed" desc="test fresh download">
        System.out.println("+ test fresh download");

        startingPosition.set(0L);
        cumulativeByteDownloaded.set(0);

        DownloadProgressListener listener = new DownloadProgressListener() {

            @Override
            public void byteStart(long pos) {
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.set(cumulativeByteDownloaded.get() + numberOfBytes);
            }

            @Override
            public void byteTotal(long total) {
            }

            @Override
            public void downloadRetry(DownloadResult result) {
            }
        };
        String url = TestCommon.urlRoot + originalFileName;
        File saveToFile = tempFile;
        long saveToFileLength = saveToFile.length();
        String fileSHA1 = CommonUtil.getSHA256String(originalFile);
        int expectedLength = (int) originalFile.length();

        DownloadResult result = null;
        FileOutputStream fout = null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            HTTPDownloader downloader = new HTTPDownloader();
            downloader.setResumeFile(saveToFile);
            downloader.setOutputTo(bout);
            result = downloader.download(listener, new URL(url), fileSHA1, expectedLength, 0, 0);
        } finally {
            CommonUtil.closeQuietly(fout);
        }

        assertEquals(DownloadResult.SUCCEED, result);
        assertEquals(0, startingPosition.get());
        assertEquals(originalFile.length(), cumulativeByteDownloaded.get());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(CommonUtil.getSHA256String(originalFile), CommonUtil.getSHA256String(saveToFile));
        assertArrayEquals(CommonUtil.readFile(originalFile), bout.toByteArray());
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test resume download">
        System.out.println("+ test resume download");

        CommonUtil.copyFile(partFile, tempFile);
        int initFileSize = (int) tempFile.length();
        startingPosition.set(0L);
        cumulativeByteDownloaded.set(0);

        listener = new DownloadProgressListener() {

            @Override
            public void byteStart(long pos) {
                startingPosition.set(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.set(cumulativeByteDownloaded.get() + numberOfBytes);
            }

            @Override
            public void byteTotal(long total) {
            }

            @Override
            public void downloadRetry(DownloadResult result) {
            }
        };
        url = TestCommon.urlRoot + originalFileName;
        saveToFile = tempFile;
        saveToFileLength = saveToFile.length();
        fileSHA1 = CommonUtil.getSHA256String(originalFile);
        expectedLength = (int) originalFile.length();

        result = null;
        fout = null;
        bout = new ByteArrayOutputStream();
        try {
            HTTPDownloader downloader = new HTTPDownloader();
            downloader.setResumeFile(saveToFile);
            downloader.setOutputTo(bout);
            result = downloader.download(listener, new URL(url), fileSHA1, expectedLength, 0, 0);
        } finally {
            CommonUtil.closeQuietly(fout);
        }

        assertEquals(DownloadResult.SUCCEED, DownloadResult.SUCCEED);
        assertEquals(initFileSize, startingPosition.get());
        assertEquals(originalFile.length() - initFileSize, cumulativeByteDownloaded.get());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(CommonUtil.getSHA256String(originalFile), CommonUtil.getSHA256String(saveToFile));
        byte[] originalFileData = CommonUtil.readFile(originalFile);
        byte[] dataToCheck = new byte[(int) (originalFile.length() - saveToFileLength)];
        System.arraycopy(originalFileData, (int) saveToFileLength, dataToCheck, 0, (int) (originalFile.length() - saveToFileLength));
        assertArrayEquals(dataToCheck, bout.toByteArray());
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test resume download but some downloaded bytes in the file are broken">
        System.out.println("+ test resume download but some downloaded bytes in the file are broken");

        CommonUtil.copyFile(partBrokenFile, tempFile);
        initFileSize = (int) tempFile.length();
        startingPosition.set(0L);
        cumulativeByteDownloaded.set(0);

        listener = new DownloadProgressListener() {

            @Override
            public void byteStart(long pos) {
                startingPosition.set(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.set(cumulativeByteDownloaded.get() + numberOfBytes);
            }

            @Override
            public void byteTotal(long total) {
            }

            @Override
            public void downloadRetry(DownloadResult result) {
            }
        };
        url = TestCommon.urlRoot + originalFileName;
        saveToFile = tempFile;
        saveToFileLength = saveToFile.length();
        fileSHA1 = CommonUtil.getSHA256String(originalFile);
        expectedLength = (int) originalFile.length();

        TestCommon.suppressErrorOutput();
        result = null;
        fout = null;
        bout = new ByteArrayOutputStream();
        try {
            HTTPDownloader downloader = new HTTPDownloader();
            downloader.setResumeFile(saveToFile);
            downloader.setOutputTo(bout);
            result = downloader.download(listener, new URL(url), fileSHA1, expectedLength, 0, 0);
        } finally {
            CommonUtil.closeQuietly(fout);
        }
        TestCommon.restoreErrorOutput();

        assertEquals(DownloadResult.CHECKSUM_FAILED, result);
        assertEquals(initFileSize, startingPosition.get());
        assertEquals(originalFile.length() - initFileSize, cumulativeByteDownloaded.get());
        assertEquals(originalFile.length(), saveToFile.length());
        assertFalse(CommonUtil.getSHA256String(originalFile).equals(CommonUtil.getSHA256String(saveToFile)));
        originalFileData = CommonUtil.readFile(originalFile);
        dataToCheck = new byte[(int) (originalFile.length() - saveToFileLength)];
        System.arraycopy(originalFileData, (int) saveToFileLength, dataToCheck, 0, (int) (originalFile.length() - saveToFileLength));
        assertArrayEquals(dataToCheck, bout.toByteArray());
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test download when file is fully downloaded">
        System.out.println("+ test download when file is fully downloaded");

        CommonUtil.copyFile(originalFile, tempFile);
        initFileSize = (int) tempFile.length();
        startingPosition.set(0L);
        cumulativeByteDownloaded.set(0);

        listener = new DownloadProgressListener() {

            @Override
            public void byteStart(long pos) {
                startingPosition.set(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.set(cumulativeByteDownloaded.get() + numberOfBytes);
            }

            @Override
            public void byteTotal(long total) {
            }

            @Override
            public void downloadRetry(DownloadResult result) {
            }
        };
        url = TestCommon.urlRoot + originalFileName;
        saveToFile = tempFile;
        saveToFileLength = saveToFile.length();
        fileSHA1 = CommonUtil.getSHA256String(originalFile);
        expectedLength = (int) originalFile.length();

        result = null;
        fout = null;
        bout = new ByteArrayOutputStream();
        try {
            HTTPDownloader downloader = new HTTPDownloader();
            downloader.setResumeFile(saveToFile);
            downloader.setOutputTo(bout);
            result = downloader.download(listener, new URL(url), fileSHA1, expectedLength, 0, 0);
        } finally {
            CommonUtil.closeQuietly(fout);
        }

        assertEquals(DownloadResult.SUCCEED, result);
        assertEquals(7007, startingPosition.get());
        assertEquals(originalFile.length() - initFileSize, cumulativeByteDownloaded.get());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(CommonUtil.getSHA256String(originalFile), CommonUtil.getSHA256String(saveToFile));
        assertEquals(0, bout.toByteArray().length);
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test download when file is fully downloaded but some downloaded bytes in the file are broken">
        System.out.println("+ test download when file is fully downloaded but some downloaded bytes in the file are broken");

        CommonUtil.copyFile(fullBrokenFile, tempFile);
        startingPosition.set(0L);
        cumulativeByteDownloaded.set(0);

        listener = new DownloadProgressListener() {

            @Override
            public void byteStart(long pos) {
                startingPosition.set(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.set(cumulativeByteDownloaded.get() + numberOfBytes);
            }

            @Override
            public void byteTotal(long total) {
            }

            @Override
            public void downloadRetry(DownloadResult result) {
            }
        };
        url = TestCommon.urlRoot + originalFileName;
        saveToFile = tempFile;
        saveToFileLength = saveToFile.length();
        fileSHA1 = CommonUtil.getSHA256String(originalFile);
        expectedLength = (int) originalFile.length();

        result = null;
        fout = null;
        bout = new ByteArrayOutputStream();
        try {
            HTTPDownloader downloader = new HTTPDownloader();
            downloader.setResumeFile(saveToFile);
            downloader.setOutputTo(bout);
            result = downloader.download(listener, new URL(url), fileSHA1, expectedLength, 0, 0);
        } finally {
            CommonUtil.closeQuietly(fout);
        }

        assertEquals(DownloadResult.SUCCEED, result);
        assertEquals(0, startingPosition.get());
        assertEquals(originalFile.length(), cumulativeByteDownloaded.get());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(CommonUtil.getSHA256String(originalFile), CommonUtil.getSHA256String(saveToFile));
        assertArrayEquals(CommonUtil.readFile(originalFile), bout.toByteArray());
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test download when the downloaded file is larger">
        System.out.println("+ test download when the downloaded file is larger");

        CommonUtil.copyFile(largerFile, tempFile);
        startingPosition.set(0L);
        cumulativeByteDownloaded.set(0);

        listener = new DownloadProgressListener() {

            @Override
            public void byteStart(long pos) {
                startingPosition.set(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.set(cumulativeByteDownloaded.get() + numberOfBytes);
            }

            @Override
            public void byteTotal(long total) {
            }

            @Override
            public void downloadRetry(DownloadResult result) {
            }
        };
        url = TestCommon.urlRoot + originalFileName;
        saveToFile = tempFile;
        saveToFileLength = saveToFile.length();
        fileSHA1 = CommonUtil.getSHA256String(originalFile);
        expectedLength = (int) originalFile.length();

        result = null;
        fout = null;
        bout = new ByteArrayOutputStream();
        try {
            HTTPDownloader downloader = new HTTPDownloader();
            downloader.setResumeFile(saveToFile);
            downloader.setOutputTo(bout);
            result = downloader.download(listener, new URL(url), fileSHA1, expectedLength, 0, 0);
        } finally {
            CommonUtil.closeQuietly(fout);
        }

        assertEquals(DownloadResult.SUCCEED, result);
        assertEquals(0, startingPosition.get());
        assertEquals(originalFile.length(), cumulativeByteDownloaded.get());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(CommonUtil.getSHA256String(originalFile), CommonUtil.getSHA256String(saveToFile));
        assertArrayEquals(CommonUtil.readFile(originalFile), bout.toByteArray());
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test resume download but some downloaded bytes in the file are broken with retry">
        System.out.println("+ test resume download but some downloaded bytes in the file are broken with retry");

        CommonUtil.copyFile(partBrokenFile, tempFile);
        initFileSize = (int) tempFile.length();
        startingPosition.set(0L);
        cumulativeByteDownloaded.set(0);

        final ByteArrayOutputStream retryOut = new ByteArrayOutputStream();
        listener = new DownloadProgressListener() {

            @Override
            public void byteStart(long pos) {
                startingPosition.set(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.set(cumulativeByteDownloaded.get() + numberOfBytes);
            }

            @Override
            public void byteTotal(long total) {
            }

            @Override
            public void downloadRetry(DownloadResult result) {
                cumulativeByteDownloaded.set(0);
                retryOut.reset();
            }
        };
        url = TestCommon.urlRoot + originalFileName;
        saveToFile = tempFile;
        saveToFileLength = saveToFile.length();
        fileSHA1 = CommonUtil.getSHA256String(originalFile);
        expectedLength = (int) originalFile.length();

        TestCommon.suppressErrorOutput();
        result = null;
        fout = null;
        try {
            HTTPDownloader downloader = new HTTPDownloader();
            downloader.setResumeFile(saveToFile);
            downloader.setOutputTo(retryOut);
            result = downloader.download(listener, new URL(url), fileSHA1, expectedLength, 1, 0);
        } finally {
            CommonUtil.closeQuietly(fout);
        }
        TestCommon.restoreErrorOutput();

        assertEquals(DownloadResult.SUCCEED, result);
        assertEquals(0, startingPosition.get());
        assertEquals(originalFile.length(), cumulativeByteDownloaded.get());
        assertEquals(originalFile.length(), saveToFile.length());
        assertTrue(CommonUtil.getSHA256String(originalFile).equals(CommonUtil.getSHA256String(saveToFile)));
        assertArrayEquals(CommonUtil.readFile(originalFile), retryOut.toByteArray());
        //</editor-fold>

        // test thread interrupt


        tempFile.delete();
    }
}
