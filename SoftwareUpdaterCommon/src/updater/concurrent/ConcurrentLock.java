package updater.concurrent;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileLock;
import updater.util.CommonUtil;

/**
 * An object that contain the {@link FileOutputStream} and {@link FileLock} 
 * related to one lock.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class ConcurrentLock {

  /**
   * The file input/output stream that the lock belongs to.
   */
  protected Closeable lockFileStream;
  /**
   * The file lock.
   */
  protected FileLock fileLock;

  /**
   * Constructor.
   * 
   * @param lockFileStream the file input/output stream that the lock belongs to
   * @param fileLock the file lock
   */
  public ConcurrentLock(Closeable lockFileStream, FileLock fileLock) {
    if (lockFileStream == null) {
      throw new NullPointerException("argument 'fileOut' cannot be null");
    }
    if (fileLock == null) {
      throw new NullPointerException("argument 'fileLock' cannot be null");
    }

    this.lockFileStream = lockFileStream;
    this.fileLock = fileLock;
  }

  /**
   * Release the lock.
   */
  public synchronized void release() {
    CommonUtil.releaseLockQuietly(fileLock);
    CommonUtil.closeQuietly(lockFileStream);
    fileLock = null;
    lockFileStream = null;
  }
}
