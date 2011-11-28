package updater.patch;

import java.util.List;
import updater.patch.PatchLogReader.PatchRecord;
import updater.patch.PatchLogWriter.Action;
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

        PatchLogWriter writer1 = null, writer2 = null, writer3 = null, writer4 = null, writer5 = null;
        try {
            writer1 = new PatchLogWriter(logFile1);
            writer2 = new PatchLogWriter(logFile2);
            writer3 = new PatchLogWriter(logFile3);
            writer4 = new PatchLogWriter(logFile4);
            writer5 = new PatchLogWriter(logFile5);

            writer1.logStart(1, "1.0.0", "1.0.1");
            writer1.logPatch(Action.START, 0, OperationType.PATCH, "backup1", "from1", "to1");
            writer1.logPatch(Action.FINISH, 0, OperationType.PATCH, "", "", "");
            writer1.logPatch(Action.START, 1, OperationType.NEW, "backup2", "from2", "to2");
            writer1.logPatch(Action.FINISH, 1, OperationType.NEW, "", "", "");
            writer1.logPatch(Action.START, 2, OperationType.REMOVE, "backup3", "from3", "to3");
            writer1.logPatch(Action.FAILED, 2, OperationType.REMOVE, "", "", "");
            writer1.logPatch(Action.START, 3, OperationType.PATCH, "backup4", "from4", "to4");
            writer1.logPatch(Action.START, 3, OperationType.PATCH, "backup4", "from4", "to4");
            writer1.logPatch(Action.FINISH, 3, OperationType.PATCH, "", "", "");
            writer1.logPatch(Action.START, 4, OperationType.PATCH, "backup5", "from5", "to5");
            writer1.logPatch(Action.FAILED, 4, OperationType.PATCH, "", "", "");
            writer1.logPatch(Action.START, 5, OperationType.REMOVE, "backup6", "from6", "to6");
            writer1.logPatch(Action.FAILED, 5, OperationType.REMOVE, "", "", "");
            writer1.logPatch(Action.START, 6, OperationType.PATCH, "backup7", "from7", "to7");
            writer1.logPatch(Action.FINISH, 6, OperationType.PATCH, "", "", "");
            writer1.logEnd();

            writer2.logStart(2, "1.0.1", "1.0.3");
            writer2.logPatch(Action.START, 0, OperationType.PATCH, "backup1", "from1", "to1");
            writer2.logPatch(Action.FINISH, 0, OperationType.PATCH, "", "", "");
            writer2.logPatch(Action.START, 1, OperationType.NEW, "backup2", "from2", "to2");
            writer2.logPatch(Action.START, 1, OperationType.NEW, "backup2", "from2", "to2");
            writer2.logPatch(Action.FINISH, 1, OperationType.NEW, "", "", "");
            writer2.logPatch(Action.START, 2, OperationType.REMOVE, "backup3", "from3", "to3");
            writer2.logPatch(Action.FAILED, 2, OperationType.REMOVE, "", "", "");
            writer2.logPatch(Action.START, 3, OperationType.PATCH, "backup4", "from4", "to4");

            writer3.logStart(3, "1.0.3", "1.0.5");
            writer3.logPatch(Action.START, 0, OperationType.PATCH, "backup1", "from1", "to1");
            writer3.logPatch(Action.FAILED, 0, OperationType.PATCH, "", "", "");
            writer3.logPatch(Action.START, 1, OperationType.NEW, "backup2", "from2", "to2");
            writer3.logPatch(Action.FINISH, 1, OperationType.NEW, "", "", "");
            writer3.logPatch(Action.START, 2, OperationType.REMOVE, "backup3", "from3", "to3");
            writer3.logPatch(Action.FAILED, 2, OperationType.REMOVE, "", "", "");

            writer4.logStart(4, "1.0.5", "1.0.7");
            writer4.logPatch(Action.START, 0, OperationType.PATCH, "backup1", "from1", "to1");
            writer4.logPatch(Action.FAILED, 0, OperationType.PATCH, "", "", "");
            writer4.logPatch(Action.START, 1, OperationType.NEW, "backup2", "from2", "to2");
            writer4.logPatch(Action.FAILED, 1, OperationType.NEW, "", "", "");

            writer5.logStart(5, "1.0.7", "1.0.8");
            writer5.logPatch(Action.START, 0, OperationType.PATCH, "backup1", "from1", "to1");
            writer5.logPatch(Action.FINISH, 0, OperationType.PATCH, "", "", "");
            writer5.logPatch(Action.START, 1, OperationType.NEW, "backup2", "from2", "to2");
            writer5.logPatch(Action.FINISH, 1, OperationType.NEW, "", "", "");
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
            PatchLogReader reader1 = new PatchLogReader(logFile1);
            PatchLogReader reader2 = new PatchLogReader(logFile2);
            PatchLogReader reader3 = new PatchLogReader(logFile3);
            PatchLogReader reader4 = new PatchLogReader(logFile4);
            PatchLogReader reader5 = new PatchLogReader(logFile5);

            boolean logStarted = reader1.isLogStarted();
            boolean logEnded = reader1.isLogEnded();
            List<PatchRecord> failList = reader1.getFailList();
            List<PatchRecord> revertList = reader1.getRevertList();
            int startFileIndex = reader1.getStartFileIndex();
            PatchRecord unfinishedReplacement = reader1.getUnfinishedReplacement();

            assertTrue(logStarted);
            assertTrue(logEnded);
            assertArrayEquals(new PatchRecord[]{new PatchRecord(2, "backup3", "from3", "to3"), new PatchRecord(4, "backup5", "from5", "to5"), new PatchRecord(5, "backup6", "from6", "to6")}, failList.toArray(new PatchRecord[failList.size()]));
            assertArrayEquals(new PatchRecord[]{new PatchRecord(6, "backup7", "from7", "to7"), new PatchRecord(3, "backup4", "from4", "to4"), new PatchRecord(1, "backup2", "from2", "to2"), new PatchRecord(0, "backup1", "from1", "to1")}, revertList.toArray(new PatchRecord[revertList.size()]));
            assertEquals(7, startFileIndex);
            assertNull(unfinishedReplacement);


            logStarted = reader2.isLogStarted();
            logEnded = reader2.isLogEnded();
            failList = reader2.getFailList();
            revertList = reader2.getRevertList();
            startFileIndex = reader2.getStartFileIndex();
            unfinishedReplacement = reader2.getUnfinishedReplacement();

            assertTrue(logStarted);
            assertFalse(logEnded);
            assertArrayEquals(new PatchRecord[]{new PatchRecord(2, "backup3", "from3", "to3")}, failList.toArray(new PatchRecord[failList.size()]));
            assertArrayEquals(new PatchRecord[]{new PatchRecord(1, "backup2", "from2", "to2"), new PatchRecord(0, "backup1", "from1", "to1")}, revertList.toArray(new PatchRecord[revertList.size()]));
            assertEquals(3, startFileIndex);
            assertEquals(new PatchRecord(3, "backup4", "from4", "to4"), unfinishedReplacement);


            logStarted = reader3.isLogStarted();
            logEnded = reader3.isLogEnded();
            failList = reader3.getFailList();
            revertList = reader3.getRevertList();
            startFileIndex = reader3.getStartFileIndex();
            unfinishedReplacement = reader3.getUnfinishedReplacement();

            assertTrue(logStarted);
            assertFalse(logEnded);
            assertArrayEquals(new PatchRecord[]{new PatchRecord(0, "backup1", "from1", "to1"), new PatchRecord(2, "backup3", "from3", "to3")}, failList.toArray(new PatchRecord[failList.size()]));
            assertArrayEquals(new PatchRecord[]{new PatchRecord(1, "backup2", "from2", "to2")}, revertList.toArray(new PatchRecord[revertList.size()]));
            assertEquals(3, startFileIndex);
            assertNull(unfinishedReplacement);


            logStarted = reader4.isLogStarted();
            logEnded = reader4.isLogEnded();
            failList = reader4.getFailList();
            revertList = reader4.getRevertList();
            startFileIndex = reader4.getStartFileIndex();
            unfinishedReplacement = reader4.getUnfinishedReplacement();

            assertTrue(logStarted);
            assertFalse(logEnded);
            assertArrayEquals(new PatchRecord[]{new PatchRecord(0, "backup1", "from1", "to1"), new PatchRecord(1, "backup2", "from2", "to2")}, failList.toArray(new PatchRecord[failList.size()]));
            assertArrayEquals(new PatchRecord[]{}, revertList.toArray(new PatchRecord[revertList.size()]));
            assertEquals(2, startFileIndex);
            assertNull(unfinishedReplacement);


            logStarted = reader5.isLogStarted();
            logEnded = reader5.isLogEnded();
            failList = reader5.getFailList();
            revertList = reader5.getRevertList();
            startFileIndex = reader5.getStartFileIndex();
            unfinishedReplacement = reader5.getUnfinishedReplacement();

            assertTrue(logStarted);
            assertFalse(logEnded);
            assertArrayEquals(new PatchRecord[]{}, failList.toArray(new PatchRecord[failList.size()]));
            assertArrayEquals(new PatchRecord[]{new PatchRecord(1, "backup2", "from2", "to2"), new PatchRecord(0, "backup1", "from1", "to1")}, revertList.toArray(new PatchRecord[revertList.size()]));
            assertEquals(2, startFileIndex);
            assertNull(unfinishedReplacement);
        } catch (IOException ex) {
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
