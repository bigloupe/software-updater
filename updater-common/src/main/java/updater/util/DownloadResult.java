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
package updater.util;

/**
 * Download result for {@link #download(updater.util.DownloadProgressListener, java.net.URL, java.lang.String, int)}.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public enum DownloadResult {

  SUCCEED("SUCCEED"),
  FILE_NOT_MODIFIED("FILE_NOT_MODIFIED"),
  EXPECTED_LENGTH_NOT_MATCH("EXPECTED_LENGTH_NOT_MATCH"),
  CHECKSUM_FAILED("CHECKSUM_FAILED"),
  FAILED("FAILED"),
  INTERRUPTED("INTERRUPTED"),
  RESUME_RANGE_FAILED("RESUME_RANGE_FAILED"),
  RESUME_RANGE_RESPOND_INVALID("RESUME_RANGE_RESPOND_INVALID"),
  RANGE_LENGTH_NOT_MATCH_CONTENT_LENGTH("RANGE_LENGTH_NOT_MATCH_CONTENT_LENGTH");
  protected final String value;

  /**
   * Constructor.
   * 
   * @param value 
   */
  DownloadResult(String value) {
    this.value = value;
  }

  /**
   * Get the unique integer representation for this type of compression.
   * 
   * @return the integer value
   */
  public String getValue() {
    return value;
  }

  /**
   * Get the {@link updater.util.HTTPDownloader.DownloadResult} by the 
   * downloadResults' string value.
   * 
   * @param value the string value
   * 
   * @return the {@link updater.util.HTTPDownloader.DownloadResult} or null 
   * if not correspondent found
   */
  public static DownloadResult getDownloadResult(String value) {
    DownloadResult[] downloadResults = DownloadResult.values();
    for (DownloadResult downloadResult : downloadResults) {
      if (downloadResult.getValue().equals(value)) {
        return downloadResult;
      }
    }
    return null;
  }
}