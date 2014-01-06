package updater.downloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.transform.TransformerException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import updater.TestCommon;
import updater.script.Catalog;
import updater.script.InvalidFormatException;
import updater.script.Patch;
import updater.util.CommonUtil;
import updater.util.DownloadResult;

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
   * Test of getSuitablePatches method, of class SoftwareUpdater. Very simple 
   * test case only.
   */
  @Test
  public void testGetPatches_Catalog_String() {
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


    List<Patch> result = PatchDownloader.getSuitablePatches(catalog, "1.0.0", false);
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


    result = PatchDownloader.getSuitablePatches(catalog, "1.0.2", false);
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

  @Test
  public void testGetPatches_Catalog_String2() {
    System.out.println("+++++ testGetPatches_Catalog_String2 +++++");
    System.out.println("+ full patch only");

    byte[] catalogData = null;
    try {
      catalogData = Util.readFile(new File(packagePath + "PatchDownloaderTest_getPatches2.xml"));
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


    List<Patch> result = PatchDownloader.getSuitablePatches(catalog, "1.0.0", true);
    assertEquals(1, result.size());

    int totalSize = 0;

    Patch update = result.get(0);
    totalSize += update.getDownloadLength();
    assertEquals("1.0.0", update.getVersionFromSubsequent());
    assertEquals("2.0.0", update.getVersionTo());

    assertEquals(200, totalSize);
  }

  /**
   * Test of getCatalog method, of class PatchDownloader.
   * This test depends on some functions in /updater/util/Util.java.
   */
  @Test
  public void testGetCatalog() throws IOException, InvalidKeySpecException, InvalidFormatException {
    System.out.println("+++++ testGetCatalog +++++");

    String xmlFileName = "PatchDownloaderTest_getCatalog.xml";
    String manipulatedXmlFileName = "PatchDownloaderTest_getCatalog_manipulated.xml";
    File originalFile = new File(packagePath + xmlFileName);
    String originalFileString = null;
    try {
      originalFileString = new String(Util.readFile(originalFile), "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      Logger.getLogger(PatchDownloaderTest.class.getName()).log(Level.SEVERE, null, ex);
    }
    assertTrue(originalFileString != null);

    //<editor-fold defaultstate="collapsed" desc="test normal request">
    System.out.println("+ test normal request");

    String url = TestCommon.urlRoot + manipulatedXmlFileName;
    long lastUpdateDate = 0L;
    RSAPublicKey key = Util.getPublicKey(new BigInteger(TestCommon.modulusString, 16), new BigInteger(TestCommon.publicExponentString, 16));
    ByteArrayOutputStream cout = new ByteArrayOutputStream();
    DownloadResult result = PatchDownloader.getCatalog(cout, url, lastUpdateDate, key, new BigInteger(TestCommon.modulusString, 16).bitLength() / 8);

    assertNotNull(result);
    assertFalse(result == DownloadResult.FILE_NOT_MODIFIED);
    Catalog catalog = Catalog.read(cout.toByteArray());
    assertNotNull(catalog);
    try {
      assertEquals(originalFileString, new String(catalog.output(), "UTF-8"));
    } catch (TransformerException ex) {
      Logger.getLogger(PatchDownloaderTest.class.getName()).log(Level.SEVERE, null, ex);
      fail("! Invalid output format.");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="test If-Modified-Since header">
    System.out.println("- test If-Modified-Since header");

    url = TestCommon.urlRoot + manipulatedXmlFileName;
    lastUpdateDate = System.currentTimeMillis() - 2000;
    key = Util.getPublicKey(new BigInteger(TestCommon.modulusString, 16), new BigInteger(TestCommon.publicExponentString, 16));
    cout = new ByteArrayOutputStream();
    result = PatchDownloader.getCatalog(cout, url, lastUpdateDate, key, new BigInteger(TestCommon.modulusString, 16).bitLength() / 8);

    assertNotNull(result);
    assertTrue(result == DownloadResult.FILE_NOT_MODIFIED);
    assertEquals(0, cout.toByteArray().length);
    //</editor-fold>
  }

  /**
   * Test of getPatch method, of class PatchDownloader.
   */
  @Test
  public void testGetPatch() throws IOException, InvalidFormatException {
    System.out.println("+++++ testGetPatch +++++");

    File getPatchTestFolder = new File("testGetPatch");
    getPatchTestFolder.mkdirs();
    assertTrue(getPatchTestFolder.isDirectory());
    assertTrue(CommonUtil.truncateFolder(getPatchTestFolder));

    ByteArrayOutputStream cout = new ByteArrayOutputStream();
    DownloadResult result = PatchDownloader.getCatalog(cout, TestCommon.urlRoot + "PatchDownloaderTest_getPatch_catalog.xml", 0L, null, -1);
    assertEquals(DownloadResult.SUCCEED, result);

    Catalog catalog = Catalog.read(cout.toByteArray());
    assertNotNull(catalog);
    List<Patch> patches = catalog.getPatchs();
    assertEquals(2, patches.size());
    Patch patch1 = patches.get(0);
    assertEquals("http://localhost/SoftwareUpdaterTest/PatchDownloaderTest_getPatch_1.4.4_2.0.patch", patch1.getDownloadUrl());
    assertEquals("6d7d2dfe32d56de62bf9b8ba487382db241e31491dc574dddc5642edb0ed70b2", patch1.getDownloadChecksum());
    assertEquals(505058, patch1.getDownloadLength());
    Patch patch2 = patches.get(1);
    assertEquals("http://localhost/SoftwareUpdaterTest/PatchDownloaderTest_getPatch_2.0_3.0.9.patch", patch2.getDownloadUrl());
    assertEquals("0f3f9c3e5f4deb43de2d84e446743806220b88b039cf9cc941cccfc905ea7fbc", patch2.getDownloadChecksum());
    assertEquals(1340810, patch2.getDownloadLength());

    File clientScriptTemp = new File("PatchDownloaderTest_getPatch_client.xml");
    CommonUtil.copyFile(new File(packagePath + "PatchDownloaderTest_getPatch_client.xml"), clientScriptTemp);

    final AtomicBoolean progressIncrementCorrect = new AtomicBoolean(true);
    final AtomicInteger progressRecord = new AtomicInteger(0);
    final AtomicInteger numberOfPatchDownloaded = new AtomicInteger(0);
    PatchDownloader.downloadPatches(new DownloadPatchesListener() {

      @Override
      public void downloadPatchesPatchDownloaded(Patch patch) throws IOException {
        numberOfPatchDownloaded.incrementAndGet();
      }

      @Override
      public void downloadPatchesProgress(int progress) {
        if (progress < progressRecord.get()) {
          progressIncrementCorrect.set(false);
        }
        progressRecord.set(progress);
      }

      @Override
      public void downloadPatchesMessage(String message) {
      }
    }, patches, getPatchTestFolder.getAbsolutePath(), 0, 0);
    assertEquals(2, numberOfPatchDownloaded.get());
    assertTrue(progressIncrementCorrect.get());
    assertEquals(100, progressRecord.get());

    File downloadedPatch1 = new File(getPatchTestFolder.getAbsolutePath() + File.separator + "1.patch");
    File downloadedPatch2 = new File(getPatchTestFolder.getAbsolutePath() + File.separator + "2.patch");
    assertEquals(patch1.getDownloadChecksum(), CommonUtil.getSHA256String(downloadedPatch1));
    assertEquals(505058L, downloadedPatch1.length());
    assertEquals(patch2.getDownloadChecksum(), CommonUtil.getSHA256String(downloadedPatch2));
    assertEquals(1340810L, downloadedPatch2.length());

    assertTrue(CommonUtil.truncateFolder(getPatchTestFolder));
    getPatchTestFolder.delete();

    clientScriptTemp.delete();
  }
}
