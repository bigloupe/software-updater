package updater.downloader.util;

import updater.downloader.util.DownloadProgessUtil;
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
public class DownloadProgessUtilTest {

    public DownloadProgessUtilTest() {
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
     * This test needs about 1 second (Thread.sleep)
     */
    @Test
    public void test() {
        System.out.println("DownloadProgessUtilTest - simple test only, this test is **time sensitive**");

        DownloadProgessUtil instance = new DownloadProgessUtil();

        instance.setTotalSize(1000000);
        instance.setDownloadedSize(1000);
        instance.setAverageTimeSpan(2);

        assertEquals(1000000, instance.getTotalSize());
        assertEquals(1000, instance.getDownloadedSize());
        assertEquals(2, instance.getAverageTimeSpan());
        assertEquals(0, instance.getSpeed());
        assertEquals(0, instance.getTimeRemaining());


        // test simutaneous feed, interval feed
        try {
            instance.feed(1000);
            instance.feed(1000);
            instance.feed(1000);
            Thread.sleep(500);
            instance.feed(1000);
            Thread.sleep(490);
            instance.feed(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(DownloadProgessUtilTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        assertEquals(1000000, instance.getTotalSize());
        assertEquals(6000, instance.getDownloadedSize());
        assertEquals(2, instance.getAverageTimeSpan());

        long speed = instance.getSpeed();
        System.out.println("Speed (5050): " + speed);
        // 0ms, 1ms, 2ms delay
        assertTrue(speed - 5050 < 200);

        long timeRemaining = (int) ((double) (1000000 - 6000) / (double) speed);
        assertEquals(timeRemaining, instance.getTimeRemaining());


        // preparation for the next test, test setAverageTimeSpan
        try {
            Thread.sleep(11);
            instance.feed(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(DownloadProgessUtilTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        assertEquals(1000000, instance.getTotalSize());
        assertEquals(9000, instance.getDownloadedSize());
        assertEquals(2, instance.getAverageTimeSpan());

        speed = instance.getSpeed();
        System.out.println("Speed (7992): " + speed);
        assertTrue(speed - 7992 < 200);

        timeRemaining = (int) ((double) (1000000 - 9000) / (double) speed);
        assertEquals(timeRemaining, instance.getTimeRemaining());


        // test if the first 3 records are removed
        instance.setAverageTimeSpan(1);

        assertEquals(1000000, instance.getTotalSize());
        assertEquals(9000, instance.getDownloadedSize());
        assertEquals(1, instance.getAverageTimeSpan());

        speed = instance.getSpeed();
        System.out.println("Speed (9980): " + speed);
        assertTrue(speed - 9980 < 200);

        timeRemaining = (int) ((double) (1000000 - 9000) / (double) speed);
        assertEquals(timeRemaining, instance.getTimeRemaining());
    }
}
