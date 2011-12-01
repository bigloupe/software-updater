package updater.downloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.BadPaddingException;
import updater.script.Catalog;
import updater.script.Client;
import updater.script.InvalidFormatException;
import updater.script.Patch;
import updater.script.Patch.Operation;
import updater.script.Patch.ValidationFile;
import updater.util.DownloadProgressListener;
import updater.util.DownloadProgressUtil;
import updater.util.HTTPDownloader;
import updater.util.HTTPDownloader.DownloadResult;

/**
 * Patch and catalog downloader.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchDownloader {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }

    protected PatchDownloader() {
    }

    /**
     * Download specified patches and update the client script.
     * @param listener the download patch listener listen to progress and result
     * @param clientScriptPath the file path of the client script
     * @param patches the patches to download
     * @param retryTimes total number of times to retry
     * @param retryDelay the time to delay before each retry
     * @return the download result
     * @throws InvalidFormatException the format of the client script is invalid
     * @throws MalformedURLException any one URL of patches is invalid
     * @throws IOException error occurred when reading the client script
     */
    public static DownloadPatchesResult downloadPatches(DownloadPatchesListener listener, String clientScriptPath, List<Patch> patches, int retryTimes, int retryDelay) throws InvalidFormatException, MalformedURLException, IOException {
        if (clientScriptPath == null) {
            throw new NullPointerException("argument 'clientScriptPath' cannot be null");
        }
        byte[] clientScriptData = Util.readFile(new File(clientScriptPath));
        return downloadPatches(listener, new File(clientScriptPath), Client.read(clientScriptData), patches, retryTimes, retryDelay);
    }

    /**
     * Download specified patches and update the client script.
     * @param listener the download patch listener listen to progress and result
     * @param clientScriptFile the file of the client script
     * @param clientScript the client script content/object
     * @param patches the patches to download
     * @param retryTimes total number of times to retry
     * @param retryDelay the time to delay before each retry
     * @return the download result
     * @throws MalformedURLException any one URL of patches is invalid
     */
    public static DownloadPatchesResult downloadPatches(final DownloadPatchesListener listener, File clientScriptFile, Client clientScript, List<Patch> patches, int retryTimes, int retryDelay) throws MalformedURLException {
        return downloadPatches(listener, patches, clientScript.getStoragePath(), retryTimes, retryDelay);
    }

    /**
     * Download specified patches and update the client script.
     * @param listener the download patch listener listen to progress and result
     * @param storagePath the path for storage temporary files
     * @param patches the patches to download
     * @param retryTimes total number of times to retry
     * @param retryDelay the time to delay before each retry
     * @return the download result
     * @throws MalformedURLException any one URL of patches is invalid
     */
    public static DownloadPatchesResult downloadPatches(final DownloadPatchesListener listener, List<Patch> patches, String storagePath, int retryTimes, int retryDelay) throws MalformedURLException {
        if (listener == null) {
            throw new NullPointerException("argument 'listener' cannot be null");
        }
        if (storagePath == null) {
            throw new NullPointerException("argument 'storagePath' cannot be null");
        }
        if (patches == null) {
            throw new NullPointerException("argument 'patches' cannot be null");
        }

        if (patches.isEmpty()) {
            return DownloadPatchesResult.COMPLETED;
        }

        listener.downloadPatchesProgress(0);
        listener.downloadPatchesMessage("Getting patches catalog ...");

        final AtomicInteger retryTimesRemaining = new AtomicInteger(retryTimes);

        final AtomicLong lastRefreshTime = new AtomicLong(0L);
        final AtomicInteger downloadedSizeSinceLastRefresh = new AtomicInteger(0);

        final long totalDownloadSize = calculateTotalLength(patches);
        final AtomicInteger downloadedSize = new AtomicInteger(0);

        final DownloadProgressUtil downloadProgress = new DownloadProgressUtil();
        downloadProgress.setTotalSize(totalDownloadSize);

        // download
        for (Patch update : patches) {
            final AtomicInteger patchDownloadedSize = new AtomicInteger(0);
            DownloadProgressListener getPatchListener = new DownloadProgressListener() {

                private String totalDownloadSizeString = Util.humanReadableByteCount(totalDownloadSize, false);
                private float totalDownloadSizeFloat = (float) totalDownloadSize;

                @Override
                public void byteStart(long pos) {
                    patchDownloadedSize.set((int) pos);
                    downloadProgress.setDownloadedSize(downloadedSize.get() + patchDownloadedSize.get());
                    listener.downloadPatchesProgress((int) ((float) (downloadedSize.get() + patchDownloadedSize.get()) * 100F / (float) totalDownloadSize));
                }

                @Override
                public void byteDownloaded(int numberOfBytes) {
                    downloadedSizeSinceLastRefresh.set(downloadedSizeSinceLastRefresh.get() + numberOfBytes);

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastRefreshTime.get() > 200) {
                        lastRefreshTime.set(currentTime);

                        patchDownloadedSize.set(patchDownloadedSize.get() + downloadedSizeSinceLastRefresh.get());

                        downloadProgress.feed(downloadedSizeSinceLastRefresh.get());
                        downloadedSizeSinceLastRefresh.set(0);

                        listener.downloadPatchesProgress((int) ((float) (downloadedSize.get() + patchDownloadedSize.get()) * 100F / totalDownloadSizeFloat));
                        // Downloading: 1.6 MiB / 240 MiB, 2.6 MiB/s, 1m 32s remaining
                        listener.downloadPatchesMessage("Downloading: "
                                + Util.humanReadableByteCount((downloadedSize.get() + patchDownloadedSize.get()), false) + " / " + totalDownloadSizeString + ", "
                                + Util.humanReadableByteCount(downloadProgress.getSpeed(), false) + "/s" + ", "
                                + Util.humanReadableTimeCount(downloadProgress.getTimeRemaining(), 3) + " remaining");
                    }
                }

                @Override
                public void byteTotal(long total) {
                }

                @Override
                public void downloadRetry(DownloadResult result) {
                    retryTimesRemaining.decrementAndGet();

                    lastRefreshTime.set(System.currentTimeMillis());
                    downloadProgress.feed(downloadedSizeSinceLastRefresh.get());
                    downloadedSizeSinceLastRefresh.set(0);

                    byteStart(0);
                }
            };

            File saveToFile = new File(storagePath + File.separator + update.getId() + ".patch");

            DownloadResult updateResult = getPatch(getPatchListener, update.getDownloadUrl(), saveToFile, update.getDownloadChecksum(), update.getDownloadLength(), retryTimesRemaining.get(), retryDelay);
            if (updateResult == DownloadResult.INTERRUPTED) {
                return DownloadPatchesResult.DOWNLOAD_INTERRUPTED;
            }
            if (updateResult != DownloadResult.SUCCEED) {
                return DownloadPatchesResult.ERROR;
            }

            downloadedSize.set(downloadedSize.get() + update.getDownloadLength());
            try {
                // update client script
                listener.downloadPatchesPatchDownloaded(new Patch(update.getId(),
                        update.getType(), update.getVersionFrom(), update.getVersionFromSubsequent(), update.getVersionTo(),
                        null, null, -1,
                        update.getDownloadEncryptionType(), update.getDownloadEncryptionKey(), update.getDownloadEncryptionIV(),
                        new ArrayList<Operation>(), new ArrayList<ValidationFile>()));
            } catch (IOException ex) {
                return DownloadPatchesResult.SAVE_TO_CLIENT_SCRIPT_FAIL;
            }
        }

        listener.downloadPatchesProgress(100);
        listener.downloadPatchesMessage("Finished");

        return DownloadPatchesResult.COMPLETED;
    }

    /**
     * Get the patch from the Internet.
     * This will check the exist file in the path of {@code saveToFile} and determine resume download.
     * @param listener the progress listener
     * @param url the URL to download the patch from
     * @param saveToFile the place to save the downloaded patch
     * @param fileSHA256 the SHA-256 digest of the patch
     * @param expectedLength the expected file length of the patch
     * @param retryTimes total number of times to retry
     * @param retryDelay the time to delay before each retry
     * @return the get patch result
     * @throws MalformedURLException {@code url} is not a valid HTTP URL
     */
    public static DownloadResult getPatch(final DownloadProgressListener listener, String url, File saveToFile, String fileSHA256, int expectedLength, int retryTimes, int retryDelay) throws MalformedURLException {
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
            return downloader.download(listener, new URL(url), fileSHA256, expectedLength, retryTimes, retryDelay);
        } finally {
            Util.closeQuietly(fout);
        }
    }

    /**
     * The return result used by {@link #downloadPatches(updater.downloader.PatchDownloader.DownloadPatchesListener, java.io.File, updater.script.Client, java.util.List, int, int)}.
     */
    public static enum DownloadPatchesResult {

        SAVE_TO_CLIENT_SCRIPT_FAIL, DOWNLOAD_INTERRUPTED, ERROR, COMPLETED
    }

    /**
     * Determine the suitable patches to download to upgrade the current version of software to highest possible version with least download size.
     * @param catalog the patches catalog
     * @param currentVersion the current software version
     * @param acceptOnlyFullPack true to accept only full pack patches, false to accept both patch (diff/partly) and full pack patches.
     * @return the list of suitable patches
     */
    public static List<Patch> getSuitablePatches(Catalog catalog, String currentVersion, boolean acceptOnlyFullPack) {
        return getSuitablePatches(catalog.getPatchs(), currentVersion, acceptOnlyFullPack);
    }

    /**
     * Determine the suitable patches to download to upgrade the software with version <code>fromVersion</code> to highest possible version with least download size.
     * @param allPatches all available patches to choose from
     * @param fromVersion the starting version to match
     * @param acceptOnlyFullPack true to accept only full pack patches, false to accept both patch (diff/partly) and full pack patches.
     * @return the list of suitable patches
     */
    public static List<Patch> getSuitablePatches(List<Patch> allPatches, String fromVersion, boolean acceptOnlyFullPack) {
        if (allPatches == null) {
            throw new NullPointerException("argument 'allPatches' cannot be null");
        }
        if (fromVersion == null) {
            throw new NullPointerException("argument 'fromVersion' cannot be null");
        }
        List<Patch> returnResult = new ArrayList<Patch>();

        String maxVersion = fromVersion;
        for (Patch patch : allPatches) {
            if (acceptOnlyFullPack && (patch.getType() == null || !patch.getType().equals("full"))) {
                continue;
            }

            if ((patch.getVersionFrom() != null && patch.getVersionFrom().equals(fromVersion))
                    || (patch.getVersionFromSubsequent() != null && Util.compareVersion(fromVersion, patch.getVersionFromSubsequent()) >= 0 && Util.compareVersion(patch.getVersionTo(), fromVersion) > 0)) {
                List<Patch> tempResult = new ArrayList<Patch>();

                tempResult.add(patch);

                List<Patch> _allUpdates = getSuitablePatches(allPatches, patch.getVersionTo(), acceptOnlyFullPack);
                if (!_allUpdates.isEmpty()) {
                    tempResult.addAll(_allUpdates);
                }

                Patch _maxUpdateThisRound = tempResult.get(tempResult.size() - 1);
                long compareResult = Util.compareVersion(_maxUpdateThisRound.getVersionTo(), maxVersion);
                if (compareResult > 0) {
                    maxVersion = _maxUpdateThisRound.getVersionTo();
                    returnResult = tempResult;
                } else if (compareResult == 0) {
                    long tempResultCost = calculateTotalLength(tempResult);
                    long returnResultCost = calculateTotalLength(returnResult);
                    if (tempResultCost < returnResultCost
                            || (tempResultCost == returnResultCost && tempResult.size() < returnResult.size())) {
                        returnResult = tempResult;
                    }
                }
            }
        }

        return returnResult;
    }

    /**
     * Calculate the summation of size of all patches in <code>allPatches</code>.
     * @param allPatches the patches list
     * @return the summation of size of all patches
     */
    protected static long calculateTotalLength(List<Patch> allPatches) {
        if (allPatches == null) {
            throw new NullPointerException("argument 'allPatches' cannot be null");
        }
        long returnResult = 0;
        for (Patch patch : allPatches) {
            returnResult += patch.getDownloadLength();
        }
        return returnResult;
    }

    /**
     * Get the updated catalog.
     * @param clientScriptPath the path of the file of the client script
     * @return the catalog, null means no newer version of catalog is available
     * @throws IOException error occurred when reading the client script
     * @throws InvalidFormatException the format of the client script is invalid
     */
    public static Catalog getUpdatedCatalog(String clientScriptPath) throws IOException, InvalidFormatException {
        if (clientScriptPath == null) {
            throw new NullPointerException("argument 'clientScriptPath' cannot be null");
        }
        byte[] clientScriptData = Util.readFile(new File(clientScriptPath));
        return getUpdatedCatalog(Client.read(clientScriptData));
    }

    /**
     * Get the updated catalog.
     * @param client the path of the file of the client script
     * @return the catalog, null means no newer version of catalog is available
     * @throws IOException RSA key invalid or error occurred when getting the catalog
     * @throws InvalidFormatException the format of the downloaded catalog is invalid
     */
    public static Catalog getUpdatedCatalog(Client client) throws IOException, InvalidFormatException {
        if (client == null) {
            throw new NullPointerException("argument 'client' cannot be null");
        }

        String catalogURL = client.getCatalogUrl();

        RSAPublicKey publicKey = null;
        int keyLength = 0;
        if (client.getCatalogPublicKeyModulus() != null) {
            try {
                publicKey = Util.getPublicKey(new BigInteger(client.getCatalogPublicKeyModulus(), 16), new BigInteger(client.getCatalogPublicKeyExponent(), 16));
                keyLength = new BigInteger(client.getCatalogPublicKeyModulus(), 16).bitLength() / 8;
            } catch (InvalidKeySpecException ex) {
                throw new IOException("RSA key invalid: " + ex.getMessage());
            }
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DownloadResult getCatalogResult = getCatalog(bout, catalogURL, client.getCatalogLastUpdated(), publicKey, keyLength);
        if (getCatalogResult == DownloadResult.FILE_NOT_MODIFIED) {
            return null;
        }
        if (getCatalogResult != DownloadResult.SUCCEED) {
            throw new IOException("Error occurred when getting the catalog.");
        }

        return Catalog.read(bout.toByteArray());
    }

    /**
     * Get the catalog from Internet.
     * @param out the stream to output the catalog data to
     * @param url the URL to download the catalog from
     * @param lastUpdateDate the last update date, if the catalog not be updated since this date, the content of the catalog will not be downloaded (save time and traffic); -1 means not specified
     * @param key the RSA key to decrypt the catalog, null means no encryption
     * @param keyLength if {@code key} specified, provide the key length of the RSA key in byte
     * @return the get catalog result
     * @throws MalformedURLException {@code url} is not a valid HTTP URL
     * @throws IOException catalog content invalid
     */
    public static DownloadResult getCatalog(OutputStream out, String url, long lastUpdateDate, RSAPublicKey key, int keyLength) throws MalformedURLException, IOException {
        if (out == null) {
            throw new NullPointerException("argument 'out' cannot be null");
        }
        if (url == null) {
            throw new NullPointerException("argument 'url' cannot be null");
        }
        if (lastUpdateDate < -1) {
            throw new IllegalArgumentException("argument 'lastUpdateDate' smaller than -1");
        }
        if (key != null && keyLength < 1) {
            throw new IllegalArgumentException("argument 'keyLength' invalid, should >= 1");
        }

        FileOutputStream fout = null;
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            HTTPDownloader downloader = new HTTPDownloader();
            downloader.setOutputTo(bout);
            if (lastUpdateDate != -1) {
                downloader.setIfModifiedSince(lastUpdateDate);
            }

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
}
