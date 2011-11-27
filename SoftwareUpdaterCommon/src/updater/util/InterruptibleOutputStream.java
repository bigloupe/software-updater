package updater.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Interruptible output stream.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class InterruptibleOutputStream extends FilterOutputStream implements Pausable, Interruptible {

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
     * @param out the output stream to output to
     */
    public InterruptibleOutputStream(OutputStream out) {
        super(out);

        if (out == null) {
            throw new NullPointerException("argument 'out' cannot be null");
        }

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
    public void write(int b) throws IOException {
        check();
        out.write(b);
    }

    @Override
    public void write(byte b[]) throws IOException {
        check();
        out.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        check();
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        check();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        check();
        out.close();
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
