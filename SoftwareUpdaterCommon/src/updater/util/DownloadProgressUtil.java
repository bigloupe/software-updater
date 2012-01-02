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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A tool to calculate/monitor the download speeding and calculate remaining 
 * time and download size.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class DownloadProgressUtil {

  /**
   * Current downloaded size in bytes.
   */
  protected long downloadedSize;
  /**
   * Total size needed to download (in bytes).
   */
  protected long totalSize;
  /**
   * The download speed will be taken average within this time span, it is in 
   * milli second. Default is 5000.
   */
  protected int averageTimeSpan = 5000;
  /**
   * The feed records list. Expired records will be removed when update.
   */
  protected List<Record> records;
  /**
   * The current download speed within {@link averageTimeSpan}. It is 
   * bytes/second.
   */
  protected long speed;

  /**
   * Constructor.
   */
  public DownloadProgressUtil() {
    downloadedSize = 0;
    totalSize = 0;
    records = new LinkedList<Record>();
    speed = 0;
  }

  /**
   * Get the current downloaded size.
   * @return the size in bytes
   */
  public long getDownloadedSize() {
    return downloadedSize;
  }

  /**
   * Set the current downloaded size.
   * @param downloadedSize the size in bytes
   */
  public void setDownloadedSize(long downloadedSize) {
    if (downloadedSize < 0) {
      throw new IllegalArgumentException("argument 'downloadSize' should >= 0");
    }
    this.downloadedSize = downloadedSize;
  }

  /**
   * Get total size needed to download (in bytes).
   * @return the size
   */
  public long getTotalSize() {
    return totalSize;
  }

  /**
   * Set total size needed to download (in bytes).
   * @param totalSize the size
   */
  public void setTotalSize(long totalSize) {
    if (totalSize < 0) {
      throw new IllegalArgumentException("argument 'totalSize' should >= 0");
    }
    this.totalSize = totalSize;
  }

  /**
   * Get the time span. See {@link #averageTimeSpan} in advance.
   * @return the time in milli second
   */
  public int getAverageTimeSpan() {
    return averageTimeSpan;
  }

  /**
   * Set the time span. See {@link #averageTimeSpan} in advance.
   * @param averageTimeSpan the time in milli second
   */
  public synchronized void setAverageTimeSpan(int averageTimeSpan) {
    if (averageTimeSpan < 1) {
      throw new IllegalArgumentException("argument 'averageTimeSpan' should >= 1");
    }
    this.averageTimeSpan = averageTimeSpan;
    updateSpeed();
  }

  /**
   * Notify how many bytes has been downloaded since last feed.
   * @param bytesDownloaded the total bytes downloaded this time
   */
  public synchronized void feed(long bytesDownloaded) {
    if (bytesDownloaded < 0) {
      throw new IllegalArgumentException("argument 'bytesDownloaded' should >= 0");
    }
    this.downloadedSize += bytesDownloaded;
    records.add(new Record(bytesDownloaded));
    updateSpeed();
  }

  /**
   * Get the current download speed within {@link averageTimeSpan}.
   * @return the speed is bytes/second
   */
  public long getSpeed() {
    return speed;
  }

  /**
   * Get the remaining download time.
   * @return the time in second
   */
  public int getTimeRemaining() {
    return speed == 0 ? 0 : (int) ((double) (totalSize - downloadedSize) / (double) speed);
  }

  /**
   * Update {@link #speed}, it will also remove expired records according to 
   * {@link #averageTimeSpan} from {@link #records}.
   */
  protected void updateSpeed() {
    // should be synchronized
    long currentTime = System.currentTimeMillis();

    long minimumTime = currentTime;
    long bytesDownloadedWithinPeriod = 0;

    Iterator<Record> iterator = records.iterator();
    while (iterator.hasNext()) {
      Record record = iterator.next();
      if (currentTime - record.time > averageTimeSpan) {
        iterator.remove();
      } else {
        if (record.time < minimumTime) {
          minimumTime = record.time;
        }
        bytesDownloadedWithinPeriod += record.byteDownloaded;
      }
    }

    speed = currentTime == minimumTime ? 0 : (long) ((double) bytesDownloadedWithinPeriod / ((double) (currentTime - minimumTime) / 1000F));
  }

  /**
   * Feed record used by {@link #feed(long)}.
   * <p>For performance concern, no getter/setter methods, direct access only.
   */
  protected static class Record {

    /**
     * The time when this record is constructed.
     */
    protected long time;
    /**
     * The byte downloaded in one feed.
     */
    protected long byteDownloaded;

    /**
     * Constructor.
     * @param byteDownloaded the byte downloaded in one feed
     */
    protected Record(long byteDownloaded) {
      time = System.currentTimeMillis();
      this.byteDownloaded = byteDownloaded;
    }
  }
}
