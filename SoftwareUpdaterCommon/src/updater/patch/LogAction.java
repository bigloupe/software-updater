/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package updater.patch;

/**
 * The allowed patch action used by {@link #logPatch(updater.patch.LogWriter.Action, int, updater.patch.LogWriter.OperationType, java.lang.String, java.lang.String)}.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public enum LogAction {

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
  LogAction(String word) {
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
