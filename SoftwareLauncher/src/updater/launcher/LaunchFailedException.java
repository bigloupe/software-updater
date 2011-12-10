package updater.launcher;

/**
 * An exception indicating failed to launch the software.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class LaunchFailedException extends Exception {

  private static final long serialVersionUID = 1L;

  public LaunchFailedException() {
    super();
  }

  public LaunchFailedException(String message) {
    super(message);
  }

  public LaunchFailedException(String message, Throwable cause) {
    super(message, cause);
  }

  public LaunchFailedException(Throwable cause) {
    super(cause);
  }
}
