package updater.util;

/**
 * Support executing tasks after thread be interrupted.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public interface Interruptible {

    /**
     * Add task to be execute after interrupted.
     */
    void addInterruptedTask(Runnable task);

    /**
     * Remove task that will be executed after interrupted.
     */
    void removeInterruptedTask(Runnable task);
}
