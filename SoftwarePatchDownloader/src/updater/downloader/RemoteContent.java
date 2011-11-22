package updater.downloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import javax.crypto.BadPaddingException;
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
     * @param out the stream to output the catalog data to
     * @param url the URL to download the catalog from
     * @param lastUpdateDate the last update date, if the catalog not be updated since this date, the content of the catalog will not be downloaded (save time and traffic)
     * @param key the RSA key to decrypt the catalog, null means no encryption
     * @param keyLength if {@code key} specified, provide the key length of the RSA key in byte
     * @return the get catalog result
     * @throws MalformedURLException {@code url} is not a valid HTTP URL
     * @throws IOException catalog content invalid
     */
    public static DownloadResult getCatalog(OutputStream out, String url, long lastUpdateDate, RSAPublicKey key, int keyLength) throws MalformedURLException, IOException {
        if (url == null) {
            throw new NullPointerException("argument 'url' cannot be null");
        }

        FileOutputStream fout = null;
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            HTTPDownloader downloader = new HTTPDownloader();
            downloader.setOutputTo(bout);
            downloader.setIfModifiedSince(lastUpdateDate);

            DownloadResult result = downloader.download(null, new URL(url), null, -1, 10, 1000);
            if (result == DownloadResult.SUCCEED) {
                byte[] content = bout.toByteArray();
                // decrypt & decompress
                if (key != null) {
                    content = Util.rsaDecrypt(key, keyLength, content);
                    content = Util.GZipDecompress(content);
                }
                out.write(content);
            }

            return result;
        } catch (BadPaddingException ex) {
            throw new IOException(ex);
        } finally {
            Util.closeQuietly(fout);
        }
    }

    /**
     * Get the patch from the Internet.
     * This will check the exist file in the path of {@code saveToFile} and determine resume download.
     * @param listener the progress listener
     * @param url the URL to download the patch from
     * @param saveToFile the place to save the downloaded patch
     * @param fileSHA256 the SHA-256 digest of the patch
     * @param expectedLength the expected file length of the patch
     * @return the get patch result
     * @throws MalformedURLException {@code url} is not a valid HTTP URL
     */
    public static DownloadResult getPatch(final DownloadProgressListener listener, String url, File saveToFile, String fileSHA256, int expectedLength) throws MalformedURLException {
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
        if (expectedLength <= 0) {
            throw new IllegalArgumentException("argument 'expectedLength' should greater than 0");
        }

        FileOutputStream fout = null;
        try {
            HTTPDownloader downloader = new HTTPDownloader();
            downloader.setResumeFile(saveToFile);
            return downloader.download(listener, new URL(url), fileSHA256, expectedLength, 0, 0);
        } finally {
            Util.closeQuietly(fout);
        }
    }
}
