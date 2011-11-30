package updater.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import updater.util.CommonUtil;

/**
 * Utilities for acquiring file lock;
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class LockUtil {

    protected LockUtil() {
    }

    /**
     * Acquire a exclusive lock on the file with retry.
     * @param fileToLock the file to lock
     * @param timeout the retry timeout
     * @param retryDelay the time to sleep between retries
     * @return the lock if acquired successfully, null if failed
     */
    public static ConcurrentLock acquireLock(File fileToLock, int timeout, int retryDelay) {
        ConcurrentLock returnLock = null;
        long acquireLockStart = System.currentTimeMillis();

        FileOutputStream lockFileOut = null;
        FileLock fileLock = null;
        while (true) {
            try {
                lockFileOut = new FileOutputStream(fileToLock);
                fileLock = lockFileOut.getChannel().tryLock();
                if (fileLock == null) {
                    throw new IOException("retry");
                }
                returnLock = new ConcurrentLock(lockFileOut, fileLock);
                break;
            } catch (Exception ex) {
                if (acquireLockStart - System.currentTimeMillis() >= timeout) {
                    break;
                }
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ex1) {
                    break;
                }
                continue;
            }
        }

        return returnLock;
    }

    /**
     * Acquire a lock according to the {@code lockType}.
     * @param lockType the type of the lock to acquire
     * @param lockFolder the folder to place the locked file
     * @param timeout the retry timeout
     * @param retryDelay the time to sleep between retries
     * @return the lock if acquired successfully, null if failed
     */
    public static ConcurrentLock acquireLock(LockType lockType, File lockFolder, int timeout, int retryDelay) {
        if (lockFolder == null) {
            throw new NullPointerException("argument 'lockFolder' cannot be null");
        }
        if (lockType == null) {
            throw new NullPointerException("argument 'lockType' cannot be null");
        }
        if (!lockFolder.isDirectory()) {
            throw new IllegalArgumentException("'lockFolder' is not a directory");
        }

        ConcurrentLock returnLock = null;
        long acquireLockStart = System.currentTimeMillis();

        boolean releaseGlobalLock = true;
        ConcurrentLock globalLock = null;
        ConcurrentLock updaterLock = null;
        try {
            globalLock = acquireLock(new File(lockFolder.getAbsolutePath() + File.separator + "global_lock"), (int) (timeout - (System.currentTimeMillis() - acquireLockStart)), retryDelay);

            switch (lockType) {
                case INSTANCE:
                    String lockFileName = lockFolder.getAbsolutePath() + File.separator + "instance_lock_" + acquireLockStart;

                    int retryCount = 0;
                    File lockFile = new File(lockFileName + "_" + retryCount);
                    while ((returnLock = acquireLock(lockFile, 0, 0)) != null) {
                        if (acquireLockStart - System.currentTimeMillis() >= timeout) {
                            break;
                        }
                        retryCount++;
                        lockFile = new File(lockFileName + "_" + retryCount);
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ex1) {
                            break;
                        }
                    }
                    break;
                case DOWNLOADER:
                    returnLock = acquireLock(new File(lockFolder.getAbsolutePath() + File.separator + "updater_lock"), 0, 0);
                    break;
                case UPDATER:
                    updaterLock = acquireLock(new File(lockFolder.getAbsolutePath() + File.separator + "updater_lock"), 0, 0);
                    if (updaterLock == null) {
                        return null;
                    }

                    File[] files = lockFolder.listFiles();
                    for (File file : files) {
                        if (!file.getName().matches("instance_lock_[0-9]{13}_[0-9]+")) {
                            continue;
                        }
                        if (CommonUtil.tryLock(file)) {
                            file.delete();
                        } else {
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
            if (lockType == LockType.UPDATER) {
                if (updaterLock != null) {
                    updaterLock.release();
                }
            }
        }

        return returnLock;
    }

    /**
     * The lock type.
     */
    public static enum LockType {

        UPDATER, DOWNLOADER, INSTANCE
    }
}
