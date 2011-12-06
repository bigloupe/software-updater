package updater.patch;

/**
 * The listener to listen to patching event and information.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public interface PatcherListener {

  /**
   * Notify the patching progress.
   * @param percentage the percentage from 0 to 100
   * @param message the message that describe the current doing
   */
  void patchProgress(int percentage, String message);

  /**
   * Tell whether the patching is currently allow cancel.
   * @param enable true means enable, false if not
   */
  void patchEnableCancel(boolean enable);
}
