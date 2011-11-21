package updater.util;

import updater.util.HTTPDownloader.DownloadResult;

/**
 * Progress Listener for {@link updater.util.HTTPDownloader}.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public interface DownloadProgressListener {

    /**
     * Notify which position to start the download from.
     * This should be called only once or less, and be called before first notifying {@link #byteDownloaded(int)}.
     * @param pos the position
     */
    void byteStart(long pos);

    /**
     * Notify the total length of the file to download.
     * This should be called only once or less, and be called before first notifying {@link #byteDownloaded(int)}.
     * @param total the size in byte, -1 means length not known
     */
    void byteTotal(long total);

    /**
     * Notify the byte downloaded since last notification.
     * @param numberOfBytes the bytes downloaded
     */
    void byteDownloaded(int numberOfBytes);

    /**
     * The downloader is going to retry download. This will be invoked every time before the downloader is going to retry.
     * @param result the reason for why the downloader will retry download
     */
    void downloadRetry(DownloadResult result);
}
