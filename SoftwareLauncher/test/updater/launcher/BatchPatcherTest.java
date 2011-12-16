package updater.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import updater.TestCommon;
import updater.script.Patch;
import updater.util.CommonUtil;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class BatchPatcherTest {

  protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

  public BatchPatcherTest() {
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
  public void testDoPatch() throws Exception {
    System.out.println("+++++ testDoPatch +++++");

    File doPatchTestFolder = new File("testDoPatch");
    doPatchTestFolder.mkdirs();
    assertTrue(doPatchTestFolder.isDirectory());
    assertTrue(CommonUtil.truncateFolder(doPatchTestFolder));

    File doPatchTestTempFolder = new File(doPatchTestFolder.getAbsolutePath() + File.separator + "temp");
    assertTrue(doPatchTestTempFolder.mkdir());

    CommonUtil.copyFile(new File(packagePath + "BatchPatcherTest_doPatch_1.4.4_2.0.patch"), new File(doPatchTestTempFolder.getAbsolutePath() + File.separator + "1.patch"));
    CommonUtil.copyFile(new File(packagePath + "BatchPatcherTest_doPatch_2.0_3.0.9.patch"), new File(doPatchTestTempFolder.getAbsolutePath() + File.separator + "2.patch"));
    assertTrue(new File(doPatchTestTempFolder.getAbsolutePath() + File.separator + "1.patch").exists());
    assertTrue(new File(doPatchTestTempFolder.getAbsolutePath() + File.separator + "2.patch").exists());

    TestCommon.unzip(new File(packagePath + "BatchPatcherTest_doPatch_phpBB.zip"), doPatchTestFolder);
    File softwareFolder = new File(doPatchTestFolder.getAbsolutePath() + File.separator + "1.4.4");
    File newVersionFolder = new File(doPatchTestFolder.getAbsolutePath() + File.separator + "3.0.9");

    List<Patch> patches = new ArrayList<Patch>();
    patches.add(new Patch(1,
            "patch", "1.4.4", null, "2.0",
            null, null, -1,
            null, null, null,
            null, null));
    patches.add(new Patch(2,
            "patch", "2.0", null, "3.0.9",
            null, null, -1,
            null, null, null,
            null, null));

    final AtomicBoolean invalidPatch = new AtomicBoolean(false);
    final AtomicInteger numberOfPatchFinished = new AtomicInteger(0);
    final AtomicBoolean progressIncrementCorrect = new AtomicBoolean(true);
    final AtomicInteger progressRecord = new AtomicInteger(0);
    BatchPatcher batchPatcher = new BatchPatcher();
    batchPatcher.doPatch(new BatchPatchListener() {

      @Override
      public void patchInvalid(Patch patch) throws IOException {
        invalidPatch.set(true);
      }

      @Override
      public void patchFinished(Patch patch) throws IOException {
        numberOfPatchFinished.incrementAndGet();
      }

      @Override
      public void patchProgress(int percentage, String message) {
        if (percentage < progressRecord.get()) {
          progressIncrementCorrect.set(false);
        }
        progressRecord.set(percentage);
      }

      @Override
      public void patchEnableCancel(boolean enable) {
      }
    }, softwareFolder, doPatchTestTempFolder, "1.4.4", patches);
    assertFalse(invalidPatch.get());
    assertEquals(2, numberOfPatchFinished.get());
    assertTrue(progressIncrementCorrect.get());
    assertEquals(100, progressRecord.get());

    assertTrue(TestCommon.compareFolder(softwareFolder, newVersionFolder));

    assertTrue(CommonUtil.truncateFolder(doPatchTestFolder));
    assertTrue(doPatchTestFolder.delete());
  }
}
