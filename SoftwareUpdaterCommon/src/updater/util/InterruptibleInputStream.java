package updater.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class InterruptibleInputStream extends FilterInputStream {

    protected int sizeAvailable;
    protected final List<Runnable> interruptedTasks;
    protected boolean pause;

    public InterruptibleInputStream(InputStream in) {
        this(in, -1);
    }

    public InterruptibleInputStream(InputStream in, int sizeAvailable) {
        super(in);
        this.sizeAvailable = sizeAvailable;
        interruptedTasks = Collections.synchronizedList(new ArrayList<Runnable>());
        pause = false;
    }

    public void addInterruptedTask(Runnable task) {
        interruptedTasks.add(task);
    }

    public void removeInterruptedTask(Runnable task) {
        interruptedTasks.remove(task);
    }

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
        in.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        check();
        in.reset();
    }

    @Override
    public boolean markSupported() {
        check();
        return in.markSupported();
    }

    protected void check() {
        synchronized (this) {
            if (pause) {
                try {
                    wait();
                } catch (InterruptedException ex) {
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
