package updater.concurrent;

/**
 * The lock type.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public enum LockType {

  /**
   * Acquire when need to do patching.
   */
  UPDATER,
  /**
   * Acquire when need to download patches.
   */
  DOWNLOADER,
  /**
   * Acquire when need to open new instance of the software.
   */
  INSTANCE
}