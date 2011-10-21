package updater.launcher.patch;

import updater.launcher.patch.PatchLogReader.UnfinishedPatch;
import java.util.List;
import updater.launcher.patch.PatchLogWriter.Action;
import updater.launcher.patch.PatchLogWriter.OperationType;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchLogTest {

    public PatchLogTest() {
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

    @Test
    public void test() {
        System.out.println("PatchLogTest");

        File logFile = new File("PatchLogTest_lamA5.log");
        logFile.delete();

        try {
            PatchLogWriter writer = new PatchLogWriter(logFile);

            writer.logStart(1, "1.0.0", "1.0.1");
            writer.logPatch(Action.START, 0, OperationType.PATCH, "oldFilePath", "newFilePath");
            writer.logEnd();

            writer.logStart(2, "1.0.1", "1.0.2");
            writer.logPatch(Action.START, 0, OperationType.PATCH, "oldFilePath", "newFilePath");
            writer.logPatch(Action.FINISH, 0, OperationType.PATCH, "oldFilePath", "newFilePath");
            writer.logPatch(Action.START, 1, OperationType.NEW, "", "newFilePath");
            writer.logPatch(Action.FINISH, 1, OperationType.NEW, "", "newFilePath");
            writer.logPatch(Action.START, 2, OperationType.REMOVE, "oldFilePath", "");
            writer.logEnd();

            writer.logStart(3, "1.0.3", "1.0.5");
            writer.logPatch(Action.START, 0, OperationType.PATCH, "oldFilePath", "newFilePath");
            writer.logPatch(Action.FINISH, 0, OperationType.PATCH, "oldFilePath", "newFilePath");
            writer.logPatch(Action.START, 1, OperationType.NEW, "", "newFilePath");
            writer.logPatch(Action.FINISH, 1, OperationType.NEW, "", "newFilePath");
            writer.logPatch(Action.START, 2, OperationType.REMOVE, "oldFilePath", "");

            writer.close();

            assertTrue(logFile.exists());
            assertTrue(logFile.length() > 10);
        } catch (IOException ex) {
            fail("prepare log failed");
            Logger.getLogger(PatchLogTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            PatchLogReader reader = new PatchLogReader(logFile);

            List<Integer> finishedPatches = reader.getfinishedPatches();
            assertEquals(2, finishedPatches.size());
            assertTrue(finishedPatches.get(0) == 1);
            assertTrue(finishedPatches.get(1) == 2);

            UnfinishedPatch unfinishedPatch = reader.getUnfinishedPatch();
            assertEquals(3, unfinishedPatch.getPatchId());
            assertEquals(2, unfinishedPatch.getFileIndex());
        } catch (IOException ex) {
            fail("read log failed");
            Logger.getLogger(PatchLogTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        logFile.delete();
    }
}
