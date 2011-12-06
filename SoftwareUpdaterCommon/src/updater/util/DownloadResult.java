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