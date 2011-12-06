package updater.script;

/**
 * Thrown when the format of the XML file is invalid.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class InvalidFormatException extends Exception {

  private static final long serialVersionUID = 1L;

  public InvalidFormatException() {
    super();
  }

  public InvalidFormatException(String message) {
    super(message);
  }

  public InvalidFormatException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidFormatException(Throwable cause) {
    super(cause);
  }
}
