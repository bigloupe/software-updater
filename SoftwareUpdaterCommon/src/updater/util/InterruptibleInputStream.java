package updater.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class InterruptibleInputStream extends FilterInputStream {

    protected int sizeAvailable;
    protected final List<Runnable> interruptedTasks;

    public InterruptibleInputStream(InputStream in) {
        this(in, -1);
    }

    public InterruptibleInputStream(InputStream in, int sizeAvailable) {
        super(in);
        this.sizeAvailable = sizeAvailable;
        interruptedTasks = Collections.synchronizedList(new ArrayList<Runnable>());
    }

    public void addInterruptedTask(Runnable task) {
        interruptedTasks.add(task);
    }

    public void removeInterruptedTask(Runnable task) {
        interruptedTasks.remove(task);
    }

    public int remaining() {
        return sizeAvailable;
    }

    @Override
    public int read() throws IOException {
        checkInterrupted();

        if (sizeAvailable <= 0) {
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
        checkInterrupted();

        if (sizeAvailable <= 0) {
            return -1;
        }

        if (sizeAvailable != -1 && len > sizeAvailable) {
            len = sizeAvailable;
        }

        int result = in.read(b, off, len);
        if (sizeAvailable != -1 && result != -1) {
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
        checkInterrupted();

        if (sizeAvailable != -1 && n > sizeAvailable) {
            n = sizeAvailable;
        }

        long result = in.skip(n);
        if (sizeAvailable != -1 && result != -1) {
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
        checkInterrupted();

        int result = in.available();
        if (sizeAvailable != -1 && result > sizeAvailable) {
            result = sizeAvailable;
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        checkInterrupted();
        in.close();
    }

    @Override
    public void mark(int readlimit) {
        checkInterrupted();
        in.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        checkInterrupted();
        in.reset();
    }

    @Override
    public boolean markSupported() {
        checkInterrupted();
        return in.markSupported();
    }

    protected void checkInterrupted() {
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
