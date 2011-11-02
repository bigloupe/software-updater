package updater.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class InterruptibleOutputStream extends FilterOutputStream {

    protected final List<Runnable> interruptedTasks;
    protected boolean pause;

    public InterruptibleOutputStream(OutputStream out) {
        super(out);
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
