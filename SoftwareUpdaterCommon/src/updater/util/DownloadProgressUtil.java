package updater.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A tool to calculate/monitor the download speeding and calculate remaining time and download size.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class DownloadProgressUtil {

    /**
     * Current downloaded size.
     */
    protected long downloadedSize;
    /**
     * Total size needed to download.
     */
    protected long totalSize;
    /**
     * The download speed will be taken average within this time span, it is in milli second. Default is 5000.
     */
    protected int averageTimeSpan = 5000;
    /**
     * The feed records list. Expired records will be removed when update.
     */
    protected List<Record> records;
    /**
     * The current download speed. It is bytes/second.
     */
    protected long speed;

    public DownloadProgressUtil() {
        downloadedSize = 0;
        totalSize = 0;
        records = new LinkedList<Record>();
        speed = 0;
    }

    public long getDownloadedSize() {
        return downloadedSize;
    }

    public void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public int getAverageTimeSpan() {
        return averageTimeSpan;
    }

    public synchronized void setAverageTimeSpan(int averageTimeSpan) {
        this.averageTimeSpan = averageTimeSpan;
        updateSpeed();
    }

    /**
     * Notify how many bytes has been downloaded since last feed.
     * @param bytesDownloaded the total bytes downloaded this time
     */
    public synchronized void feed(long bytesDownloaded) {
        this.downloadedSize += bytesDownloaded;
        records.add(new Record(bytesDownloaded));
        updateSpeed();
    }

    public long getSpeed() {
        return speed;
    }

    public int getTimeRemaining() {
        return speed == 0 ? 0 : (int) ((double) (totalSize - downloadedSize) / (double) speed);
    }

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

    protected static class Record {

        /**
         * For performance concern, no getter/setter methods, direct access only.
         */
        protected long time;
        protected long byteDownloaded;

        protected Record(long byteDownloaded) {
            time = System.currentTimeMillis();
            this.byteDownloaded = byteDownloaded;
        }
    }
}
