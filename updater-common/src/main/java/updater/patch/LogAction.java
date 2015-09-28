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
