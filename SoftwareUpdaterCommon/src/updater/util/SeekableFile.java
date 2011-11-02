package updater.util;

import com.nothome.delta.RandomAccessFileSeekableSource;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SeekableFile extends RandomAccessFileSeekableSource {

    protected final List<Runnable> interruptedTasks;
    protected boolean pause;

    public SeekableFile(RandomAccessFile file) {
        super(file);
        interruptedTasks = Collections.synchronizedList(new ArrayList<Runnable>());
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
