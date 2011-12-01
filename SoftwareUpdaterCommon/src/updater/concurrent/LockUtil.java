package updater.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.logging.Level;
import java.util.logging.Logger;
import updater.util.CommonUtil;

/**
 * Utilities for acquiring file lock. 
 * This class contain function that can help to acquire suitable lock for launcher and downloader.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class LockUtil {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }

    protected LockUtil() {
    }

    /**
     * Acquire a exclusive lock on the file with retry.
     * 
     * @param fileToLock the file to lock
     * @param timeout the retry timeout (in milli seconds)
     * @param retryDelay the time to sleep between retries, should >= 0 (in milli seconds)
     * 
     * @return the lock if acquired successfully, null if failed
     * 
     * @throws IllegalArgumentException {@code retryDelay} < 0
     */
    public static ConcurrentLock acquireLock(File fileToLock, int timeout, int retryDelay) {
        if (fileToLock == null) {
            throw new NullPointerException("argument 'fileToLock' cannot be null");
        }
        if (retryDelay < 0) {
            throw new IllegalArgumentException(String.format("argument 'retryDelay' must >= 0, found: %1$d", retryDelay));
        }

        ConcurrentLock returnLock = null;
        long acquireLockStart = System.currentTimeMillis();

        FileOutputStream lockFileOut = null;
        FileLock fileLock = null;
        while (true) {
            try {
                lockFileOut = new FileOutputStream(fileToLock);
                try {
                    fileLock = lockFileOut.getChannel().tryLock();
                    if (fileLock == null) {
                        throw new IOException("retry");
                    }
                } catch (OverlappingFileLockException ex) {
                    throw new IOException(ex);
                }

                returnLock = new ConcurrentLock(lockFileOut, fileLock);
                break;
            } catch (IOException ex) {
                if (debug) {
                    Logger.getLogger(LockUtil.class.getName()).log(Level.INFO, null, ex);
                }

                // exceed timeout
                if (System.currentTimeMillis() - acquireLockStart >= timeout) {
                    break;
                }
                // retry delay
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ex1) {
                    Thread.currentThread().interrupt();
                    break;
                }

                continue;
            } finally {
                if (returnLock == null) {
                    CommonUtil.closeQuietly(lockFileOut);
                    CommonUtil.releaseLockQuietly(fileLock);
                }
            }
        }

        return returnLock;
    }

    /**
     * Acquire a lock according to the {@code lockType}.
     * 
     * @param lockType the type of the lock to acquire
     * @param lockFolder the folder to place the lock file
     * @param timeout the retry timeout (in milli seconds)
     * @param retryDelay the time to sleep between retries, should >= 0 (in milli seconds)
     * 
     * @return the lock if acquired successfully, null if failed
     * 
     * @throws IllegalArgumentException {@code lockFolder} is not a valid directory; {@code retryDelay} < 0
     */
    public static ConcurrentLock acquireLock(LockType lockType, File lockFolder, int timeout, int retryDelay) {
        if (lockType == null) {
            throw new NullPointerException("argument 'lockType' cannot be null");
        }
        if (lockFolder == null) {
            throw new NullPointerException("argument 'lockFolder' cannot be null");
        }
        if (!lockFolder.isDirectory()) {
            throw new IllegalArgumentException("'lockFolder' is not a directory");
        }
        if (retryDelay < 0) {
            throw new IllegalArgumentException(String.format("argument 'retryDelay' must >= 0, found: %1$d", retryDelay));
        }

        ConcurrentLock returnLock = null;
        long acquireLockStart = System.currentTimeMillis();

        boolean releaseGlobalLock = true; // release the global lock before return or not
        ConcurrentLock globalLock = null;
        try {
            // need to acquire a global lock before acquiring updater lock
            globalLock = acquireLock(new File(lockFolder.getAbsolutePath() + File.separator + "global_lock"), (int) (timeout - (System.currentTimeMillis() - acquireLockStart)), retryDelay);
            if (globalLock == null) {
                return null;
            }

            switch (lockType) {
                case INSTANCE:
                    String lockFileName = lockFolder.getAbsolutePath() + File.separator + "instance_lock_" + acquireLockStart;

                    int retryCount = 0;
                    File lockFile = new File(lockFileName + "_" + retryCount);
                    while ((returnLock = acquireLock(lockFile, 0, 0)) == null) {
                        if (System.currentTimeMillis() - acquireLockStart >= timeout) {
                            break;
                        }
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ex1) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        retryCount++;
                        lockFile = new File(lockFileName + "_" + retryCount);
                    }
                    lockFile.deleteOnExit();
                    break;
                case DOWNLOADER:
                    returnLock = acquireLock(new File(lockFolder.getAbsolutePath() + File.separator + "updater_lock"), 0, 0);
                    break;
                case UPDATER:
                    ConcurrentLock updaterLock = acquireLock(new File(lockFolder.getAbsolutePath() + File.separator + "updater_lock"), 0, 0);
                    if (updaterLock == null) {
                        return null;
                    }
                    updaterLock.release();

                    File[] files = lockFolder.listFiles();
                    if (files == null) {
                        return null;
                    }
                    for (File file : files) {
                        if (!file.getName().matches("instance_lock_[0-9]{13}_[0-9]+")) {
                            continue;
                        }
                        if (!file.delete()) {
                            return null;
                        }
                    }

                    releaseGlobalLock = false;
                    returnLock = globalLock;
                    break;
            }
        } finally {
            if (releaseGlobalLock) {
                if (globalLock != null) {
                    globalLock.release();
                }
            }
        }

        return returnLock;
    }

    /**
     * The lock type.
     */
    public static enum LockType {

        /**
         * Acquire when need to do patching.
         */
        UPDATER,
        /**
         * Acquire when need to download patches.
         */
        DOWNLOADER,
        /**
         * Acquire when need to open new instance of the software.
         */
        INSTANCE
    }
}
