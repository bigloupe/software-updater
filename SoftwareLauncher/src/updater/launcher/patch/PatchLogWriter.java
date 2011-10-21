package updater.launcher.patch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * <b>Format: </b><br />
 * ([patch/update id] [action code] [file index (optional)])    [from version]->[to version], [action (string)], fileIndex: [file Index], action: [operation type (string)], [old file path] -> [new file path]
 * <ul>
 * <li>action code: 0 - start, 1 - finish, 2 - patch start, 3 - patch finish</li>
 * </ul>
 * Note that '[' and ']' didn't really exist.
 * 
 * <p>
 * <b>Sample:</b><br />
 * (1 0)   1.0.0->1.0.1, start<br />
 * (1 2 0)   1.0.0->1.0.1, patch start, fileIndex: 0, action: patch, start.jar->start.jar<br />
 * (1 3 0)   1.0.0->1.0.1, patch end, fileIndex: 0<br />
 * (1 2 1)   1.0.0->1.0.1, patch start, fileIndex: 1, action: new, updater.jar<br />
 * (1 3 1)   1.0.0->1.0.1, patch end, fileIndex: 1<br />
 * (1 2 2)   1.0.0->1.0.1, patch start, fileIndex: 2, action: remove, test.jar<br />
 * (1 3 2)   1.0.0->1.0.1, patch end, fileIndex: 2<br />
 * (1 2 3)   1.0.0->1.0.1, patch start, fileIndex: 3, action: replace, logo.png->logo.png<br />
 * (1 3 3)   1.0.0->1.0.1, patch end, fileIndex: 3<br />
 * (1 1)   1.0.0->1.0.1, finish
 * </p>
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchLogWriter {

    public static enum Action {

        START("start"), FINISH("finish");
        private final String word;

        Action(String word) {
            this.word = word;
        }

        private String word() {
            return word;
        }
    }

    public static enum OperationType {

        PATCH("patch"), REPLACE("replace"), REMOVE("remove"), NEW("new");
        private final String word;

        OperationType(String word) {
            this.word = word;
        }

        private String word() {
            return word;
        }

        public static OperationType get(String type) {
            for (OperationType obj : OperationType.values()) {
                if (obj.word().equals(type)) {
                    return obj;
                }
            }
            return null;
        }
    }
    protected FileOutputStream out;
    //
    protected String currentPatchId;
    protected String currentPatchVersionFrom;
    protected String currentPatchVersionTo;

    public PatchLogWriter(File file) throws IOException {
        out = new FileOutputStream(file, true);

        currentPatchId = "0";
        currentPatchVersionFrom = "";
        currentPatchVersionTo = "";
    }

    public void close() throws IOException {
        out.close();
    }

    public void logStart(int patchId, String fromVersion, String toVersion) throws IOException {
        currentPatchId = Integer.toString(patchId);
        currentPatchVersionFrom = fromVersion;
        currentPatchVersionTo = toVersion;

//        StringBuilder sb = new StringBuilder(1 + currentPatchId.length() + 3
//                + 1
//                + currentPatchVersionFrom.length() + 2 + currentPatchVersionTo.length() + 7
//                + 1);
        StringBuilder sb = new StringBuilder(32);

        sb.append('(');
        sb.append(currentPatchId);
        sb.append(' ');
        sb.append('0');
        sb.append(')');

        sb.append('\t');

        sb.append(currentPatchVersionFrom);
        sb.append("->");
        sb.append(currentPatchVersionTo);
        sb.append(", start");

        sb.append("\n");

        out.write(sb.toString().getBytes());
        out.flush();
    }

    public void logEnd() throws IOException {
//        StringBuilder sb = new StringBuilder(1 + currentPatchId.length() + 3
//                + 1
//                + currentPatchVersionFrom.length() + 2 + currentPatchVersionTo.length() + 8
//                + 1);
        StringBuilder sb = new StringBuilder(32);

        sb.append('(');
        sb.append(currentPatchId);
        sb.append(' ');
        sb.append('1');
        sb.append(')');

        sb.append('\t');

        sb.append(currentPatchVersionFrom);
        sb.append("->");
        sb.append(currentPatchVersionTo);
        sb.append(", finish");

        sb.append("\n");

        out.write(sb.toString().getBytes());
        out.flush();
    }

    public void logPatch(Action action, int fileIndex, OperationType operationType, String oldFilePath, String newFilePath) throws IOException {
        String fileIndexString = Integer.toString(fileIndex);

//        StringBuilder sb = new StringBuilder(1 + currentPatchId.length() + 3
//                + 1
//                + currentPatchVersionFrom.length() + 2 + currentPatchVersionTo.length()
//                + 8 + action.word().length()
//                + 13 + fileIndexString.length()
//                + 10 + operationType.word().length()
//                + 2 + (oldFilePath != null ? oldFilePath.length() + 4 : 0) + (newFilePath != null ? newFilePath.length() : 0)
//                + 1);
        StringBuilder sb = new StringBuilder(64);

        sb.append('(');
        sb.append(currentPatchId);
        sb.append(' ');
        sb.append(action == Action.START ? '2' : '3');
        sb.append(' ');
        sb.append(fileIndex);
        sb.append(')');

        sb.append('\t');

        sb.append(currentPatchVersionFrom);
        sb.append("->");
        sb.append(currentPatchVersionTo);

        sb.append(", patch ");
        sb.append(action.word());

        sb.append(", fileIndex: ");
        sb.append(fileIndexString);

        sb.append(", action: ");
        sb.append(operationType.word());

        sb.append(", ");
        if (oldFilePath != null) {
            sb.append(oldFilePath);
            sb.append(" -> ");
        }
        if (newFilePath != null) {
            sb.append(newFilePath);
        }

        sb.append("\n");

        out.write(sb.toString().getBytes());
        out.flush();
    }
}
