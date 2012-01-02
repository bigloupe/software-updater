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
package updater.util;

import com.nothome.delta.RandomAccessFileSeekableSource;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extend the {@link com.nothome.delta.RandomAccessFileSeekableSource} to 
 * support pause and thread interrupt.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SeekableFile extends RandomAccessFileSeekableSource implements Pausable, Interruptible {

  /**
   * List of tasks to be executed after interrupted.
   */
  protected final List<Runnable> interruptedTasks;
  /**
   * Indicate currently is paused or not.
   */
  protected boolean pause;

  /**
   * Constructor.
   * @param file the file for random seek
   */
  public SeekableFile(RandomAccessFile file) {
    super(file);
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

  @Override
  public void seek(long pos) throws IOException {
    check();
    super.seek(pos);
  }

  @Override
  public int read(ByteBuffer bb) throws IOException {
    check();
    return super.read(bb);
  }

  @Override
  public void close() throws IOException {
    super.close();
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
