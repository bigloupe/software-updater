package updater.selfupdater;

import java.util.concurrent.atomic.AtomicLong;
import java.nio.channels.FileLock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.channels.FileChannel;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareSelfUpdaterTest {

    static {
        // set test mode
        System.setProperty("SoftwareSelfUpdaterTestMode", "true");
    }
    protected final String packagePath = "test/" + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public SoftwareSelfUpdaterTest() {
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
    public void test() throws IOException {
        System.out.println("+++++ test +++++");

        File testFolder = new File("test folder/");

        if (testFolder.exists()) {
            if (testFolder.isDirectory()) {
                truncateFolder(testFolder);
            } else {
                testFolder.delete();
            }
        }

        try {
            copy(new File("test/updater/selfupdater/SoftwareSelfUpdaterTest"), testFolder);

            FileOutputStream fout = null;
            FileLock lock = null;
            try {
                fout = new FileOutputStream(new File("test folder/global_lock"));
                lock = fout.getChannel().tryLock();
            } finally {
            }

            final File testFile = new File("test folder/testLaunch A9fD6");
            testFile.delete();
            assertFalse(testFile.exists());

            long startTime = System.currentTimeMillis();
            final AtomicBoolean runFinished = new AtomicBoolean(false);
            final AtomicLong runFinishedTime = new AtomicLong(0);
            new Thread(new Runnable() {

                @Override
                public void run() {
                    SoftwareSelfUpdater.main(new String[]{"test folder/", "test folder/replacement list.txt", "java", "-jar", "test folder/LaunchTest.jar", testFile.getAbsolutePath()});
                    runFinished.set(true);
                    runFinishedTime.set(System.currentTimeMillis());
                }
            }).start();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }

            lock.release();
            fout.close();
            long releaseLockTime = System.currentTimeMillis();

            while (!runFinished.get()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                }
            }

            assertTrue(runFinishedTime.get() - startTime > 1000);
            long timeDifference = runFinishedTime.get() - releaseLockTime;
            assertTrue(Long.toString(timeDifference), timeDifference < 1020);

            assertEquals(new String(readFile(new File("test folder/0.txt"))), "1");
            assertEquals(new String(readFile(new File("test folder/2.txt"))), "0");
            assertFalse(new File("test folder/1.txt").exists());
            assertEquals(new String(readFile(new File("test folder/a.txt"))), "b");
            assertEquals(new String(readFile(new File("test folder/c.txt"))), "a");
            assertFalse(new File("test folder/b.txt").exists());
            assertEquals(new String(readFile(new File("test folder/backup1.txt"))), "dest1");
            assertFalse(new File("test folder/dest1.txt").exists());
            assertTrue(new File("test folder/dest2").isDirectory());
            assertEquals(new String(readFile(new File("test folder/dest3.txt"))), "new3");
            assertFalse(new File("test folder/new3.txt").exists());

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }

            assertTrue(testFile.exists());
            testFile.delete();
            assertFalse(testFile.exists());
        } finally {
            assertTrue(truncateFolder(testFolder));
            testFolder.delete();
        }
    }

    /**
     * Copy a file/folder to another location.
     * @param from the file/folder to copy form
     * @param to the file/folder location to copy to
     * @throws IOException error occurred when reading/writing the content from/into the file
     */
    public static void copy(File from, File to) throws IOException {
        if (from.isDirectory()) {
            if (!to.isDirectory()) {
                if (!to.mkdirs()) {
                    throw new IOException("Cannot create folder: " + to.getAbsolutePath());
                }
            }

            File[] files = from.listFiles();
            if (files == null) {
                throw new IOException(String.format("Failed to list the files in folder: %1$s", from.getAbsolutePath()));
            }
            String fromAbsPath = from.getAbsolutePath();
            String toAbsPath = to.getAbsolutePath();
            for (File file : files) {
                File copyTo = new File(toAbsPath + File.separator + file.getAbsolutePath().replace(fromAbsPath, ""));
                if (file.isDirectory()) {
                    copy(file, copyTo);
                } else {
                    copyFile(file, copyTo);
                }
            }
        } else {
            copyFile(from, to);
        }
    }

    /**
     * Read the whole file and return the content in byte array.
     * @param file the file to read
     * @return the content of the file in byte array
     * @throws IOException error occurred when reading the content from the file
     */
    public static byte[] readFile(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("argument 'file' cannot be null");
        }

        long fileLength = file.length();
        byte[] content = new byte[(int) fileLength];

        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);

            int byteRead = 0, cumulateByteRead = 0;
            while ((byteRead = fin.read(content, cumulateByteRead, content.length - cumulateByteRead)) != -1) {
                cumulateByteRead += byteRead;
                if (cumulateByteRead >= fileLength) {
                    break;
                }
            }

            if (cumulateByteRead != fileLength) {
                throw new IOException(String.format("The total number of bytes read does not match the file size. Actual file size: %1$d, bytes read: %2$d, path: %3$s",
                        fileLength, cumulateByteRead, file.getAbsolutePath()));
            }
        } finally {
            SoftwareSelfUpdater.closeQuietly(fin);
        }

        return content;
    }

    /**
     * Copy a file to another location.
     * @param fromFile the file to copy form
     * @param toFile the file/location to copy to
     * @throws IOException error occurred when reading/writing the content from/into the file
     */
    public static void copyFile(File fromFile, File toFile) throws IOException {
        if (fromFile == null) {
            throw new NullPointerException("argument 'fromFile' cannot be null");
        }
        if (toFile == null) {
            throw new NullPointerException("argument 'toFile' cannot be null");
        }

        FileInputStream fromFileStream = null;
        FileOutputStream toFileStream = null;
        try {
            long fromFileLength = fromFile.length();

            fromFileStream = new FileInputStream(fromFile);
            toFileStream = new FileOutputStream(toFile);

            FileChannel fromFileChannel = fromFileStream.getChannel();
            FileChannel toFileChannel = toFileStream.getChannel();

            long byteToRead = 0, cumulateByteRead = 0;
            while (cumulateByteRead < fromFileLength) {
                byteToRead = (fromFileLength - cumulateByteRead) > 32768 ? 32768 : (fromFileLength - cumulateByteRead);
                cumulateByteRead += toFileChannel.transferFrom(fromFileChannel, cumulateByteRead, byteToRead);
            }

            if (cumulateByteRead != fromFileLength) {
                throw new IOException(String.format("The total number of bytes read does not match the file size. Actual file size: %1$d, bytes read: %2$d, path: %3$s",
                        fromFileLength, cumulateByteRead, fromFile.getAbsolutePath()));
            }
        } finally {
            SoftwareSelfUpdater.closeQuietly(fromFileStream);
            SoftwareSelfUpdater.closeQuietly(toFileStream);
        }
    }

    /**
     * Remove all folders and files in the {@code directory}. (The {@code directory} will not be removed)
     * @param directory the directory to truncate
     * @return true if all folders and files has been removed successfully, false if failed to remove any
     */
    public static boolean truncateFolder(File directory) {
        if (directory == null) {
            throw new NullPointerException("argument 'directory' cannot be null");
        }

        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if (!truncateFolderRecursively(file)) {
                    return false;
                }
            } else {
                if (!file.delete()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Remove all folders and files in the {@code directory}. (The {@code directory} will be removed)
     * It is used by {@link #truncateFolder(java.io.File)}.
     * @param directory the directory to truncate
     * @return true if all folders and files has been removed successfully, false if failed to remove any
     */
    protected static boolean truncateFolderRecursively(File directory) {
        if (directory == null) {
            throw new NullPointerException("argument 'directory' cannot be null");
        }

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    if (!truncateFolderRecursively(file)) {
                        return false;
                    }
                } else {
                    if (!file.delete()) {
                        return false;
                    }
                }
            }
        }

        return directory.delete();
    }
}
