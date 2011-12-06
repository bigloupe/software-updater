package updater.patch;

import java.util.List;
import java.util.HashMap;
import updater.crypto.AESKey;
import java.io.File;
import updater.TestCommon;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import updater.crypto.KeyGenerator;
import updater.util.CommonUtil;
import static org.junit.Assert.*;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchTest {

  protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";
  protected File tempDir;
  protected File softwareFolder;
  protected File tempDirForApplyPatch;

  public PatchTest() {
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
    tempDir = new File("PatchTest_tmp_dir_na4Ja");
    if (!tempDir.isDirectory()) {
      assertTrue(tempDir.mkdirs());
    }
    assertTrue(CommonUtil.truncateFolder(tempDir));

    softwareFolder = new File(tempDir.getAbsolutePath() + File.separator + "software");
    tempDirForApplyPatch = new File(tempDir.getAbsolutePath() + File.separator + "apply_patch");
  }

  @After
  public void tearDown() {
  }

  public List<PatchRecord> detailPatchingTestInit(File patch, AESKey aesKey) throws Exception {
    File logFile = new File(tempDir.getAbsolutePath() + File.separator + "action.log");
    return new Patcher(logFile).doPatch(new PatcherListener() {

      @Override
      public void patchProgress(int percentage, String message) {
      }

      @Override
      public void patchEnableCancel(boolean enable) {
      }
    }, patch, 1, aesKey, softwareFolder, tempDirForApplyPatch, new HashMap<String, String>());
  }

  @Test
  public void test() throws Exception {
    File aesKeyFile = new File(tempDir.getAbsolutePath() + File.separator + "aes.xml");
    KeyGenerator.generateAES(256, aesKeyFile);
    AESKey aesKey = AESKey.read(CommonUtil.readFile(aesKeyFile));

    File oldFolder = new File(packagePath + File.separator + "test3/software/1.0");
    File newFolder = new File(packagePath + File.separator + "test3/software/1.1");
    File patch = new File(tempDir.getAbsolutePath() + File.separator + "patch");
    File tempDirForCreatePatch = new File(tempDir.getAbsolutePath() + File.separator + "create_patch");
    File tempFileForPatchEncryption = new File(tempDir.getAbsolutePath() + File.separator + "patch.encrypted");
    tempDirForCreatePatch.mkdirs();
    tempDirForApplyPatch.mkdirs();

    PatchCreator.createPatch(oldFolder, newFolder, tempDirForCreatePatch, patch, -1, "1.0.0", "1.0.1", aesKey, tempFileForPatchEncryption);
    TestCommon.copyFolder(new File(packagePath + File.separator + "test3/1.0/"), softwareFolder);
    TestCommon.copyFolder(new File(packagePath + File.separator + "test3/temp/"), tempDirForApplyPatch);

    testStep1(patch, aesKey);
    testStep2(patch, aesKey);
    testStep3(patch, aesKey);
    testStep4(patch, aesKey);
    testStep5(patch, aesKey);
    testStep6(patch, aesKey);
    testStep7(patch, aesKey);
    testStep8(patch, aesKey);
    testStep9(patch, aesKey);
    testStep10(patch, aesKey);

    assertTrue(CommonUtil.truncateFolder(tempDir));
    tempDir.delete();
  }

  public void testStep1(File patch, AESKey aesKey) throws Exception {
    try {
      List<PatchRecord> replacementList = detailPatchingTestInit(patch, aesKey);
      assertTrue(replacementList.isEmpty());
      fail();
    } catch (Exception ex) {
      assertExistance(8, 1, false, false, false, false);
      assertExistance(7, 2, false, false, false, true);
      assertExistance(6, 3, false, false, true, false);
      CommonUtil.writeFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_1"), "8_old");
    }
  }

  public void testStep2(File patch, AESKey aesKey) throws Exception {
    try {
      List<PatchRecord> replacementList = detailPatchingTestInit(patch, aesKey);
      assertTrue(replacementList.isEmpty());
      fail();
    } catch (Exception ex) {
      assertExistance(8, 1, false, false, false, true);
      assertExistance(7, 2, false, false, false, true);
      assertExistance(6, 3, false, false, false, true);
      assertExistance(5, 4, true, true, true, false);
      assertExistance(4, 5, true, true, false, false);
      new File(softwareFolder.getAbsolutePath() + File.separator + "5").delete();
      CommonUtil.truncateFile(new File(softwareFolder.getAbsolutePath() + File.separator + "5"));
    }
  }

  public void testStep3(File patch, AESKey aesKey) throws Exception {
    try {
      List<PatchRecord> replacementList = detailPatchingTestInit(patch, aesKey);
      assertTrue(replacementList.isEmpty());
      fail();
    } catch (Exception ex) {
      assertExistance(8, 1, false, false, false, true);
      assertExistance(7, 2, false, false, false, true);
      assertExistance(6, 3, false, false, false, true);

      assertExistance(5, 4, false, false, false, true);
      assertExistance(4, 5, true, true, false, false);
      assertExistance(3, 6, false, false, true, false);
      assertExistance(2, 7, true, true, true, false);
      assertTrue(new File(softwareFolder.getAbsolutePath() + File.separator + "2/2").exists());

      new File(softwareFolder.getAbsolutePath() + File.separator + "3").delete();
      new File(softwareFolder.getAbsolutePath() + File.separator + "3").mkdirs();
    }
  }

  public void testStep4(File patch, AESKey aesKey) throws Exception {
    try {
      List<PatchRecord> replacementList = detailPatchingTestInit(patch, aesKey);
      assertTrue(replacementList.isEmpty());
      fail();
    } catch (Exception ex) {
      assertExistance(8, 1, false, false, false, true);
      assertExistance(7, 2, false, false, false, true);
      assertExistance(6, 3, false, false, false, true);

      assertExistance(5, 4, false, false, false, true);
      assertExistance(4, 5, true, true, false, false);

      assertExistance(3, 6, true, true, false, true);
      assertExistance(2, 7, true, true, true, false);
      assertTrue(new File(softwareFolder.getAbsolutePath() + File.separator + "2/2").exists());
      assertExistance(1, 8, true, true, false, true);
      assertExistance(10, 9, false, false, true, false);
      assertExistance(11, 10, true, true, false, false);

      new File(softwareFolder.getAbsolutePath() + File.separator + "10").delete();
      new File(softwareFolder.getAbsolutePath() + File.separator + "10").mkdirs();
    }
  }

  public void testStep5(File patch, AESKey aesKey) throws Exception {
    try {
      List<PatchRecord> replacementList = detailPatchingTestInit(patch, aesKey);
      assertTrue(replacementList.isEmpty());
      fail();
    } catch (Exception ex) {
      assertExistance(8, 1, false, false, false, true);
      assertExistance(7, 2, false, false, false, true);
      assertExistance(6, 3, false, false, false, true);

      assertExistance(5, 4, false, false, false, true);
      assertExistance(4, 5, true, true, false, false);

      assertExistance(3, 6, true, true, false, true);
      assertExistance(2, 7, true, true, true, false);
      assertTrue(new File(softwareFolder.getAbsolutePath() + File.separator + "2/2").exists());
      assertExistance(1, 8, true, true, false, true);

      assertExistance(10, 9, true, true, true, false);
      assertExistance(11, 10, true, true, true, false);
      assertExistance(12, 11, true, true, true, false);
      assertExistance(13, 12, false, false, true, false);

      new File(softwareFolder.getAbsolutePath() + File.separator + "12").delete();
      CommonUtil.writeFile(new File(softwareFolder.getAbsolutePath() + File.separator + "12"), "");
    }
  }

  public void testStep6(File patch, AESKey aesKey) throws Exception {
    try {
      List<PatchRecord> replacementList = detailPatchingTestInit(patch, aesKey);
      assertTrue(replacementList.isEmpty());
      fail();
    } catch (Exception ex) {
      assertExistance(8, 1, false, false, false, true);
      assertExistance(7, 2, false, false, false, true);
      assertExistance(6, 3, false, false, false, true);

      assertExistance(5, 4, false, false, false, true);
      assertExistance(4, 5, true, true, false, false);

      assertExistance(3, 6, true, true, false, true);
      assertExistance(2, 7, true, true, true, false);
      assertTrue(new File(softwareFolder.getAbsolutePath() + File.separator + "2/2").exists());
      assertExistance(1, 8, true, true, false, true);

      assertExistance(10, 9, true, true, true, false);
      assertExistance(11, 10, true, true, true, false);

      assertExistance(12, 11, false, false, true, false);
      assertExistance(13, 12, false, false, true, false);
      assertExistance(14, 13, false, false, true, false);
      assertExistance(15, 14, false, false, false, false);

      new File(softwareFolder.getAbsolutePath() + File.separator + "14").delete();
      CommonUtil.writeFile(new File(softwareFolder.getAbsolutePath() + File.separator + "14"), "14");
    }
  }

  public void testStep7(File patch, AESKey aesKey) throws Exception {
    try {
      List<PatchRecord> replacementList = detailPatchingTestInit(patch, aesKey);
      assertTrue(replacementList.isEmpty());
      fail();
    } catch (Exception ex) {
      assertExistance(8, 1, false, false, false, true);
      assertExistance(7, 2, false, false, false, true);
      assertExistance(6, 3, false, false, false, true);

      assertExistance(5, 4, false, false, false, true);
      assertExistance(4, 5, true, true, false, false);

      assertExistance(3, 6, true, true, false, true);
      assertExistance(2, 7, true, true, true, false);
      assertTrue(new File(softwareFolder.getAbsolutePath() + File.separator + "2/2").exists());
      assertExistance(1, 8, true, true, false, true);

      assertExistance(10, 9, true, true, true, false);
      assertExistance(11, 10, true, true, true, false);

      assertExistance(12, 11, false, false, true, false);
      assertExistance(13, 12, false, false, true, false);

      assertExistance(14, 13, false, false, true, false);
      assertExistance(15, 14, false, false, true, false);
      assertExistance(9, 15, true, true, true, false);
      assertExistance(24, 16, true, true, true, false);
      assertExistance(25, 17, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "25"))).equals("25_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_17"))).equals("25_old"));

      new File(softwareFolder.getAbsolutePath() + File.separator + "24").delete();
      CommonUtil.writeFile(new File(softwareFolder.getAbsolutePath() + File.separator + "24"), "24_old");
    }
  }

  public void testStep8(File patch, AESKey aesKey) throws Exception {
    try {
      List<PatchRecord> replacementList = detailPatchingTestInit(patch, aesKey);
      assertTrue(replacementList.isEmpty());
      fail();
    } catch (Exception ex) {
      assertExistance(8, 1, false, false, false, true);
      assertExistance(7, 2, false, false, false, true);
      assertExistance(6, 3, false, false, false, true);

      assertExistance(5, 4, false, false, false, true);
      assertExistance(4, 5, true, true, false, false);

      assertExistance(3, 6, true, true, false, true);
      assertExistance(2, 7, true, true, true, false);
      assertTrue(new File(softwareFolder.getAbsolutePath() + File.separator + "2/2").exists());
      assertExistance(1, 8, true, true, false, true);

      assertExistance(10, 9, true, true, true, false);
      assertExistance(11, 10, true, true, true, false);

      assertExistance(12, 11, false, false, true, false);
      assertExistance(13, 12, false, false, true, false);

      assertExistance(14, 13, false, false, true, false);
      assertExistance(15, 14, false, false, true, false);
      assertExistance(9, 15, true, true, true, false);

      assertExistance(24, 16, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "24"))).equals("24_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_16"))).equals("24_old"));
      assertExistance(25, 17, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "25"))).equals("25_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_17"))).equals("25_old"));
      assertExistance(26, 18, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "26"))).equals("26_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_18"))).equals("26_old"));
      assertExistance(27, 19, false, false, true, false);
      assertExistance(28, 20, false, false, false, true);
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_20"))).equals("28_old"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "20"))).equals("28_new"));

      new File(softwareFolder.getAbsolutePath() + File.separator + "27").delete();
      CommonUtil.writeFile(new File(softwareFolder.getAbsolutePath() + File.separator + "27"), "27_old");
    }
  }

  public void testStep9(File patch, AESKey aesKey) throws Exception {
    try {
      List<PatchRecord> replacementList = detailPatchingTestInit(patch, aesKey);
      assertTrue(replacementList.isEmpty());
      fail();
    } catch (Exception ex) {
      assertExistance(8, 1, false, false, false, true);
      assertExistance(7, 2, false, false, false, true);
      assertExistance(6, 3, false, false, false, true);

      assertExistance(5, 4, false, false, false, true);
      assertExistance(4, 5, true, true, false, false);

      assertExistance(3, 6, true, true, false, true);
      assertExistance(2, 7, true, true, true, false);
      assertTrue(new File(softwareFolder.getAbsolutePath() + File.separator + "2/2").exists());
      assertExistance(1, 8, true, true, false, true);

      assertExistance(10, 9, true, true, true, false);
      assertExistance(11, 10, true, true, true, false);

      assertExistance(12, 11, false, false, true, false);
      assertExistance(13, 12, false, false, true, false);

      assertExistance(14, 13, false, false, true, false);
      assertExistance(15, 14, false, false, true, false);
      assertExistance(9, 15, true, true, true, false);

      assertExistance(24, 16, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "24"))).equals("24_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_16"))).equals("24_old"));
      assertExistance(25, 17, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "25"))).equals("25_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_17"))).equals("25_old"));
      assertExistance(26, 18, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "26"))).equals("26_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_18"))).equals("26_old"));
      assertExistance(27, 19, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "27"))).equals("27_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_19"))).equals("27_old"));
      assertExistance(28, 20, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "28"))).equals("28_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_20"))).equals("28_old"));
      assertExistance(29, 21, false, false, false, true);
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_21"))).equals("29_old"));

      new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_21").delete();
      new File(softwareFolder.getAbsolutePath() + File.separator + "29").delete();
      CommonUtil.writeFile(new File(softwareFolder.getAbsolutePath() + File.separator + "29"), "29_old");
    }
  }

  public void testStep10(File patch, AESKey aesKey) throws Exception {
    try {
      List<PatchRecord> replacementList = detailPatchingTestInit(patch, aesKey);
      assertTrue(replacementList.isEmpty());
      assertExistance(8, 1, false, false, false, true);
      assertExistance(7, 2, false, false, false, true);
      assertExistance(6, 3, false, false, false, true);

      assertExistance(5, 4, false, false, false, true);
      assertExistance(4, 5, true, true, false, false);

      assertExistance(3, 6, true, true, false, true);
      assertExistance(2, 7, true, true, true, false);
      assertTrue(new File(softwareFolder.getAbsolutePath() + File.separator + "2/2").exists());
      assertExistance(1, 8, true, true, false, true);

      assertExistance(10, 9, true, true, true, false);
      assertExistance(11, 10, true, true, true, false);

      assertExistance(12, 11, false, false, true, false);
      assertExistance(13, 12, false, false, true, false);

      assertExistance(14, 13, false, false, true, false);
      assertExistance(15, 14, false, false, true, false);
      assertExistance(9, 15, true, true, true, false);

      assertExistance(24, 16, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "24"))).equals("24_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_16"))).equals("24_old"));
      assertExistance(25, 17, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "25"))).equals("25_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_17"))).equals("25_old"));
      assertExistance(26, 18, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "26"))).equals("26_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_18"))).equals("26_old"));
      assertExistance(27, 19, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "27"))).equals("27_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_19"))).equals("27_old"));
      assertExistance(28, 20, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "28"))).equals("28_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_20"))).equals("28_old"));

      assertExistance(29, 21, false, false, true, true);
      assertTrue(new String(CommonUtil.readFile(new File(softwareFolder.getAbsolutePath() + File.separator + "29"))).equals("29_new"));
      assertTrue(new String(CommonUtil.readFile(new File(tempDirForApplyPatch.getAbsolutePath() + File.separator + "old_21"))).equals("29_old"));
    } catch (Exception ex) {
      fail();
    }
  }

  public void assertExistance(int fileIndex, int operationId, boolean destIsDirectory, boolean backupIsDirectory, boolean destExist, boolean backupExist) {
    File destFile = new File(softwareFolder + File.separator + fileIndex);
    File backupFile = new File(tempDirForApplyPatch + File.separator + "old_" + operationId);
    if (destExist) {
      if (destIsDirectory) {
        assertTrue(destFile.isDirectory());
      } else {
        assertTrue(destFile.isFile());
      }
      assertTrue(destFile.exists());
    } else {
      assertFalse(destFile.exists());
    }
    if (backupExist) {
      if (backupIsDirectory) {
        assertTrue(backupFile.isDirectory());
      } else {
        assertTrue(backupFile.isFile());
      }
      assertTrue(backupFile.exists());
    } else {
      assertFalse(backupFile.exists());
    }
  }

//  @Test
  public void patchingTest() throws Exception {
    System.out.println("+++++ patchingTest +++++");

    File aesKeyFile = new File(tempDir.getAbsolutePath() + File.separator + "aes.xml");
    KeyGenerator.generateAES(256, aesKeyFile);
    AESKey aesKey = AESKey.read(CommonUtil.readFile(aesKeyFile));


    // self-made simple test case
    File packedZipFile = new File(packagePath + "test1_pack");
    File unzipToFolder = new File("test1_pack/");
    zipPackTest(packedZipFile, unzipToFolder, tempDir, aesKey);
    assertTrue(CommonUtil.truncateFolder(tempDir));

    // Discuz! upgrade
    packedZipFile = new File(packagePath + "test2_pack");
    unzipToFolder = new File("test2_pack/");
    zipPackTest(packedZipFile, unzipToFolder, tempDir, null);
    assertTrue(CommonUtil.truncateFolder(tempDir));

    // phpBB 1.4.4 -> 2.0
    packedZipFile = new File(packagePath + "test3_pack");
    unzipToFolder = new File("test3_pack/");
    zipPackTest(packedZipFile, unzipToFolder, tempDir, aesKey);
    assertTrue(CommonUtil.truncateFolder(tempDir));

    // phpBB 2.0 -> 3.0.9
    packedZipFile = new File(packagePath + "test4_pack");
    unzipToFolder = new File("test4_pack/");
    zipPackTest(packedZipFile, unzipToFolder, tempDir, null);
    assertTrue(CommonUtil.truncateFolder(tempDir));

    CommonUtil.truncateFolder(tempDir);
    tempDir.delete();
  }

  public static void zipPackTest(File packedZipFile, File unzipToFolder, File tempDir, AESKey aesKey) throws Exception {
    if (!unzipToFolder.isDirectory()) {
      assertTrue(unzipToFolder.mkdirs());
    }
    if (!tempDir.isDirectory()) {
      assertTrue(tempDir.mkdirs());
    }

    CommonUtil.truncateFolder(unzipToFolder);
    TestCommon.unzip(packedZipFile, unzipToFolder);

    // follow the folder name and change the path to do real software patching test
    File oldFolder = new File(unzipToFolder.getAbsolutePath() + File.separator + "PatchTest_old");
    File newFolder = new File(unzipToFolder.getAbsolutePath() + File.separator + "PatchTest_new");
    File newOverOldFolder = new File(unzipToFolder.getAbsolutePath() + File.separator + "PatchTest_new_over_old"); // this is the folder that contains all files after applying full patch of 'new' on 'old'


    System.out.println("+ patching");
    File patch = new File(tempDir.getAbsolutePath() + File.separator + "patch.patch");
    File tempDirForCreatePatch = new File(tempDir.getAbsolutePath() + File.separator + "create_patch");
    File tempFileForPatchEncryption = new File(patch.getAbsolutePath() + ".encrypted");
    File tempDirForPatch = new File(tempDir.getAbsolutePath() + File.separator + "patch");
    File logFile = new File(tempDirForCreatePatch.getAbsolutePath() + File.separator + "action.log");
    File tempDirForApplyPatch = new File(tempDir.getAbsolutePath() + File.separator + "apply_patch");
    tempDirForCreatePatch.mkdirs();
    tempDirForPatch.mkdirs();
    tempDirForApplyPatch.mkdirs();

    // create patch of new from old (upgrade patch)
    PatchCreator.createPatch(oldFolder, newFolder, tempDirForCreatePatch, patch, -1, "1.0.0", "1.0.1", aesKey, tempFileForPatchEncryption);
    // copy 'old' folder to new directory
    TestCommon.copyFolder(oldFolder, tempDirForPatch);
    // apply the patch on 'old' folder
    Patcher patcher = new Patcher(logFile);
    patcher.doPatch(new PatcherListener() {

      @Override
      public void patchProgress(int percentage, String message) {
      }

      @Override
      public void patchEnableCancel(boolean enable) {
      }
    }, patch, 1, aesKey, tempDirForPatch, tempDirForApplyPatch, new HashMap<String, String>());
    // compare the new 'old' folder and the 'new' folder
    assertTrue(TestCommon.compareFolder(tempDirForPatch, newFolder));


    System.out.println("+ full-pack patching");
    File fullPatch = new File(tempDir.getAbsolutePath() + File.separator + "full_patch.patch");
    File tempDirForCreateFullPatch = new File(tempDir.getAbsolutePath() + File.separator + "create_full_patch");
    File tempFileForFullPatchEncryption = new File(fullPatch.getAbsolutePath() + ".encrypted");
    File tempDirForFullPatch = new File(tempDir.getAbsolutePath() + File.separator + "full_patch");
    File logFileForFullPatch = new File(tempDirForCreateFullPatch.getAbsolutePath() + File.separator + "action_full_patch.log");
    File tempDirForApplyFullPatch = new File(tempDir.getAbsolutePath() + File.separator + "apply_full_patch");
    tempDirForCreateFullPatch.mkdirs();
    tempDirForFullPatch.mkdirs();
    tempDirForApplyFullPatch.mkdirs();

    // create patch of new from old (full patch)
    PatchCreator.createFullPatch(newFolder, fullPatch, -1, "1.0.0", null, "1.0.1", aesKey, tempFileForFullPatchEncryption);
    // copy 'old' folder to new directory
    assertTrue(CommonUtil.truncateFolder(tempDirForFullPatch));
    TestCommon.copyFolder(oldFolder, tempDirForFullPatch);
    // apply the patch on 'old' folder
    patcher = new Patcher(logFileForFullPatch);
    patcher.doPatch(new PatcherListener() {

      @Override
      public void patchProgress(int percentage, String message) {
      }

      @Override
      public void patchEnableCancel(boolean enable) {
      }
    }, fullPatch, 1, aesKey, tempDirForFullPatch, tempDirForApplyFullPatch, new HashMap<String, String>());
    // compare the new 'old' folder and the 'new_over_old' folder
    assertTrue(TestCommon.compareFolder(tempDirForFullPatch, newOverOldFolder));


    System.out.println("+ extraction and packing");
    File repackedPatch = new File(tempDir.getAbsolutePath() + File.separator + "patch.repacked.patch");
    File tempFileForExtractPatchEncryption = new File(repackedPatch.getAbsolutePath() + ".encrypted");
    File tempFileForExtractPatchDecryption = new File(repackedPatch.getAbsolutePath() + ".decrypted");
    File tempDirForExtractPatch = new File(tempDir.getAbsolutePath() + File.separator + "extract");
    File repackedFullPatch = new File(tempDir.getAbsolutePath() + File.separator + "full_patch.repacked.patch");
    File tempFileForExtractFullPatchEncryption = new File(repackedFullPatch.getAbsolutePath() + ".encrypted");
    File tempFileForExtractFullPatchDecryption = new File(repackedFullPatch.getAbsolutePath() + ".decrypted");
    File tempDirForExtractFullPatch = new File(tempDir.getAbsolutePath() + File.separator + "extract_full");

    // extract the patch
    PatchExtractor.extract(patch, tempDirForExtractPatch, aesKey, tempFileForExtractPatchDecryption);
    // pack the patch
    PatchPacker.pack(tempDirForExtractPatch, repackedPatch, aesKey, tempFileForExtractPatchEncryption);
    // compare the newly packed patch with the original patch
    assertTrue(CommonUtil.compareFile(repackedPatch, patch));

    // extract the full patch
    PatchExtractor.extract(fullPatch, tempDirForExtractFullPatch, aesKey, tempFileForExtractFullPatchDecryption);
    // pack the full patch
    PatchPacker.pack(tempDirForExtractFullPatch, repackedFullPatch, aesKey, tempFileForExtractFullPatchEncryption);
    // compare the newly packed full patch with the original full patch
    assertTrue(CommonUtil.compareFile(repackedFullPatch, fullPatch));


    CommonUtil.truncateFolder(unzipToFolder);
    unzipToFolder.delete();
  }
}
