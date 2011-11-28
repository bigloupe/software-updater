package updater.patch;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import updater.util.CommonUtil;

/**
 * Patch log writer.
 * 
 * <b>Format: </b><br />
 * ([patch id] [action code] [file index (optional)] [hex encoded 'from' path (optional)])    [from version]->[to version], [action (string)], fileIndex: [file Index], action: [operation type (string)], ['from' path] -> ['to' path]
 * <ul>
 * <li>action code: 0 - start, 1 - finish, 2 - replacement start, 3 - replacement finish, 4 - replacement failed</li>
 * </ul>
 * Note that '[' and ']' didn't really exist.
 * 
 * <p>
 * <b>Sample:</b><br />
 * (1 0)   1.0.0->1.0.1, start<br />
 * (1 2 0 433a5c75706 46174655c737461 72742e6a6172)   1.0.0->1.0.1, replacement start, fileIndex: 0, action: patch, back up: C:\\update\\old_start.jar, C:\\update\\start.jar->C:\\start.jar<br />
 * (1 3 0)   1.0.0->1.0.1, replacement end, fileIndex: 0<br />
 * (1 2 1 433a5c75706 46174655c757064617 465722e6a6172)   1.0.0->1.0.1, replacement start, fileIndex: 1, action: new, back up: C:\\update\\old_updater.jar, C:\\update\\updater.jar->C:\\updater.jar<br />
 * (1 3 1)   1.0.0->1.0.1, replacement end, fileIndex: 1<br />
 * (1 2 2 433a5c7570 646174655c7465737 42e6a6172)   1.0.0->1.0.1, replacement start, fileIndex: 2, action: remove, back up: C:\\update\\old_test.jar, C:\\update\\test.jar->C:\\test.jar<br />
 * (1 3 2)   1.0.0->1.0.1, replacement end, fileIndex: 2<br />
 * (1 2 3 433a5c7570 646174655c6c6f676f2 e706e67)   1.0.0->1.0.1, replacement start, fileIndex: 3, action: replace, back up: C:\\update\\old_logo.png, C:\\update\\logo.png->C:\\logo.png<br />
 * (1 4 3)   1.0.0->1.0.1, replacement failed, fileIndex: 3<br />
 * (1 5 3)  1.0.0->1.0.1, fileIndex: 3<br />
 * (1 2 3 433a5c7570 646174655c6c6f676f2 e706e67)   1.0.0->1.0.1, replacement start, fileIndex: 3, action: replace, back up: C:\\update\\old_logo.png, C:\\update\\logo.png->C:\\logo.png<br />
 * (1 4 3)   1.0.0->1.0.1, replacement failed, fileIndex: 3<br />
 * (1 1)   1.0.0->1.0.1, finish
 * </p>
 * 
 * <p>Should invoke {@link #logStart(int, java.lang.String, java.lang.String)} first, {@link #logPatch(updater.patch.PatchLogWriter.Action, int, updater.patch.PatchLogWriter.OperationType, java.lang.String, java.lang.String)} 
 * second, {@link #logEnd()} last.</p>
 * 
 * <p>One log should serve only one apply-patch event.</p>
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchLogWriter implements Closeable {

    /**
     * The allowed patch action used by {@link #logPatch(updater.patch.PatchLogWriter.Action, int, updater.patch.PatchLogWriter.OperationType, java.lang.String, java.lang.String)}.
     */
    public static enum Action {

        /**
         * Start replacement.
         */
        START("start"),
        /**
         * Replacement finished.
         */
        FINISH("finish"),
        /**
         * Replacement failed.
         */
        FAILED("failed");
        /**
         * The string representation of the action.
         */
        private final String word;

        /**
         * Constructor.
         * @param word the string representation of the action
         */
        Action(String word) {
            this.word = word;
        }

        /**
         * Get the string representation of the action.
         * @return 
         */
        protected String word() {
            return word;
        }
    }
    /**
     * The output stream of the log file.
     */
    protected OutputStream out;
    /**
     * This log is recording update/patch actions of this patch id.
     */
    protected int currentPatchId;
    /**
     * This version-from of the patch with id {@link #currentPatchId}.
     */
    protected String currentPatchVersionFrom;
    /**
     * This version-to of the patch with id {@link #currentPatchId}.
     */
    protected String currentPatchVersionTo;

    /**
     * Constructor.
     * @param file the file to append the log on
     * @throws IOException if the file exists but is a directory rather than a regular file, does not exist but cannot be created, or cannot be opened for any other reason
     */
    public PatchLogWriter(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("argument 'file' cannot be null");
        }

        out = new FileOutputStream(file, true);

        currentPatchId = 0;
        currentPatchVersionFrom = "";
        currentPatchVersionTo = "";
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    /**
     * Record the patching start.
     * @param patchId the id of the patch
     * @param fromVersion the version-from of the patch
     * @param toVersion the version-to of the patch
     * @throws IOException error occurred when writing to the log
     */
    public void logStart(int patchId, String fromVersion, String toVersion) throws IOException {
        currentPatchId = patchId;
        currentPatchVersionFrom = fromVersion;
        currentPatchVersionTo = toVersion;

//        StringBuilder sb = new StringBuilder(1 + currentPatchId.length() + 3
//                + 1
//                + currentPatchVersionFrom.length() + 2 + currentPatchVersionTo.length() + 7
//                + 1);
        StringBuilder sb = new StringBuilder(32);

        sb.append('(');
        sb.append(currentPatchId);
        sb.append(" 0)");

        sb.append('\t');

        sb.append(currentPatchVersionFrom);
        sb.append("->");
        sb.append(currentPatchVersionTo);
        sb.append(", start");

        sb.append("\n");

        out.write(sb.toString().getBytes());
        out.flush();
    }

    /**
     * Log resume patching. This will output a new line character.
     * This is to solve the case if last log record is not ended with new line character due to IO error.
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
//        StringBuilder sb = new StringBuilder(1 + currentPatchId.length() + 3
//                + 1
//                + currentPatchVersionFrom.length() + 2 + currentPatchVersionTo.length() + 8
//                + 1);
        StringBuilder sb = new StringBuilder(32);

        sb.append('(');
        sb.append(currentPatchId);
        sb.append(" 1)");

        sb.append('\t');

        sb.append(currentPatchVersionFrom);
        sb.append("->");
        sb.append(currentPatchVersionTo);
        sb.append(", finish");

        sb.append("\n");

        out.write(sb.toString().getBytes());
        out.flush();
    }

    /**
     * Record file replacement action.
     * @param action the action
     * @param fileIndex the current file index in the patch
     * @param operationType the operation type of the patch event
     * @throws IOException error occurred when writing to log
     */
    public void logPatch(Action action, int fileIndex, OperationType operationType) throws IOException {
        logPatch(action, fileIndex, operationType, "", "", "");
    }

    /**
     * Record file replacement action.
     * @param action the action
     * @param fileIndex the current file index in the patch
     * @param operationType the operation type of the patch event
     * @param backupFilePath the back up file path
     * @param newFilePath the move-from path
     * @param destinationFilePath the move-to path
     * @throws IOException error occurred when writing to log
     */
    public void logPatch(Action action, int fileIndex, OperationType operationType, String backupFilePath, String newFilePath, String destinationFilePath) throws IOException {
        if (action == null) {
            throw new NullPointerException("argument 'action' cannot be null");
        }
        if (operationType == null) {
            throw new NullPointerException("argument 'operationType' cannot be null");
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

        String fileIndexString = Integer.toString(fileIndex);

//        StringBuilder sb = new StringBuilder(1 + currentPatchId.length() + 3
//                + 1
//                + currentPatchVersionFrom.length() + 2 + currentPatchVersionTo.length()
//                + 14 + action.word().length()
//                + 13 + fileIndexString.length()
//                + 10 + operationType.word().length()
//                + 2 + (oldFilePath != null ? oldFilePath.length() + 4 : 0) + (newFilePath != null ? newFilePath.length() : 0)
//                + 1);
        StringBuilder sb = new StringBuilder(64);

        sb.append('(');
        sb.append(currentPatchId);
        sb.append(' ');
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
        if (action == Action.START) {
            sb.append(' ');
            sb.append(CommonUtil.byteArrayToHexString(backupFilePath.getBytes("UTF-8")));
            sb.append(' ');
            sb.append(CommonUtil.byteArrayToHexString(newFilePath.getBytes("UTF-8")));
            sb.append(' ');
            sb.append(CommonUtil.byteArrayToHexString(destinationFilePath.getBytes("UTF-8")));
        }
        sb.append(')');

        sb.append('\t');

        sb.append(currentPatchVersionFrom);
        sb.append("->");
        sb.append(currentPatchVersionTo);

        sb.append(", replacement ");
        sb.append(action.word());

        sb.append(", fileIndex: ");
        sb.append(fileIndexString);

        if (action == Action.START) {
            sb.append(", action: ");
            sb.append(operationType.getValue());

            sb.append(", back up: ");
            sb.append(backupFilePath);
            sb.append(", new: ");
            sb.append(newFilePath);
            sb.append(", dest: ");
            sb.append(destinationFilePath);
        }

        sb.append("\n");

        out.write(sb.toString().getBytes());
        out.flush();
    }

    /**
     * Log the revert action.
     * @param fileIndex the file index of the revertion
     * @throws IOException error occurred when writing to log
     */
    public void logRevert(int fileIndex) throws IOException {
        String fileIndexString = Integer.toString(fileIndex);

        StringBuilder sb = new StringBuilder();

        sb.append('(');
        sb.append(currentPatchId);
        sb.append(" 5 ");
        sb.append(fileIndex);
        sb.append(')');

        sb.append('\t');

        sb.append(currentPatchVersionFrom);
        sb.append("->");
        sb.append(currentPatchVersionTo);

        sb.append(", fileIndex: ");
        sb.append(fileIndexString);

        sb.append("\n");

        out.write(sb.toString().getBytes());
        out.flush();
    }
}
