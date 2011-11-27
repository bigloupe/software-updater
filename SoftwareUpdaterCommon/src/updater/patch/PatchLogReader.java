package updater.patch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import updater.util.CommonUtil;

/**
 * Patch log reader.
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
 * (1 1)   1.0.0->1.0.1, finish
 * </p>
 * 
 * <p>One log should serve only one apply-patch event.</p>
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchLogReader {

    /**
     * Indicate whether the log has started.
     */
    protected boolean logStarted;
    /**
     * Indicate whether the log has ended.
     */
    protected boolean logEnded;
    /**
     * The list that store the replace sequence to revert the patch.
     */
    protected List<PatchRecord> revertList;
    /**
     * The list that failed to do replacement due to possibly file locking.
     */
    protected List<PatchRecord> failList;
    /**
     * Unfinished replacement, possibly the replacement really didn't finish or log unfinished only. null means not any.
     */
    protected PatchRecord unfinishedReplacement;
    /**
     * Indicate when to start to patch the unfinished patching. -1 means patch finished.
     */
    protected int startFileIndex;

    /**
     * Constructor.
     * @param file the log file
     * @throws IOException error occurred when reading the log file
     */
    public PatchLogReader(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("argument 'file' cannot be null");
        }

        logStarted = false;
        logEnded = false;
        revertList = new ArrayList<PatchRecord>();
        failList = new ArrayList<PatchRecord>();
        unfinishedReplacement = null;
        startFileIndex = -1;

        List<PatchRecord> _revertList = new ArrayList<PatchRecord>();

        Pattern logPattern = Pattern.compile("^\\(([0-9]+)\\s(?:(0|1)|(2)\\s([0-9]+)\\s([a-z0-9]+)\\s([a-z0-9]+)\\s([a-z0-9]+)|(3|4)\\s([0-9]+))\\)\t.+?$");

        // not very strict check, assume the log is correct and in sequence
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            String readLine = null;
            int currentPatchId = -1, currentFileIndex = -1;
            String currentBackupPath = null, currentfromPath = null, currentToPath = null;

            while ((readLine = in.readLine()) != null) {
                Matcher matcher = logPattern.matcher(readLine);
                if (!matcher.matches()) {
                    // broken log
                    continue;
                }

                int actionId = -1;
                try {
                    if (matcher.group(2) != null) {
                        actionId = Integer.parseInt(matcher.group(2));
                    } else if (matcher.group(3) != null) {
                        actionId = Integer.parseInt(matcher.group(3));
                    } else if (matcher.group(8) != null) {
                        actionId = Integer.parseInt(matcher.group(8));
                    }
                } catch (NumberFormatException ex) {
                    throw new IOException("Log format invalid.");
                }

                switch (actionId) {
                    case 0:
                        currentPatchId = Integer.parseInt(matcher.group(1));
                        currentFileIndex = 0;
                        logStarted = true;
                        break;
                    case 1:
                        if (currentPatchId != Integer.parseInt(matcher.group(1))) {
                            throw new IOException("Log format invalid.");
                        }
                        currentPatchId = -1;
                        currentFileIndex = -1;
                        logEnded = true;
                        break;
                    case 2:
                        if (currentPatchId != Integer.parseInt(matcher.group(1))) {
                            throw new IOException("Log format invalid.");
                        }
                        currentFileIndex = Integer.parseInt(matcher.group(4));
                        currentBackupPath = new String(CommonUtil.hexStringToByteArray(matcher.group(5)), "UTF-8");
                        currentfromPath = new String(CommonUtil.hexStringToByteArray(matcher.group(6)), "UTF-8");
                        currentToPath = new String(CommonUtil.hexStringToByteArray(matcher.group(7)), "UTF-8");
                        break;
                    case 3:
                        if (currentPatchId != Integer.parseInt(matcher.group(1))) {
                            continue;
                        }
                        if (currentFileIndex != Integer.parseInt(matcher.group(9))) {
                            throw new IOException("Log format invalid.");
                        }
                        _revertList.add(new PatchRecord(currentFileIndex, currentBackupPath, currentfromPath, currentToPath));
                        currentFileIndex++;
                        currentBackupPath = null;
                        break;
                    case 4:
                        if (currentPatchId != Integer.parseInt(matcher.group(1))) {
                            continue;
                        }
                        if (currentFileIndex != Integer.parseInt(matcher.group(9))) {
                            throw new IOException("Log format invalid.");
                        }
                        failList.add(new PatchRecord(currentFileIndex, currentBackupPath, currentfromPath, currentToPath));
                        currentFileIndex++;
                        currentBackupPath = null;
                        break;
                }
            }

            for (int i = _revertList.size() - 1; i >= 0; i--) {
                revertList.add(_revertList.get(i));
            }

            if (currentFileIndex != -1) {
                startFileIndex = currentFileIndex;
                if (currentBackupPath != null) {
                    unfinishedReplacement = new PatchRecord(startFileIndex, currentBackupPath, currentfromPath, currentToPath);
                }
            }
        } finally {
            CommonUtil.closeQuietly(in);
        }
    }

    /**
     * Check whether the log has started.
     * @return true means started, false if not
     */
    public boolean isLogStarted() {
        return logStarted;
    }

    /**
     * Check whether the log has ended.
     * @return true means ended, false if not
     */
    public boolean isLogEnded() {
        return logEnded;
    }

    /**
     * Get the list that store the replace sequence to revert the patch.
     * @return a copy of the list
     */
    public List<PatchRecord> getRevertList() {
        return new ArrayList<PatchRecord>(revertList);
    }

    /**
     * Get the list that failed to do replacement due to possibly file locking.
     * @return a copy of the list
     */
    public List<PatchRecord> getFailList() {
        return new ArrayList<PatchRecord>(failList);
    }

    /**
     * Get unfinished replacement, possibly the replacement really didn't finish or log unfinished only.
     * @return the record, null means not any.
     */
    public PatchRecord getUnfinishedReplacement() {
        return unfinishedReplacement;
    }

    /**
     * Get the file index that indicate when to start to patch the unfinished patching. -1 means patch not started yet or finished.
     * @return the file index
     */
    public int getStartFileIndex() {
        return startFileIndex;
    }

    /**
     * The record used to represent the essential data in a row of replacement log.
     */
    public static class PatchRecord {

        /**
         * The file index of the record.
         */
        protected int fileIndex;
        /**
         * The backup file path of the replacement record.
         */
        protected String backupFilePath;
        /**
         * The copy-from file path of the replacement record.
         */
        protected String newFilePath;
        /**
         * The copy-to file path of the replacement record.
         */
        protected String destinationFilePath;

        /**
         * Constructor.
         * @param fileIndex the file index of the record
         * @param backupPath the backup file path
         * @param newFilePath the copy-from file path
         * @param destinationFilePath the copy-to file path
         */
        public PatchRecord(int fileIndex, String backupPath, String newFilePath, String destinationFilePath) {
            this.fileIndex = fileIndex;
            this.backupFilePath = backupPath;
            this.newFilePath = newFilePath;
            this.destinationFilePath = destinationFilePath;
        }

        /**
         * Get the file index of the record.
         * @return the file index
         */
        public int getFileIndex() {
            return fileIndex;
        }

        /**
         * Get the backup file path of the replacement record.
         * @return the file path
         */
        public String getBackupFilePath() {
            return newFilePath;
        }

        /**
         * Get the copy-from file path of the replacement record.
         * @return the file path
         */
        public String getNewFilePath() {
            return newFilePath;
        }

        /**
         * Get the copy-to file path of the replacement record.
         * @return the file path
         */
        public String getDestinationFilePath() {
            return destinationFilePath;
        }

        @Override
        public String toString() {
            return fileIndex + ": " + newFilePath;
        }

        @Override
        public boolean equals(Object compareTo) {
            if (compareTo == null || !(compareTo instanceof PatchRecord)) {
                return false;
            }
            if (compareTo == this) {
                return true;
            }
            PatchRecord _object = (PatchRecord) compareTo;

            return _object.getFileIndex() == fileIndex && _object.getNewFilePath().equals(getNewFilePath());
        }
    }
}
