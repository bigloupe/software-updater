// Copyright (c) 2012 Chan Wai Shing
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
