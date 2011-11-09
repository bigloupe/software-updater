package updater.downloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.FileLock;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import updater.downloader.PatchDownloader.DownloadPatchesListener.DownloadPatchesResult;
import updater.downloader.RemoteContent.GetCatalogResult;
import updater.downloader.RemoteContent.GetPatchListener;
import updater.downloader.RemoteContent.GetPatchResult;
import updater.script.Catalog;
import updater.script.Client;
import updater.script.InvalidFormatException;
import updater.script.Patch;
import updater.script.Patch.Operation;
import updater.script.Patch.ValidationFile;
import updater.util.DownloadProgressUtil;

/**
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

    public static void downloadPatches(DownloadPatchesListener listener, String clientScriptPath, List<Patch> patches) throws InvalidFormatException, IOException {
        byte[] clientScriptData = Util.readFile(new File(clientScriptPath));
        downloadPatches(listener, new File(clientScriptPath), Client.read(clientScriptData), patches);
    }

    public static void downloadPatches(final DownloadPatchesListener listener, File clientScriptFile, Client clientScript, List<Patch> patches) {
        if (patches.isEmpty()) {
            return;
        }

        // acquire lock
        FileOutputStream lockFileOut = null;
        FileLock lock = null;
        try {
            lockFileOut = new FileOutputStream(clientScript.getStoragePath() + "/update.lck");
            lock = lockFileOut.getChannel().tryLock();
            if (lock == null) {
                throw new IOException("Acquire exclusive lock failed");
            }
        } catch (IOException ex) {
            if (debug) {
                Logger.getLogger(PatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (lock != null) {
                try {
                    lock.release();
                } catch (IOException ex1) {
                    if (debug) {
                        Logger.getLogger(PatchDownloader.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
            }
            Util.closeQuietly(lockFileOut);
            listener.downloadPatchesResult(DownloadPatchesListener.DownloadPatchesResult.MULTIPLE_UPDATER_RUNNING);
            return;
        }

        try {
            listener.downloadPatchesProgress(0);
            listener.downloadPatchesMessage("Getting patches catalog ...");

            if (!clientScript.getPatches().isEmpty()) {
                // You have to restart the application to make the update take effect.
                listener.downloadPatchesResult(DownloadPatchesListener.DownloadPatchesResult.PATCHES_EXIST);
                return;
            }

            final AtomicInteger downloadedSize = new AtomicInteger(0);
            final AtomicLong lastRefreshTime = new AtomicLong(0L);
            final AtomicInteger downloadedSizeSinceLastRefresh = new AtomicInteger(0);
            final long totalDownloadSize = calculateTotalLength(patches);
            final DownloadProgressUtil downloadProgress = new DownloadProgressUtil();
            downloadProgress.setTotalSize(totalDownloadSize);
            GetPatchListener getPatchListener = new GetPatchListener() {

                @Override
                public void downloadInterrupted() {
                    listener.downloadPatchesResult(DownloadPatchesListener.DownloadPatchesResult.DOWNLOAD_INTERRUPTED);
                }

                @Override
                public void byteStart(long pos) {
                    downloadedSize.set(downloadedSize.get() + (int) pos);
                    downloadProgress.setDownloadedSize(downloadProgress.getDownloadedSize() + pos);
                    listener.downloadPatchesProgress((int) ((float) downloadedSize.get() * 100F / (float) totalDownloadSize));
                }

                @Override
                public void byteDownloaded(int numberOfBytes) {
                    downloadedSizeSinceLastRefresh.set(downloadedSizeSinceLastRefresh.get() + numberOfBytes);

//                // for test purpose - slow down the download speed (from localhost) to observe or do operation
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(SoftwarePatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
//                }

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastRefreshTime.get() > 200) {
                        lastRefreshTime.set(currentTime);

                        downloadedSize.set(downloadedSize.get() + downloadedSizeSinceLastRefresh.get());
                        downloadProgress.feed(downloadedSizeSinceLastRefresh.get());
                        downloadedSizeSinceLastRefresh.set(0);
                        listener.downloadPatchesProgress((int) ((float) downloadedSize.get() * 100F / (float) totalDownloadSize));
                        // Downloading: 1.6 MiB / 240 MiB, 2.6 MiB/s, 1m 32s remaining
                        listener.downloadPatchesMessage("Downloading: "
                                + Util.humanReadableByteCount(downloadedSize.get(), false) + " / " + Util.humanReadableByteCount(totalDownloadSize, false) + ", "
                                + Util.humanReadableByteCount(downloadProgress.getSpeed(), false) + "/s" + ", "
                                + Util.humanReadableTimeCount(downloadProgress.getTimeRemaining(), 3) + " remaining");
                    }
                }
            };

            List<Patch> existingUpdates = clientScript.getPatches();

            // download
            for (Patch update : patches) {
                File saveToFile = new File(clientScript.getStoragePath() + File.separator + update.getId() + ".patch");
                long saveToFileLength = saveToFile.length();

                GetPatchResult updateResult = RemoteContent.getPatch(getPatchListener, update.getDownloadUrl(), saveToFile, update.getDownloadChecksum(), update.getDownloadLength());
                if (!updateResult.isInterrupted() && !updateResult.getResult() && saveToFileLength != 0) {
                    // if download failed and saveToFile is not empty, delete it and download again
                    if (saveToFile.exists() && saveToFile.length() > saveToFileLength) {
                        downloadedSize.set(downloadedSize.get() - (int) (saveToFile.length() - saveToFileLength));
                    }
                    saveToFile.delete();
                    updateResult = RemoteContent.getPatch(getPatchListener, update.getDownloadUrl(), saveToFile, update.getDownloadChecksum(), update.getDownloadLength());
                }
                if (updateResult.isInterrupted() || !updateResult.getResult()) {
                    if (!updateResult.isInterrupted()) {
                        listener.downloadPatchesResult(DownloadPatchesListener.DownloadPatchesResult.ERROR);
                    } else {
                        listener.downloadPatchesResult(DownloadPatchesListener.DownloadPatchesResult.DOWNLOAD_INTERRUPTED);
                    }
                    return;
                }

                // update client script
                existingUpdates.add(new Patch(update.getId(),
                        update.getVersionFrom(), update.getVersionFromSubsequent(), update.getVersionTo(),
                        null, null, -1,
                        update.getDownloadEncryptionType(), update.getDownloadEncryptionKey(), update.getDownloadEncryptionIV(),
                        new ArrayList<Operation>(), new ArrayList<ValidationFile>()));
                clientScript.setPatches(existingUpdates);
                try {
                    Util.saveClientScript(clientScriptFile, clientScript);
                } catch (Exception ex) {
                    if (debug) {
                        Logger.getLogger(PatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    listener.downloadPatchesResult(DownloadPatchesListener.DownloadPatchesResult.SAVE_TO_CLIENT_SCRIPT_FAIL);
                    return;
                }
            }

            listener.downloadPatchesProgress(100);
            listener.downloadPatchesMessage("Finished");

            listener.downloadPatchesResult(DownloadPatchesListener.DownloadPatchesResult.COMPLETED);
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (IOException ex) {
                    if (debug) {
                        Logger.getLogger(PatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            Util.closeQuietly(lockFileOut);
        }
    }

    public static interface DownloadPatchesListener {

        public static enum DownloadPatchesResult {

            ACQUIRE_LOCK_FAILED, MULTIPLE_UPDATER_RUNNING, PATCHES_EXIST, SAVE_TO_CLIENT_SCRIPT_FAIL, DOWNLOAD_INTERRUPTED, ERROR, COMPLETED
        }

        void downloadPatchesResult(DownloadPatchesResult result);

        void downloadPatchesProgress(int progress);

        void downloadPatchesMessage(String message);
    }

    public static List<Patch> getSuitablePatches(Catalog catalog, String currentVersion) {
        return getSuitablePatches(catalog.getPatchs(), currentVersion);
    }

    protected static List<Patch> getSuitablePatches(List<Patch> allPatches, String fromVersion) {
        List<Patch> returnResult = new ArrayList<Patch>();

        String maxVersion = fromVersion;
        for (Patch patch : allPatches) {
            if ((patch.getVersionFrom() != null && patch.getVersionFrom().equals(fromVersion))
                    || (patch.getVersionFromSubsequent() != null && Util.compareVersion(fromVersion, patch.getVersionFromSubsequent()) >= 0 && Util.compareVersion(patch.getVersionTo(), fromVersion) > 0)) {
                List<Patch> tempResult = new ArrayList<Patch>();

                tempResult.add(patch);

                List<Patch> _allUpdates = getSuitablePatches(allPatches, patch.getVersionTo());
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

    protected static long calculateTotalLength(List<Patch> allPatches) {
        long returnResult = 0;
        for (Patch patch : allPatches) {
            returnResult += patch.getDownloadLength();
        }
        return returnResult;
    }

    public static Catalog getUpdatedCatalog(String clientScriptPath) throws IOException, InvalidFormatException {
        byte[] clientScriptData = Util.readFile(new File(clientScriptPath));
        return getUpdatedCatalog(Client.read(clientScriptData));
    }

    protected static Catalog getUpdatedCatalog(Client client) throws IOException {
        String catalogURL = client.getCatalogUrl();

        RSAPublicKey publicKey = null;
        if (client.getCatalogPublicKeyModulus() != null) {
            try {
                publicKey = Util.getPublicKey(new BigInteger(client.getCatalogPublicKeyModulus(), 16), new BigInteger(client.getCatalogPublicKeyExponent(), 16));
            } catch (InvalidKeySpecException ex) {
                throw new IOException("RSA key invalid: " + ex.getMessage());
            }
        }

        GetCatalogResult getCatalogResult = RemoteContent.getCatalog(catalogURL, client.getCatalogLastUpdated(), publicKey, new BigInteger(client.getCatalogPublicKeyModulus(), 16).bitLength() / 8);
        if (getCatalogResult.isNotModified()) {
            return null;
        }
        if (getCatalogResult.getCatalog() == null) {
            throw new IOException("Error occurred when getting the catalog.");
        }

        return getCatalogResult.getCatalog();
    }
}
