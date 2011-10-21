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

    public InterruptibleOutputStream(OutputStream out) {
        super(out);
        interruptedTasks = Collections.synchronizedList(new ArrayList<Runnable>());
    }

    public void addInterruptedTask(Runnable task) {
        interruptedTasks.add(task);
    }

    public void removeInterruptedTask(Runnable task) {
        interruptedTasks.remove(task);
    }

    @Override
    public void write(int b) throws IOException {
        checkInterrupted();
        out.write(b);
    }

    @Override
    public void write(byte b[]) throws IOException {
        checkInterrupted();
        out.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        checkInterrupted();
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        checkInterrupted();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        checkInterrupted();
        out.close();
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
