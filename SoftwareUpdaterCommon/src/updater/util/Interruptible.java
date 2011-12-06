package updater.util;

/**
 * Support executing tasks after thread be interrupted.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public interface Interruptible {

  /**
   * Add task to be execute after interrupted.
   * @param task the task to add
   */
  void addInterruptedTask(Runnable task);

  /**
   * Remove task that will be executed after interrupted.
   * @param task the task to remove
   */
  void removeInterruptedTask(Runnable task);
}
