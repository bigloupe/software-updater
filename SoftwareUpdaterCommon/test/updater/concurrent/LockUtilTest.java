package updater.concurrent;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import updater.TestCommon;
import updater.util.CommonUtil;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class LockUtilTest {

    protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public LockUtilTest() {
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
    public void testAcquireLock_3args() throws InterruptedException {
        System.out.println("+++++ testAcquireLock_3args +++++");

        final File lockFile = new File("testAcquireLock_3args");
        lockFile.deleteOnExit();

        System.out.println("+ case 1");

        ConcurrentLock lock = LockUtil.acquireLock(lockFile, 0, 0);
        assertNotNull(lock);

        ConcurrentLock lock2 = LockUtil.acquireLock(lockFile, 0, 0);
        assertNull(lock2);

        lock.release();

        lock2 = LockUtil.acquireLock(lockFile, 0, 0);
        assertNotNull(lock2);
        lock2.release();


        System.out.println("+ case 2 - test retry");

        lock = LockUtil.acquireLock(lockFile, 0, 0);
        assertNotNull(lock);
        final AtomicLong start = new AtomicLong(0), end = new AtomicLong(0);

        final AtomicReference<ConcurrentLock> lock2ref = new AtomicReference<ConcurrentLock>();
        new Thread(new Runnable() {

            @Override
            public void run() {
                start.set(System.currentTimeMillis());
                lock2ref.set(LockUtil.acquireLock(lockFile, 500, 50));
                end.set(System.currentTimeMillis());
            }
        }).start();
        Thread.sleep(250);
        lock.release();
        Thread.sleep(250);
        assertNotNull(lock2ref.get());
        lock2ref.get().release();
        assertEquals(250F, (double) (end.get() - start.get()), 100F);


        System.out.println("+ case 3 - test retry timeout");

        lock = LockUtil.acquireLock(lockFile, 0, 0);
        assertNotNull(lock);
        start.set(0);
        end.set(0);

        lock2ref.set(null);
        final AtomicBoolean threadFinished = new AtomicBoolean(false);
        new Thread(new Runnable() {

            @Override
            public void run() {
                start.set(System.currentTimeMillis());
                lock2ref.set(LockUtil.acquireLock(lockFile, 200, 50));
                end.set(System.currentTimeMillis());
                threadFinished.set(true);
            }
        }).start();
        Thread.sleep(400);
        lock.release();
        assertTrue(threadFinished.get());
        assertNull(lock2ref.get());
        assertEquals(200F, (double) (end.get() - start.get()), 100F);
    }

    @Test
    public void testAcquireLock_4args() {
        System.out.println("+++++ testAcquireLock_4args +++++");

        File lockFolder = new File("testAcquireLock_4args/");
        lockFolder.mkdirs();
        assertTrue(lockFolder.isDirectory());
        assertTrue(CommonUtil.truncateFolder(lockFolder));

        ConcurrentLock instanceLock1, instanceLock2, instanceLock3, updaterLock1, downloaderLock1, downloaderLock2;

        instanceLock1 = LockUtil.acquireLock(LockUtil.LockType.INSTANCE, lockFolder, 0, 0);
        assertNotNull(instanceLock1);
        instanceLock2 = LockUtil.acquireLock(LockUtil.LockType.INSTANCE, lockFolder, 0, 0);
        assertNotNull(instanceLock2);

        downloaderLock1 = LockUtil.acquireLock(LockUtil.LockType.DOWNLOADER, lockFolder, 0, 0);
        assertNotNull(downloaderLock1);

        instanceLock3 = LockUtil.acquireLock(LockUtil.LockType.INSTANCE, lockFolder, 0, 0);
        assertNotNull(instanceLock3);

        updaterLock1 = LockUtil.acquireLock(LockUtil.LockType.UPDATER, lockFolder, 0, 0);
        assertNull(updaterLock1);

        downloaderLock1.release();

        updaterLock1 = LockUtil.acquireLock(LockUtil.LockType.UPDATER, lockFolder, 0, 0);
        assertNull(updaterLock1);

        downloaderLock1 = LockUtil.acquireLock(LockUtil.LockType.DOWNLOADER, lockFolder, 0, 0);
        assertNotNull(downloaderLock1);
        downloaderLock1.release();

        instanceLock1.release();
        instanceLock2.release();
        instanceLock3.release();

        downloaderLock1 = LockUtil.acquireLock(LockUtil.LockType.DOWNLOADER, lockFolder, 0, 0);
        assertNotNull(downloaderLock1);
        downloaderLock2 = LockUtil.acquireLock(LockUtil.LockType.DOWNLOADER, lockFolder, 0, 0);
        assertNull(downloaderLock2);
        downloaderLock1.release();

        updaterLock1 = LockUtil.acquireLock(LockUtil.LockType.UPDATER, lockFolder, 0, 0);
        assertNotNull(updaterLock1);

        instanceLock1 = LockUtil.acquireLock(LockUtil.LockType.INSTANCE, lockFolder, 0, 0);
        assertNull(instanceLock1);
        downloaderLock1 = LockUtil.acquireLock(LockUtil.LockType.DOWNLOADER, lockFolder, 0, 0);
        assertNull(downloaderLock1);

        updaterLock1.release();

        assertTrue(CommonUtil.truncateFolder(lockFolder));
        assertTrue(lockFolder.delete());
    }
}
