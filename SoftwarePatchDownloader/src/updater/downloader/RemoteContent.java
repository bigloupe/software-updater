package updater.downloader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import updater.script.Catalog;

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

        GetCatalogResult returnResult = new GetCatalogResult(null, false);

        InputStream in = null;
        HttpURLConnection httpConn = null;
        try {
            URL urlObj = new URL(url);

            // open connection
            URLConnection conn = urlObj.openConnection();
            if (!(conn instanceof HttpURLConnection)) {
                throw new MalformedURLException("It is not a valid http URL: " + urlObj.toString());
            }
            httpConn = (HttpURLConnection) conn;

            // connection setting
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);

            // set request header
            httpConn.setRequestProperty("Connection", "close");
//            httpConn.setRequestProperty("Accept-Encoding", "gzip");
            httpConn.setRequestProperty("User-Agent", "Software Updater");
            httpConn.setUseCaches(false);
            if (lastUpdateDate != -1) {
                httpConn.setIfModifiedSince(lastUpdateDate);
            }

            // connect
            httpConn.connect();

            // get header
            int httpStatusCode = httpConn.getResponseCode();
            String contentEncoding = httpConn.getHeaderField("Content-Encoding");
            int contentLength = -1;
            //<editor-fold defaultstate="collapsed" desc="content length">
            String contentLengthString = httpConn.getHeaderField("Content-Length");
            if (contentLengthString != null) {
                try {
                    contentLength = Integer.parseInt(contentLengthString.trim());
                } catch (NumberFormatException ex) {
                    // ignore
                }
            }
            //</editor-fold>

            // check according to header information
            if (httpStatusCode == 304 && lastUpdateDate != -1) {
                return new GetCatalogResult(null, true);
            } else if (httpStatusCode != 200) {
                returnResult = new GetCatalogResult(null, false);
                throw new Exception("HTTP status not 200, status: " + httpStatusCode);
            }


            // download
            in = httpConn.getInputStream();
            in = (contentEncoding != null && contentEncoding.equals("gzip")) ? new GZIPInputStream(in, 8192) : new BufferedInputStream(in);
            ByteArrayOutputStream buffer = contentLength == -1 ? new ByteArrayOutputStream() : new ByteArrayOutputStream(contentLength);
            int byteRead;
            byte[] b = new byte[32];
            while ((byteRead = in.read(b)) != -1) {
                if (Thread.interrupted()) {
                    throw new InterruptedException("Download cancelled by user.");
                }
                buffer.write(b, 0, byteRead);
            }

            // downloaded
            byte[] content = buffer.toByteArray();

            // decrypt
            if (key != null) {
                content = Util.rsaDecrypt(key, keyLength, content);
            }

            // decompress
            ByteArrayOutputStream decompressedOut = new ByteArrayOutputStream();
            ByteArrayInputStream compressedIn = new ByteArrayInputStream(content);
            try {
                GZIPInputStream decompressedGIn = new GZIPInputStream(compressedIn);
                while ((byteRead = decompressedGIn.read(b)) != -1) {
                    decompressedOut.write(b, 0, byteRead);
                }
                content = decompressedOut.toByteArray();
            } catch (Exception ex) {
                // not GZIP compressed
            }

            returnResult = new GetCatalogResult(Catalog.read(content), false);
        } catch (Exception ex) {
            if (debug) {
                Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
            Util.closeQuietly(in);
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }

        return returnResult;
    }

    /**
     * Read the content in the <code>file</code> and put into the <code>digest</code>.
     * @param digest the digest object
     * @param file the file to read the content from
     * @throws IOException error occurred when reading file
     */
    protected static void digest(MessageDigest digest, File file) throws IOException {
        if (digest == null) {
            throw new NullPointerException("argument 'digest' cannot be null");
        }
        if (file == null) {
            throw new NullPointerException("argument 'file' cannot be null");
        }

        FileInputStream fin = null;
        try {
            long fileLength = file.length();
            fin = new FileInputStream(file);

            int byteRead, cumulateByteRead = 0;
            byte[] b = new byte[32768];
            while ((byteRead = fin.read(b)) != -1) {
                digest.update(b, 0, byteRead);

                cumulateByteRead += byteRead;
                if (cumulateByteRead >= fileLength) {
                    break;
                }
            }

            if (cumulateByteRead != fileLength) {
                throw new IOException("The total number of bytes read does not match the file size. Actual file size: " + fileLength + ", bytes read: " + cumulateByteRead + ", path: " + file.getAbsolutePath());
            }
        } finally {
            Util.closeQuietly(fin);
        }
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
    public static GetPatchResult getPatch(GetPatchListener listener, String url, File saveToFile, String fileSHA256, int expectedLength) {
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

        GetPatchResult returnResult = new GetPatchResult(false, false);

        InputStream in = null;
        HttpURLConnection httpConn = null;
        OutputStream fout = null;
        try {
            URL urlObj = new URL(url);
            long fileLength = saveToFile.length();

            // check saveToFile if exist with fileLength and expectedLength
            if (fileLength != 0) {
                if ((fileLength == expectedLength && !Util.getSHA256String(saveToFile).equals(fileSHA256))
                        || fileLength > expectedLength) {
                    // truncate/delete the file
                    try {
                        new FileOutputStream(saveToFile).close();
                    } catch (IOException ex) {
                        saveToFile.delete();
                    }
                    fileLength = 0;
                } else if (fileLength == expectedLength) {
                    listener.byteStart(fileLength);
                    return new GetPatchResult(true, false);
                }
            }


            // open connection
            URLConnection conn = urlObj.openConnection();
            if (!(conn instanceof HttpURLConnection)) {
                throw new MalformedURLException("It is not a valid http URL: " + conn.toString());
            }
            httpConn = (HttpURLConnection) conn;

            // connection setting
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);

            // set request header
            if (fileLength != 0) {
                httpConn.setRequestProperty("Range", "bytes=" + fileLength + "-");
            }
            httpConn.setRequestProperty("Connection", "close");
            httpConn.setRequestProperty("User-Agent", "Software Updater");
            httpConn.setUseCaches(false);

            // connect
            httpConn.connect();

            // get header
            int httpStatusCode = httpConn.getResponseCode();
            String contentEncoding = httpConn.getHeaderField("Content-Encoding");
            int contentLength = -1;
            //<editor-fold defaultstate="collapsed" desc="content length">
            String contentLengthString = httpConn.getHeaderField("Content-Length");
            if (contentLengthString != null) {
                if (fileLength != 0) {
                    Pattern pattern = Pattern.compile("^([0-9]+)-([0-9]+)/([0-9]+)$");
                    String contentRangeString = httpConn.getHeaderField("Content-Range");
                    if (contentRangeString != null) {
                        Matcher matcher = pattern.matcher(contentRangeString.trim());
                        if (matcher.matches()) {
                            int rangeStart = Integer.parseInt(matcher.group(1));
                            int rangeEnd = Integer.parseInt(matcher.group(2));
                            contentLength = Integer.parseInt(matcher.group(3));
                            if (rangeStart != fileLength) {
                                throw new Exception("Request byte range from " + rangeStart + " but respond byte range: " + contentRangeString);
                            }
                            if (contentLength - 1 != rangeEnd) {
                                throw new Exception("Respond byte range end do not match content length, http respond range end: " + rangeEnd + ", content length: " + contentLength);
                            }
                        }
                    }
                } else {
                    try {
                        contentLength = Integer.parseInt(contentLengthString.trim());
                    } catch (NumberFormatException ex) {
                    }
                }
            }
            //</editor-fold>

            // check according to header information
            if (httpStatusCode != 200 && httpStatusCode != 206) {
                throw new Exception("HTTP status is not 200 or 206, status: " + httpStatusCode);
            }
            if (contentLength != - 1 && contentLength != expectedLength) {
                throw new Exception("Expected length and respond content length not match, expected: " + expectedLength + ", actual: " + contentLength);
            }

            // notify listener the starting byte position
            if (httpStatusCode == 200) {
                listener.byteStart(0);
            } else if (httpStatusCode == 206) {
                listener.byteStart(fileLength);
            }

            // download
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (fileLength != 0) {
                digest(digest, saveToFile);
            }
            in = httpConn.getInputStream();
            in = (contentEncoding != null && contentEncoding.equals("gzip")) ? new GZIPInputStream(in, 32768) : new BufferedInputStream(in, 32768);
            fout = new BufferedOutputStream(new FileOutputStream(saveToFile, httpStatusCode == 206), 32768);

            int byteRead, cumulateByteRead = 0;
            byte[] b = new byte[32];
            while ((byteRead = in.read(b)) != -1) {
                if (Thread.interrupted()) {
                    listener.downloadInterrupted();
                    throw new InterruptedException();
                }

                digest.update(b, 0, byteRead);
                fout.write(b, 0, byteRead);
                cumulateByteRead += byteRead;

                if (listener != null) {
                    listener.byteDownloaded(byteRead);
                }
            }

            // check the downloaded file
            if (cumulateByteRead + fileLength != expectedLength) {
                throw new Exception("Error occurred when reading (cumulated bytes read != expected length), cumulated bytes read: " + cumulateByteRead + " + " + fileLength + ", expected: " + expectedLength);
            }
            if (!Util.byteArrayToHexString(digest.digest()).equals(fileSHA256)) {
                throw new Exception("Checksum not matched. got: " + Util.byteArrayToHexString(digest.digest()) + ", expected: " + fileSHA256);
            }

            returnResult = new GetPatchResult(true, false);
        } catch (InterruptedException ex) {
            returnResult = new GetPatchResult(false, true);
        } catch (Exception ex) {
            returnResult = new GetPatchResult(false, false);
            if (debug) {
                Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
            Util.closeQuietly(in);
            if (httpConn != null) {
                httpConn.disconnect();
            }
            Util.closeQuietly(fout);
        }

        return returnResult;
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
