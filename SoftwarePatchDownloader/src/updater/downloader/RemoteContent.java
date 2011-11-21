package updater.downloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import updater.script.Catalog;
import updater.util.DownloadProgressListener;
import updater.util.HTTPDownloader;
import updater.util.HTTPDownloader.DownloadResult;

/**
 * Utilities to download catalog and patch.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class RemoteContent {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }

    protected RemoteContent() {
    }

    /**
     * Get the catalog from Internet.
     * @param url the URL to download the catalog from
     * @param lastUpdateDate the last update date, if the catalog not be updated since this date, the content of the catalog will not be downloaded (save time and traffic)
     * @param key the RSA key to decrypt the catalog, null means no encryption
     * @param keyLength if <code>key</code> specified, provide the key length of the RSA key in byte
     * @return the get catalog result
     */
    public static GetCatalogResult getCatalog(String url, long lastUpdateDate, RSAPublicKey key, int keyLength) {
        if (url == null) {
            throw new NullPointerException("argument 'url' cannot be null");
        }

        FileOutputStream fout = null;
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            HTTPDownloader downloader = new HTTPDownloader();
            downloader.setOutputTo(bout);
            downloader.setIfModifiedSince(lastUpdateDate);
            DownloadResult result = downloader.download(new DownloadProgressListener() {

                @Override
                public void byteStart(long pos) {
                }

                @Override
                public void byteTotal(long total) {
                }

                @Override
                public void byteDownloaded(int numberOfBytes) {
                }

                @Override
                public void downloadRetry(DownloadResult result) {
                }
            }, new URL(url), null, -1, 0, 0);

            switch (result) {
                case SUCCEED:
                    byte[] content = bout.toByteArray();
                    // decrypt
                    if (key != null) {
                        content = Util.rsaDecrypt(key, keyLength, content);

                        // decompress
                        ByteArrayOutputStream decompressedOut = new ByteArrayOutputStream();
                        ByteArrayInputStream compressedIn = new ByteArrayInputStream(content);
                        try {
                            GZIPInputStream decompressedGIn = new GZIPInputStream(compressedIn);
                            int byteRead;
                            byte[] b = new byte[1024];
                            while ((byteRead = decompressedGIn.read(b)) != -1) {
                                decompressedOut.write(b, 0, byteRead);
                            }
                            content = decompressedOut.toByteArray();
                        } catch (Exception ex) {
                            // not GZIP compressed
                        }
                    }

                    return new GetCatalogResult(Catalog.read(content), false);
                case FILE_NOT_MODIFIED:
                    return new GetCatalogResult(null, true);
                case EXPECTED_LENGTH_NOT_MATCH:
                case CHECKSUM_FAILED:
                case FAILED:
                case INTERRUPTED:
                case RESUME_RANGE_FAILED:
                case RESUME_RANGE_RESPOND_INVALID:
                case RANGE_LENGTH_NOT_MATCH_CONTENT_LENGTH:
                    return new GetCatalogResult(null, false);
            }
        } catch (Exception ex) {
            Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
            if (debug) {
                Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
            }
            return new GetCatalogResult(null, false);
        } finally {
            Util.closeQuietly(fout);
        }

        return new GetCatalogResult(null, false);
    }

    /**
     * Get the patch from the Internet.
     * This will check the exist file in the path of <code>saveToFile</code> and determine resume download.
     * @param listener the progress and result listener
     * @param url the URL to download the patch from
     * @param saveToFile the place to save the downloaded patch
     * @param fileSHA256 the SHA-256 digest of the patch
     * @param expectedLength the expected file length of the patch
     * @return the get patch result
     */
    public static GetPatchResult getPatch(final GetPatchListener listener, String url, File saveToFile, String fileSHA256, int expectedLength) {
        if (listener == null) {
            throw new NullPointerException("argument 'listener' cannot be null");
        }
        if (url == null) {
            throw new NullPointerException("argument 'url' cannot be null");
        }
        if (saveToFile == null) {
            throw new NullPointerException("argument 'saveToFile' cannot be null");
        }
        if (!fileSHA256.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("SHA format invalid, expected: ^[0-9a-f]{64}$, checksum: " + fileSHA256);
        }

        FileOutputStream fout = null;
        try {
            HTTPDownloader downloader = new HTTPDownloader();
            if (saveToFile.length() > 0) {
                downloader.setResumeFile(saveToFile);
            } else {
                downloader.setOutputTo(fout = new FileOutputStream(saveToFile));
            }
            DownloadResult result = downloader.download(new DownloadProgressListener() {

                @Override
                public void byteStart(long pos) {
                    listener.byteStart(pos);
                }

                @Override
                public void byteTotal(long total) {
                }

                @Override
                public void byteDownloaded(int numberOfBytes) {
                    listener.byteDownloaded(numberOfBytes);
                }

                @Override
                public void downloadRetry(DownloadResult result) {
                }
            }, new URL(url), fileSHA256, expectedLength, 0, 0);

            switch (result) {
                case SUCCEED:
                case FILE_NOT_MODIFIED:
                    return new GetPatchResult(true, false);
                case EXPECTED_LENGTH_NOT_MATCH:
                case CHECKSUM_FAILED:
                case FAILED:
                case INTERRUPTED:
                case RESUME_RANGE_FAILED:
                case RESUME_RANGE_RESPOND_INVALID:
                case RANGE_LENGTH_NOT_MATCH_CONTENT_LENGTH:
                    return new GetPatchResult(false, false);
            }
        } catch (Exception ex) {
            if (debug) {
                Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
            }
            return new GetPatchResult(false, false);
        } finally {
            Util.closeQuietly(fout);
        }

        return new GetPatchResult(true, false);
    }

    /**
     * The listener for {@link #getPatch(updater.downloader.RemoteContent.GetPatchListener, java.lang.String, java.io.File, java.lang.String, int)}.
     */
    public static interface GetPatchListener {

        /**
         * The thread was interrupted.
         */
        void downloadInterrupted();

        /**
         * Notify which position to start the download from.
         * This should be called only once or less, and be called before first notifying {@link #byteDownloaded(int)}.
         * @param pos the position
         */
        void byteStart(long pos);

        /**
         * Notify the byte downloaded since last notification.
         * @param numberOfBytes the bytes downloaded
         */
        void byteDownloaded(int numberOfBytes);
    }

    /**
     * Get patch result for {@link #getPatch(updater.downloader.RemoteContent.GetPatchListener, java.lang.String, java.io.File, java.lang.String, int)}.
     */
    public static class GetPatchResult {

        /**
         * Get patch result. true if get patch succeed, false if not.
         */
        protected boolean result;
        /**
         * Indicate if the running thread was interrupted during execution. true if interrupted, false if not.
         */
        protected boolean interrupted;

        /**
         * Constructor.
         * @param result true if get patch succeed, false if not
         * @param interrupted true if the running thread was interrupted, false if not
         */
        public GetPatchResult(boolean result, boolean interrupted) {
            this.result = result;
            this.interrupted = interrupted;
        }

        /**
         * Get the result.
         * @return true if get patch succeed, false if not
         */
        public boolean getResult() {
            return result;
        }

        /**
         * Check if the running thread was interrupted during execution.
         * @return true if interrupted, false if not
         */
        public boolean isInterrupted() {
            return interrupted;
        }
    }

    /**
     * Get catalog result for {@link #getCatalog(java.lang.String, long, java.security.interfaces.RSAPublicKey, int)}.
     */
    public static class GetCatalogResult {

        /**
         * The catalog.
         */
        protected Catalog catalog;
        /**
         * Indicate if the catalog was checked modified.
         */
        protected boolean notModified;

        /**
         * Constructor.
         * @param catalog the catalog, null if get catalog failed or checked not midified
         * @param notModified true if checked the catalog not modified
         */
        public GetCatalogResult(Catalog catalog, boolean notModified) {
            this.catalog = catalog;
            this.notModified = notModified;
        }

        /**
         * Get the catalog.
         * @return the catalog, null if get catalog failed or checked not midified
         */
        public Catalog getCatalog() {
            return catalog;
        }

        /**
         * Check if the catalog was checked modified.
         * @return true if checked the catalog not modified
         */
        public boolean isNotModified() {
            return notModified;
        }
    }
}
