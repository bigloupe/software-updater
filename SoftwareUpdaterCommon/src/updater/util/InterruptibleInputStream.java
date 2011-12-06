package updater.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Interruptible input stream.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class InterruptibleInputStream extends FilterInputStream implements Pausable, Interruptible {

  /**
   * List of tasks to be executed after interrupted.
   */
  protected final List<Runnable> interruptedTasks;
  /**
   * Indicate currently is paused or not.
   */
  protected boolean pause;
  /**
   * Current remaining size available for read, -1 means remaining size is not 
   * limited.
   */
  protected int sizeAvailable;

  /**
   * Constructor.
   * @param in the input stream to read on
   */
  public InterruptibleInputStream(InputStream in) {
    this(in, -1);
  }

  /**
   * Constructor.
   * @param in the input stream to read on
   * @param sizeAvailable current remaining size available for read, -1 means 
   * remaining size is not limited
   */
  public InterruptibleInputStream(InputStream in, int sizeAvailable) {
    super(in);

    if (in == null) {
      throw new NullPointerException("argument 'in' cannot be null");
    }
    if (sizeAvailable < 0 && sizeAvailable != -1) {
      throw new IllegalArgumentException("argument 'sizeAvailable' should >= 0 or be -1");
    }

    this.sizeAvailable = sizeAvailable;
    interruptedTasks = Collections.synchronizedList(new ArrayList<Runnable>());
    pause = false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addInterruptedTask(Runnable task) {
    if (task == null) {
      return;
    }
    interruptedTasks.add(task);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeInterruptedTask(Runnable task) {
    if (task == null) {
      return;
    }
    interruptedTasks.remove(task);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void pause(boolean pause) {
    synchronized (this) {
      this.pause = pause;
      if (!pause) {
        notifyAll();
      }
    }
  }

  public int remaining() {
    return sizeAvailable;
  }

  @Override
  public int read() throws IOException {
    check();

    if (sizeAvailable <= 0 && sizeAvailable != -1) {
      return -1;
    }

    int result = in.read();
    if (sizeAvailable != -1 && result != -1) {
      sizeAvailable--;
    }
    return result;
  }

  @Override
  public int read(byte b[]) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte b[], int off, int len) throws IOException {
    check();

    if (sizeAvailable <= 0 && sizeAvailable != -1) {
      return -1;
    }

    int lengthToRead = sizeAvailable != -1 && len > sizeAvailable ? sizeAvailable : len;
    int result = in.read(b, off, lengthToRead);
    if (sizeAvailable != -1 && result != -1) {
//            sizeAvailable = Math.max(0, sizeAvailable - result);
      if (result > sizeAvailable) {
        // error
        sizeAvailable = 0;
      } else {
        sizeAvailable -= result;
      }
    }
    return result;
  }

  @Override
  public long skip(long n) throws IOException {
    check();

    long byteToSkip = sizeAvailable != -1 && n > sizeAvailable ? sizeAvailable : n;
    long result = in.skip(byteToSkip);
    if (sizeAvailable != -1 && result != -1) {
//            sizeAvailable = Math.max(0, sizeAvailable - result);
      if (result > sizeAvailable) {
        // error
        sizeAvailable = 0;
      } else {
        sizeAvailable -= result;
      }
    }
    return result;
  }

  @Override
  public int available() throws IOException {
    check();

    int result = in.available();
    if (sizeAvailable != -1 && result > sizeAvailable) {
      result = sizeAvailable;
    }
    return result;
  }

  @Override
  public void close() throws IOException {
    check();
    in.close();
  }

  @Override
  public void mark(int readlimit) {
    check();
  }

  @Override
  public void reset() throws IOException {
    check();
    throw new IOException("mark/reset not supported");
  }

  @Override
  public boolean markSupported() {
    check();
    return false;
  }

  /**
   * Check if paused or interrupted.
   */
  protected void check() {
    synchronized (this) {
      if (pause) {
        try {
          wait();
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }
    if (Thread.interrupted()) {
      synchronized (interruptedTasks) {
        for (Runnable task : interruptedTasks) {
          task.run();
        }
      }
      throw new RuntimeException(new InterruptedException());
    }
  }
}
