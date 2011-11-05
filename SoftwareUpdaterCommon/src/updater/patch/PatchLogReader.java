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
public class PatchLogReader {

    protected List<Integer> finishedPatches;
    protected UnfinishedPatch unfinishedPatch;

    public PatchLogReader(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("argument 'file' cannot be null");
        }

        finishedPatches = new ArrayList<Integer>();

        Pattern logPattern = Pattern.compile("^\\(([0-9]+)\\s(?:(0|1)|(2|3)\\s([0-9]+))\\)\t.+?$");

        // not very strict check, assume IO is correct and in sequence
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            String readLine = null;
            int currentPatchId = -1, currentFileIndex = 0;

            while ((readLine = in.readLine()) != null) {
                Matcher matcher = logPattern.matcher(readLine);
                if (!matcher.matches()) {
                    // broken log
                    continue;
                }

                int actionId = Integer.parseInt(matcher.group(2) != null ? matcher.group(2) : matcher.group(3));
                switch (actionId) {
                    case 0:
                        currentPatchId = Integer.parseInt(matcher.group(1));
                        currentFileIndex = 0;
                        break;
                    case 1:
                        if (currentPatchId != -1) {
                            finishedPatches.add(currentPatchId);
                        }
                        currentPatchId = -1;
                        currentFileIndex = 0;
                        break;
                    case 2:
                        currentFileIndex = Integer.parseInt(matcher.group(4));
                        break;
                    case 3:
                        currentFileIndex++;
                        break;
                }
            }

            if (currentPatchId != -1) {
                unfinishedPatch = new UnfinishedPatch(currentPatchId, currentFileIndex);
            }
        } finally {
            CommonUtil.closeQuietly(in);
        }
    }

    public List<Integer> getfinishedPatches() {
        return new ArrayList<Integer>(finishedPatches);
    }

    public UnfinishedPatch getUnfinishedPatch() {
        return unfinishedPatch;
    }

    public static class UnfinishedPatch {

        protected int patchId;
        protected int fileIndex;

        protected UnfinishedPatch(int patchId, int fileIndex) {
            this.patchId = patchId;
            this.fileIndex = fileIndex;
        }

        public int getPatchId() {
            return patchId;
        }

        /**
         * The recovery should start from this file index (including).
         */
        public int getFileIndex() {
            return fileIndex;
        }
    }
}
