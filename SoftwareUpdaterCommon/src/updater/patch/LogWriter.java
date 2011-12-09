package updater.patch;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Patch log writer.
 * 
 * <b>Format: </b><br />
 * [action code] [file index (optional)] [detail operation id] [backup path (optional)] [new file path (optional)] [dest file path (optional)]
 * <ul>
 * <li>action code: 0 - start, 1 - finish, 2 - replacement start, 3 - replacement finish, 4 - replacement failed, 5 - revert</li>
 * </ul>
 * Note that '[' and ']' didn't really exist.
 * 
 * <p>
 * <b>Sample:</b><br />
 * 0<br />
 * 2 0 0 "C:\\update\\old_start.jar" "C:\\update\\start.jar" "C:\\start.jar"<br />
 * 3 0<br />
 * 2 1 0 "C:\\update\\old_updater.jar" "C:\\update\\updater.jar" "C:\\updater.jar"<br />
 * 3 1<br />
 * 2 2 0 "C:\\update\\old_test.jar" "C:\\update\\test.jar" "C:\\test.jar"<br />
 * 3 2<br />
 * 2 3 0 "C:\\update\\old_logo.png" "C:\\update\\logo.png" "C:\\logo.png"<br />
 * 4 3<br />
 * 5 3<br />
 * 2 3 0 "C:\\update\\old_logo.png" "C:\\update\\logo.png" "C:\\logo.png"<br />
 * 4 3<br />
 * 1
 * </p>
 * 
 * <p>Should invoke {@link #logStart()} first, {@link #logPatch(updater.patch.LogAction, int, updater.patch.LogWriter.OperationType, java.lang.String, java.lang.String)} 
 * second, {@link #logEnd()} last.</p>
 * 
 * <p>One log should serve only one apply-patch event.</p>
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class LogWriter implements Closeable {

  /**
   * The output stream of the log file.
   */
  protected OutputStream out;

  /**
   * Constructor.
   * @param file the file to append the log on
   * @throws IOException if the file exists but is a directory rather than a 
   * regular file, does not exist but cannot be created, or cannot be opened 
   * for any other reason
   */
  public LogWriter(File file) throws IOException {
    if (file == null) {
      throw new NullPointerException("argument 'file' cannot be null");
    }

    out = new FileOutputStream(file, true);
  }

  @Override
  public void close() throws IOException {
    out.close();
  }

  /**
   * Record the patching start.
   * @throws IOException error occurred when writing to the log
   */
  public void logStart() throws IOException {
    out.write("0\n".getBytes());
    out.flush();
  }

  /**
   * Log resume patching. This will output a new line character.
   * This is to solve the case if last log record is not ended with new line 
   * character due to IO error.
   * @throws IOException error occurred when writing to log
   */
  public void logResume() throws IOException {
    out.write("\n".getBytes());
    out.flush();
  }

  /**
   * Record the patching finished.
   * @throws IOException error occurred when writing to the log
   */
  public void logEnd() throws IOException {
    out.write("1\n".getBytes());
    out.flush();
  }

  /**
   * Record file replacement action.
   * @param action the action
   * @param fileIndex the current file index in the patch
   * @throws IOException error occurred when writing to log
   */
  public void logPatch(LogAction action, int fileIndex) throws IOException {
    logPatch(action, fileIndex, 0, false, "", "", "");
  }

  /**
   * Record file replacement action.
   * @param action the action
   * @param fileIndex the current file index in the patch
   * @param operationId the detail operation id of the {@code operationType}
   * @param destinationFileExist true means destination file exist when 
   * patching, false if not
   * @param backupFilePath the back up file path
   * @param newFilePath the move-from path
   * @param destinationFilePath the move-to path
   * @throws IOException error occurred when writing to log
   */
  public void logPatch(LogAction action, int fileIndex, int operationId,
          boolean destinationFileExist,
          String backupFilePath, String newFilePath, String destinationFilePath) throws IOException {
    if (action == null) {
      throw new NullPointerException("argument 'action' cannot be null");
    }
    if (backupFilePath == null) {
      throw new NullPointerException("argument 'backupFilePath' cannot be null");
    }
    if (newFilePath == null) {
      throw new NullPointerException("argument 'newFilePath' cannot be null");
    }
    if (destinationFilePath == null) {
      throw new NullPointerException("argument 'destinationFilePath' cannot be null");
    }

    StringBuilder sb = new StringBuilder(64);

    switch (action) {
      case START:
        sb.append('2');
        break;
      case FINISH:
        sb.append('3');
        break;
      case FAILED:
        sb.append('4');
        break;
    }
    sb.append(' ');
    sb.append(fileIndex);
    if (action == LogAction.START) {
      sb.append(' ');
      sb.append(operationId);
      sb.append(' ');
      sb.append(destinationFileExist ? 1 : 0);
      sb.append(" \"");
      sb.append(backupFilePath.replace("\"", "\\\""));
      sb.append("\" \"");
      sb.append(newFilePath.replace("\"", "\\\""));
      sb.append("\" \"");
      sb.append(destinationFilePath.replace("\"", "\\\""));
      sb.append("\"");
    }

    sb.append("\n");

    out.write(sb.toString().getBytes("UTF-8"));
    out.flush();
  }

  /**
   * Log the revert action.
   * @param fileIndex the file index of the reversion
   * @throws IOException error occurred when writing to log
   */
  public void logRevert(int fileIndex) throws IOException {
    StringBuilder sb = new StringBuilder();

    sb.append("5 ");
    sb.append(fileIndex);
    sb.append("\n");

    out.write(sb.toString().getBytes("UTF-8"));
    out.flush();
  }
}
