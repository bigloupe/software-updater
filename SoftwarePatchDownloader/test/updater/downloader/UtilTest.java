package updater.downloader;

import updater.downloader.Util;
import updater.TestCommon;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class UtilTest {

    protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public UtilTest() {
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
     * Test of humanReadableTimeCount method, of class Util.
     */
    @Test
    public void testHumanReadableTimeCount() {
        System.out.println("+++++ testHumanReadableTimeCount +++++");
        // 2 yrs, 3 mths, 4 days, 5h 10m 45s
        int time = (31536000 * 2) + (2592000 * 3) + (86400 * 4) + (3600 * 5) + (60 * 10) + 45;
        assertEquals("2 yrs", Util.humanReadableTimeCount(time, 1));
        assertEquals("2 yrs, 3 mths", Util.humanReadableTimeCount(time, 2));
        assertEquals("2 yrs, 3 mths, 4 days", Util.humanReadableTimeCount(time, 3));
        assertEquals("2 yrs, 3 mths, 4 days, 5h", Util.humanReadableTimeCount(time, 4));
        assertEquals("2 yrs, 3 mths, 4 days, 5h 10m", Util.humanReadableTimeCount(time, 5));
        assertEquals("2 yrs, 3 mths, 4 days, 5h 10m 45s", Util.humanReadableTimeCount(time, 6));
        assertEquals("2 yrs, 3 mths, 4 days, 5h 10m 45s", Util.humanReadableTimeCount(time, 7));
        // 1 yr, 1 mth, 1 day, 1h 1m 59s
        time = (31536000 * 1) + (2592000 * 1) + (86400 * 1) + (3600 * 1) + (60 * 1) + 59;
        assertEquals("1 yr", Util.humanReadableTimeCount(time, 1));
        assertEquals("1 yr, 1 mth", Util.humanReadableTimeCount(time, 2));
        assertEquals("1 yr, 1 mth, 1 day", Util.humanReadableTimeCount(time, 3));
        assertEquals("1 yr, 1 mth, 1 day, 1h", Util.humanReadableTimeCount(time, 4));
        assertEquals("1 yr, 1 mth, 1 day, 1h 1m", Util.humanReadableTimeCount(time, 5));
        assertEquals("1 yr, 1 mth, 1 day, 1h 1m 59s", Util.humanReadableTimeCount(time, 6));
        assertEquals("1 yr, 1 mth, 1 day, 1h 1m 59s", Util.humanReadableTimeCount(time, 7));
        // 1 yr, 0 mth, 0 day, 0h 0m 1s
        time = (31536000 * 1) + (2592000 * 0) + (86400 * 0) + (3600 * 0) + (60 * 0) + 1;
        assertEquals("1 yr", Util.humanReadableTimeCount(time, 1));
        assertEquals("1 yr, 0 mth", Util.humanReadableTimeCount(time, 2));
        assertEquals("1 yr, 0 mth, 0 day", Util.humanReadableTimeCount(time, 3));
        assertEquals("1 yr, 0 mth, 0 day, 0h", Util.humanReadableTimeCount(time, 4));
        assertEquals("1 yr, 0 mth, 0 day, 0h 0m", Util.humanReadableTimeCount(time, 5));
        assertEquals("1 yr, 0 mth, 0 day, 0h 0m 1s", Util.humanReadableTimeCount(time, 6));
        assertEquals("1 yr, 0 mth, 0 day, 0h 0m 1s", Util.humanReadableTimeCount(time, 7));
        // 1 mth, 0 day, 0h 0m 1s
        time = (2592000 * 1) + (86400 * 0) + (3600 * 0) + (60 * 0) + 1;
        assertEquals("1 mth", Util.humanReadableTimeCount(time, 1));
        assertEquals("1 mth, 0 day", Util.humanReadableTimeCount(time, 2));
        assertEquals("1 mth, 0 day, 0h", Util.humanReadableTimeCount(time, 3));
        assertEquals("1 mth, 0 day, 0h 0m", Util.humanReadableTimeCount(time, 4));
        assertEquals("1 mth, 0 day, 0h 0m 1s", Util.humanReadableTimeCount(time, 5));
        assertEquals("1 mth, 0 day, 0h 0m 1s", Util.humanReadableTimeCount(time, 6));
        // 3 days, 3h 0m 1s
        time = (86400 * 3) + (3600 * 3) + (60 * 0) + 1;
        assertEquals("3 days", Util.humanReadableTimeCount(time, 1));
        assertEquals("3 days, 3h", Util.humanReadableTimeCount(time, 2));
        assertEquals("3 days, 3h 0m", Util.humanReadableTimeCount(time, 3));
        assertEquals("3 days, 3h 0m 1s", Util.humanReadableTimeCount(time, 4));
        assertEquals("3 days, 3h 0m 1s", Util.humanReadableTimeCount(time, 5));
        // 3h 3m 1s
        time = (3600 * 3) + (60 * 3) + 1;
        assertEquals("3h", Util.humanReadableTimeCount(time, 1));
        assertEquals("3h 3m", Util.humanReadableTimeCount(time, 2));
        assertEquals("3h 3m 1s", Util.humanReadableTimeCount(time, 3));
        assertEquals("3h 3m 1s", Util.humanReadableTimeCount(time, 4));
        // 59s
        time = (60 * 0) + 59;
        assertEquals("59s", Util.humanReadableTimeCount(time, 1));
        assertEquals("59s", Util.humanReadableTimeCount(time, 2));
    }
}
