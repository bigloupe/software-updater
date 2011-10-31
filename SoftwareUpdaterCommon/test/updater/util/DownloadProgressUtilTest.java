package updater.util;

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
public class DownloadProgressUtilTest {

    protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public DownloadProgressUtilTest() {
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
     * This test needs about 1 second (Thread.sleep)
     */
    @Test
    public void test() throws InterruptedException {
        // simple test only
        System.out.println("+++++ test +++++");

        DownloadProgressUtil instance = new DownloadProgressUtil();

        instance.setTotalSize(1000000);
        instance.setDownloadedSize(1000);
        instance.setAverageTimeSpan(2000);

        assertEquals(1000000, instance.getTotalSize());
        assertEquals(1000, instance.getDownloadedSize());
        assertEquals(2000, instance.getAverageTimeSpan());
        assertEquals(0, instance.getSpeed());
        assertEquals(0, instance.getTimeRemaining());


        // test simutaneous feed, interval feed
        instance.feed(1000);
        instance.feed(1000);
        instance.feed(1000);
        Thread.sleep(500);
        instance.feed(1000);
        Thread.sleep(490);
        instance.feed(1000);

        assertEquals(1000000, instance.getTotalSize());
        assertEquals(6000, instance.getDownloadedSize());
        assertEquals(2000, instance.getAverageTimeSpan());

        long speed = instance.getSpeed();
        System.out.println("- Speed (5050): " + speed);
        assertEquals((double) 5050, (double) speed, 200F);

        long timeRemaining = (int) ((double) (1000000 - 6000) / (double) speed);
        assertEquals(timeRemaining, instance.getTimeRemaining());


        // preparation for the next test, test setAverageTimeSpan
        Thread.sleep(11);
        instance.feed(3000);

        assertEquals(1000000, instance.getTotalSize());
        assertEquals(9000, instance.getDownloadedSize());
        assertEquals(2000, instance.getAverageTimeSpan());

        speed = instance.getSpeed();
        System.out.println("- Speed (7992): " + speed);
        assertEquals((double) 7992, (double) speed, 200F);

        timeRemaining = (int) ((double) (1000000 - 9000) / (double) speed);
        assertEquals(timeRemaining, instance.getTimeRemaining());


        // test if the first 3 records are removed
        instance.setAverageTimeSpan(1000);

        assertEquals(1000000, instance.getTotalSize());
        assertEquals(9000, instance.getDownloadedSize());
        assertEquals(1000, instance.getAverageTimeSpan());

        speed = instance.getSpeed();
        System.out.println("- Speed (9980): " + speed);
        assertEquals((double) 9980, (double) speed, 200F);

        timeRemaining = (int) ((double) (1000000 - 9000) / (double) speed);
        assertEquals(timeRemaining, instance.getTimeRemaining());
    }
}
