package updater.selfupdater;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileLock;

/**
 * An object that contain the {@link FileOutputStream} and {@link FileLock} related to one lock.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class ConcurrentLock {

    /**
     * The file output stream that the lock belongs to.
     */
    protected OutputStream lockFileOut;
    /**
     * The file lock.
     */
    protected FileLock fileLock;

    /**
     * Constructor.
     * 
     * @param fileOut the file output stream that the lock belongs to
     * @param fileLock the file lock
     */
    public ConcurrentLock(FileOutputStream fileOut, FileLock fileLock) {
        if (fileOut == null) {
            throw new NullPointerException("argument 'fileOut' cannot be null");
        }
        if (fileLock == null) {
            throw new NullPointerException("argument 'fileLock' cannot be null");
        }

        this.lockFileOut = fileOut;
        this.fileLock = fileLock;
    }

    /**
     * Release the lock.
     */
    public synchronized void release() {
        try {
            if (fileLock != null) {
                fileLock.release();
                fileLock = null;
            }
            if (lockFileOut != null) {
                lockFileOut.close();
                lockFileOut = null;
            }
        } catch (IOException ex) {
        }
    }
}
