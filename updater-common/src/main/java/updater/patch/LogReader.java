// Copyright (c) 2012 Chan Wai Shing
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package updater.patch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import updater.util.CommonUtil;

/**
 * Patch log reader.
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
 * <p>One log should serve only one apply-patch event.</p>
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class LogReader {

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
   * Indicate when to start to patch the unfinished patching. -1 means patch 
   * finished.
   */
  protected int startFileIndex;

  /**
   * Constructor.
   * @param file the log file
   * @throws IOException error occurred when reading the log file
   */
  public LogReader(File file) throws IOException {
    if (file == null) {
      throw new NullPointerException("argument 'file' cannot be null");
    }

    logStarted = false;
    logEnded = false;
    revertList = new ArrayList<PatchRecord>();
    failList = new ArrayList<PatchRecord>();
    startFileIndex = 1;
    int operationId = 0;

    TreeMap<Integer, PatchRecord> _revertMap = new TreeMap<Integer, PatchRecord>();
    Map<Integer, PatchRecord> _failMap = new TreeMap<Integer, PatchRecord>();

    Pattern logPattern = Pattern.compile("^(?:"
            + "(0|1)|"
            + "(2)\\s([0-9]+)\\s([0-9]+)\\s([0-9]+)\\s\"((?:[^\\\\\"]|\\\\.)*)\"\\s\"((?:[^\\\\\"]|\\\\.)*)\"\\s\"((?:[^\\\\\"]|\\\\.)*)\"|"
            + "(3|4|5)\\s([0-9]+)(?:\\s([0-9]+))?"
            + ")$");

    // not very strict check, assume the log is correct and in sequence
    BufferedReader in = null;
    try {
      in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

      String readLine = null;
      int currentFileIndex = -1;
      boolean backupFileExist = false, newFileExist = false, destinationFileExist = false;
      String currentBackupPath = null, currentfromPath = null, currentToPath = null;

      while ((readLine = in.readLine()) != null) {
        Matcher matcher = logPattern.matcher(readLine);
        if (!matcher.matches()) {
          // broken log
          continue;
        }

        int actionId = -1;
        try {
          if (matcher.group(1) != null) {
            actionId = Integer.parseInt(matcher.group(1));
          } else if (matcher.group(2) != null) {
            actionId = Integer.parseInt(matcher.group(2));
          } else if (matcher.group(9) != null) {
            actionId = Integer.parseInt(matcher.group(9));
          }
        } catch (NumberFormatException ex) {
          throw new IOException("Log format invalid.");
        }

        switch (actionId) {
          case 0:
            currentFileIndex = -1;
            logStarted = true;
            break;
          case 1:
            currentFileIndex = -1;
            logEnded = true;
            break;
          case 2:
            currentFileIndex = Integer.parseInt(matcher.group(3));
            operationId = Integer.parseInt(matcher.group(4));
            destinationFileExist = Integer.parseInt(matcher.group(5)) == 1;
            currentBackupPath = matcher.group(6).replace("\\\"", "\"");
            currentfromPath = matcher.group(7).replace("\\\"", "\"");
            currentToPath = matcher.group(8).replace("\\\"", "\"");
            break;
          case 3:
            if (currentFileIndex != Integer.parseInt(matcher.group(10))) {
              throw new IOException("Log format invalid.");
            }
            _revertMap.put(currentFileIndex, new PatchRecord(currentFileIndex, operationId, destinationFileExist, currentBackupPath, currentfromPath, currentToPath));
            currentBackupPath = null;
            if (currentFileIndex >= startFileIndex) {
              startFileIndex = currentFileIndex + 1;
            }
            currentFileIndex = -1;
            break;
          case 4:
            if (currentFileIndex != Integer.parseInt(matcher.group(10))) {
              throw new IOException("Log format invalid.");
            }
            _failMap.put(currentFileIndex, new PatchRecord(currentFileIndex, operationId, destinationFileExist, currentBackupPath, currentfromPath, currentToPath));
            currentBackupPath = null;
            if (currentFileIndex >= startFileIndex) {
              startFileIndex = currentFileIndex + 1;
            }
            currentFileIndex = -1;
            break;
          case 5:
            logEnded = false;
            int revertFileIndex = Integer.parseInt(matcher.group(10));
            PatchRecord revertRecord = _revertMap.remove(revertFileIndex);
            if (revertRecord != null) {
              _failMap.put(revertFileIndex, revertRecord);
            }
            break;
        }
      }

      for (Integer key : _revertMap.descendingKeySet()) {
        revertList.add(_revertMap.get(key));
      }
      for (Integer key : _failMap.keySet()) {
        if (!_revertMap.containsKey(key)) {
          failList.add(_failMap.get(key));
        }
      }

      if (currentBackupPath != null) {
        failList.add(new PatchRecord(startFileIndex, operationId, destinationFileExist, currentBackupPath, currentfromPath, currentToPath));
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
   * Get the file index that indicate when to start to patch the unfinished 
   * patching.
   * @return the file index
   */
  public int getStartFileIndex() {
    return startFileIndex;
  }
}
