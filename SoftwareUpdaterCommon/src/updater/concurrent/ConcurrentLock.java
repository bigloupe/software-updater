package updater.concurrent;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileLock;
import updater.util.CommonUtil;

/**
 * An object that contain the FileOutputStream and FileLock.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class ConcurrentLock {

    /**
     * The file output stream that the lock belongs to.
     */
    protected OutputStream fileOut;
    /**
     * The file lock.
     */
    protected FileLock fileLock;

    /**
     * Constructor.
     * @param fileOut the file output stream that the lock belongs to
     * @param fileLock the file lock
     */
    public ConcurrentLock(FileOutputStream fileOut, FileLock fileLock) {
        this.fileOut = fileOut;
        this.fileLock = fileLock;
    }

    /**
     * Release the lock.
     */
    public void release() {
        CommonUtil.releaseLockQuietly(fileLock);
        CommonUtil.closeQuietly(fileOut);
    }
}
