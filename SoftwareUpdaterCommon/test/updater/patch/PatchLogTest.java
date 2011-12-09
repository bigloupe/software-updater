package updater.patch;

import java.util.List;
import updater.TestCommon;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import updater.util.CommonUtil;
import static org.junit.Assert.*;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchLogTest {

  protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

  public PatchLogTest() {
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
  public void test() {
    System.out.println("+++++ test +++++");

    File logFile1 = new File("PatchLogTest_lamA5_1.log");
    File logFile2 = new File("PatchLogTest_lamA5_2.log");
    File logFile3 = new File("PatchLogTest_lamA5_3.log");
    File logFile4 = new File("PatchLogTest_lamA5_4.log");
    File logFile5 = new File("PatchLogTest_lamA5_5.log");
    logFile1.delete();
    logFile2.delete();
    logFile3.delete();
    logFile4.delete();
    logFile5.delete();

    LogWriter writer1 = null, writer2 = null, writer3 = null, writer4 = null, writer5 = null;
    try {
      writer1 = new LogWriter(logFile1);
      writer2 = new LogWriter(logFile2);
      writer3 = new LogWriter(logFile3);
      writer4 = new LogWriter(logFile4);
      writer5 = new LogWriter(logFile5);

      writer1.logStart();
      writer1.logPatch(LogAction.START, 0, 1, false, "backup1", "from1", "to1");
      writer1.logPatch(LogAction.FINISH, 0);
      writer1.logPatch(LogAction.START, 1, 2, false, "backup2", "from2", "to2");
      writer1.logPatch(LogAction.FINISH, 1);
      writer1.logPatch(LogAction.START, 2, 3, false, "backup3", "from3", "to3");
      writer1.logPatch(LogAction.FAILED, 2);
      writer1.logPatch(LogAction.START, 3, 4, false, "backup4", "from4", "to4");
      writer1.logPatch(LogAction.START, 3, 4, false, "backup4", "from4", "to4");
      writer1.logPatch(LogAction.FINISH, 3);
      writer1.logPatch(LogAction.START, 4, 5, false, "backup5", "from5", "to5");
      writer1.logPatch(LogAction.FAILED, 4);
      writer1.logPatch(LogAction.START, 5, 6, false, "backup6", "from6", "to6");
      writer1.logPatch(LogAction.FAILED, 5);
      writer1.logPatch(LogAction.START, 6, 7, false, "backup7", "from7", "to7");
      writer1.logPatch(LogAction.FINISH, 6);
      writer1.logPatch(LogAction.START, 2, 3, false, "backup3", "from3", "to3");
      writer1.logPatch(LogAction.FINISH, 2);
      writer1.logEnd();

      writer2.logStart();
      writer2.logPatch(LogAction.START, 0, 1, false, "backup1", "from1", "to1");
      writer2.logPatch(LogAction.FINISH, 0);
      writer2.logPatch(LogAction.START, 1, 2, false, "backup2", "from2", "to2");
      writer2.logPatch(LogAction.START, 1, 2, false, "backup2", "from2", "to2");
      writer2.logPatch(LogAction.FINISH, 1);
      writer2.logPatch(LogAction.START, 2, 3, false, "backup3", "from3", "to3");
      writer2.logPatch(LogAction.FAILED, 2);
      writer2.logRevert(0);
      writer2.logPatch(LogAction.START, 3, 4, false, "backup4", "from4", "to4");

      writer3.logStart();
      writer3.logPatch(LogAction.START, 0, 1, false, "backup1", "from1", "to1");
      writer3.logPatch(LogAction.FAILED, 0);
      writer3.logPatch(LogAction.START, 1, 2, false, "backup2", "from2", "to2");
      writer3.logPatch(LogAction.FINISH, 1);
      writer3.logPatch(LogAction.START, 2, 3, false, "backup3", "from3", "to3");
      writer3.logPatch(LogAction.FAILED, 2);

      writer4.logStart();
      writer4.logPatch(LogAction.START, 0, 1, false, "backup1", "from1", "to1");
      writer4.logPatch(LogAction.FAILED, 0);
      writer4.logPatch(LogAction.START, 1, 2, false, "backup2", "from2", "to2");
      writer4.logPatch(LogAction.FAILED, 1);

      writer5.logStart();
      writer5.logPatch(LogAction.START, 0, 1, false, "\"backup1", "fr\"om1", "to1\"");
      writer5.logPatch(LogAction.FINISH, 0);
      writer5.logPatch(LogAction.START, 1, 2, false, "backup2", "from2", "to2");
      writer5.logPatch(LogAction.FINISH, 1);
    } catch (IOException ex) {
      fail("! Prepare log failed.");
      Logger.getLogger(PatchLogTest.class.getName()).log(Level.SEVERE, null, ex);
    } finally {
      CommonUtil.closeQuietly(writer1);
      CommonUtil.closeQuietly(writer2);
      CommonUtil.closeQuietly(writer3);
      CommonUtil.closeQuietly(writer4);
      CommonUtil.closeQuietly(writer5);
    }

    assertTrue(logFile1.exists());
    assertTrue(logFile1.length() > 10);
    assertTrue(logFile2.exists());
    assertTrue(logFile2.length() > 10);
    assertTrue(logFile3.exists());
    assertTrue(logFile3.length() > 10);
    assertTrue(logFile4.exists());
    assertTrue(logFile4.length() > 10);
    assertTrue(logFile5.exists());
    assertTrue(logFile5.length() > 10);

    try {
      LogReader reader1 = new LogReader(logFile1);
      LogReader reader2 = new LogReader(logFile2);
      LogReader reader3 = new LogReader(logFile3);
      LogReader reader4 = new LogReader(logFile4);
      LogReader reader5 = new LogReader(logFile5);

      boolean logStarted = reader1.isLogStarted();
      boolean logEnded = reader1.isLogEnded();
      List<PatchRecord> failList = reader1.getFailList();
      List<PatchRecord> revertList = reader1.getRevertList();
      int startFileIndex = reader1.getStartFileIndex();

      assertTrue(logStarted);
      assertTrue(logEnded);
      assertArrayEquals(new PatchRecord[]{new PatchRecord(4, 5, false, "backup5", "from5", "to5"), new PatchRecord(5, 6, false, "backup6", "from6", "to6")}, failList.toArray(new PatchRecord[failList.size()]));
      assertArrayEquals(new PatchRecord[]{new PatchRecord(6, 7, false, "backup7", "from7", "to7"), new PatchRecord(3, 4, false, "backup4", "from4", "to4"), new PatchRecord(2, 3, false, "backup3", "from3", "to3"), new PatchRecord(1, 2, false, "backup2", "from2", "to2"), new PatchRecord(0, 1, false, "backup1", "from1", "to1")}, revertList.toArray(new PatchRecord[revertList.size()]));
      assertEquals(7, startFileIndex);


      logStarted = reader2.isLogStarted();
      logEnded = reader2.isLogEnded();
      failList = reader2.getFailList();
      revertList = reader2.getRevertList();
      startFileIndex = reader2.getStartFileIndex();

      assertTrue(logStarted);
      assertFalse(logEnded);
      assertArrayEquals(new PatchRecord[]{new PatchRecord(0, 1, false, "backup1", "from1", "to1"), new PatchRecord(2, 3, false, "backup3", "from3", "to3"), new PatchRecord(3, 4, false, "backup4", "from4", "to4")}, failList.toArray(new PatchRecord[failList.size()]));
      assertArrayEquals(new PatchRecord[]{new PatchRecord(1, 2, false, "backup2", "from2", "to2")}, revertList.toArray(new PatchRecord[revertList.size()]));
      assertEquals(3, startFileIndex);


      logStarted = reader3.isLogStarted();
      logEnded = reader3.isLogEnded();
      failList = reader3.getFailList();
      revertList = reader3.getRevertList();
      startFileIndex = reader3.getStartFileIndex();

      assertTrue(logStarted);
      assertFalse(logEnded);
      assertArrayEquals(new PatchRecord[]{new PatchRecord(0, 1, false, "backup1", "from1", "to1"), new PatchRecord(2, 3, false, "backup3", "from3", "to3")}, failList.toArray(new PatchRecord[failList.size()]));
      assertArrayEquals(new PatchRecord[]{new PatchRecord(1, 2, false, "backup2", "from2", "to2")}, revertList.toArray(new PatchRecord[revertList.size()]));
      assertEquals(3, startFileIndex);


      logStarted = reader4.isLogStarted();
      logEnded = reader4.isLogEnded();
      failList = reader4.getFailList();
      revertList = reader4.getRevertList();
      startFileIndex = reader4.getStartFileIndex();

      assertTrue(logStarted);
      assertFalse(logEnded);
      assertArrayEquals(new PatchRecord[]{new PatchRecord(0, 1, false, "backup1", "from1", "to1"), new PatchRecord(1, 2, false, "backup2", "from2", "to2")}, failList.toArray(new PatchRecord[failList.size()]));
      assertArrayEquals(new PatchRecord[]{}, revertList.toArray(new PatchRecord[revertList.size()]));
      assertEquals(2, startFileIndex);


      logStarted = reader5.isLogStarted();
      logEnded = reader5.isLogEnded();
      failList = reader5.getFailList();
      revertList = reader5.getRevertList();
      startFileIndex = reader5.getStartFileIndex();

      assertTrue(logStarted);
      assertFalse(logEnded);
      assertArrayEquals(new PatchRecord[]{}, failList.toArray(new PatchRecord[failList.size()]));
      assertArrayEquals(new PatchRecord[]{new PatchRecord(1, 2, false, "backup2", "from2", "to2"), new PatchRecord(0, 1, false, "\"backup1", "fr\"om1", "to1\"")}, revertList.toArray(new PatchRecord[revertList.size()]));
      assertEquals(2, startFileIndex);
    } catch (IOException ex) {
      System.out.println(ex);
      fail("! Read log failed.");
      Logger.getLogger(PatchLogTest.class.getName()).log(Level.SEVERE, null, ex);
    }

    logFile1.delete();
    logFile2.delete();
    logFile3.delete();
    logFile4.delete();
    logFile5.delete();
  }
}
