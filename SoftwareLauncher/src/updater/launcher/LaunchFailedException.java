package updater.launcher;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class LaunchFailedException extends Exception {

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
