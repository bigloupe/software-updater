package updater.downloader;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import updater.downloader.PatchDownloader.DownloadPatchesListener.DownloadPatchesResult;
import updater.downloader.RemoteContent.GetCatalogResult;
import updater.downloader.RemoteContent.GetPatchListener;
import updater.downloader.RemoteContent.GetPatchResult;
import updater.downloader.RemoteContent.RSAPublicKey;
import updater.gui.UpdaterWindow;
import updater.script.Catalog;
import updater.script.Client;
import updater.script.Client.Information;
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

    public static void checkForUpdates(String clientScriptPath) throws InvalidFormatException, IOException {
        byte[] clientScriptData = Util.readFile(new File(clientScriptPath));
        if (clientScriptData == null) {
            throw new IOException("Failed to read the data in the client script file path specified.");
        }

        checkForUpdates(new File(clientScriptPath), Client.read(clientScriptData));
    }

    public static void checkForUpdates(final File clientScriptFile, final Client clientScript) {
        Information clientInfo = clientScript.getInformation();

        Image softwareIcon = null;
        Image updaterIcon = null;
        //<editor-fold defaultstate="collapsed" desc="icons">
        try {
            if (clientInfo.getSoftwareIconLocation() != null) {
                if (clientInfo.getSoftwareIconLocation().equals("jar")) {
                    URL resourceURL = PatchDownloader.class.getResource(clientInfo.getSoftwareIconPath());
                    if (resourceURL != null) {
                        softwareIcon = Toolkit.getDefaultToolkit().getImage(resourceURL);
                    } else {
                        throw new IOException("Resource not found: " + clientInfo.getSoftwareIconPath());
                    }
                } else {
                    softwareIcon = ImageIO.read(new File(clientInfo.getSoftwareIconPath()));
                }
            }
            if (clientInfo.getDownloaderIconLocation() != null) {
                if (clientInfo.getDownloaderIconLocation().equals("jar")) {
                    URL resourceURL = PatchDownloader.class.getResource(clientInfo.getDownloaderIconPath());
                    if (resourceURL != null) {
                        updaterIcon = Toolkit.getDefaultToolkit().getImage(resourceURL);
                    } else {
                        throw new IOException("Resource not found: " + clientInfo.getDownloaderIconPath());
                    }
                } else {
                    updaterIcon = ImageIO.read(new File(clientInfo.getDownloaderIconPath()));
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(PatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Fail to read images stated in the config file: root->information->software->icon or root->information->downloader->icon.");
            return;
        }
        //</editor-fold>

        final Thread currentThread = Thread.currentThread();
        final UpdaterWindow updaterGUI = new UpdaterWindow(clientInfo.getSoftwareName(), softwareIcon, clientInfo.getDownloaderName(), updaterIcon);
        updaterGUI.addListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // user press cancel
                updaterGUI.setCancelEnabled(false);
                currentThread.interrupt();
            }
        });
        updaterGUI.setProgress(0);
        updaterGUI.setMessage("Getting patches catalog ...");
        final JFrame updaterFrame = updaterGUI.getGUI();
        updaterFrame.setVisible(true);

        Catalog catalog = null;
        try {
            catalog = getUpdatedCatalog(clientScript);
            if (catalog == null) {
                JOptionPane.showMessageDialog(updaterFrame, "There are no updates available.");
                disposeWindow(updaterFrame);
                return;
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(updaterFrame, "Error occurred when getting the patches catalog.");
            Logger.getLogger(PatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
            disposeWindow(updaterFrame);
            return;
        }

        List<Patch> updatePatches = getSuitablePatches(catalog, clientScript.getVersion());
        if (updatePatches.isEmpty()) {
            JOptionPane.showMessageDialog(updaterFrame, "There are no updates available.");

            clientScript.setCatalogLastUpdated(System.currentTimeMillis());
            try {
                Util.saveClientScript(clientScriptFile, clientScript);
            } catch (Exception ex) {
                // not a fatal problem
                Logger.getLogger(PatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
            }

            disposeWindow(updaterFrame);
            return;
        }

        downloadPatches(new DownloadPatchesListener() {

            @Override
            public void downloadPatchesResult(DownloadPatchesResult result) {
                switch (result) {
                    case ACQUIRE_LOCK_FAILED:
                        break;
                    case MULTIPLE_UPDATER_RUNNING:
                        JOptionPane.showMessageDialog(updaterFrame, "There is another updater running.");
                        disposeWindow(updaterFrame);
                        break;
                    case PATCHES_EXIST:
                        JOptionPane.showMessageDialog(updaterFrame, "You have to restart the application to make the update take effect.");
                        disposeWindow(updaterFrame);
                        break;
                    case DOWNLOAD_INTERRUPTED:
                        disposeWindow(updaterFrame);
                        break;
                    case ERROR:
                    case SAVE_TO_CLIENT_SCRIPT_FAIL:
                        JOptionPane.showMessageDialog(updaterFrame, "Error occurred when getting the update patch.");
                        disposeWindow(updaterFrame);
                        break;
                    case COMPLETED:
                        JOptionPane.showMessageDialog(updaterFrame, "Download patches finished.");
                        JOptionPane.showMessageDialog(updaterFrame, "You have to restart the application to make the update take effect.");

                        clientScript.setCatalogLastUpdated(System.currentTimeMillis());
                        try {
                            Util.saveClientScript(clientScriptFile, clientScript);
                        } catch (Exception ex) {
                            // not a fatal problem
                            Logger.getLogger(PatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        disposeWindow(updaterFrame);
                        break;
                }
            }

            @Override
            public void downloadPatchesProgress(int progress) {
                updaterGUI.setProgress(progress);
            }

            @Override
            public void downloadPatchesMessage(String message) {
                updaterGUI.setMessage(message);
            }
        }, clientScriptFile, clientScript, updatePatches);
    }

    protected static void disposeWindow(JFrame frame) {
        if (frame != null) {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    public static void downloadPatches(DownloadPatchesListener listener, String clientScriptPath, List<Patch> patches) throws InvalidFormatException, IOException {
        byte[] clientScriptData = Util.readFile(new File(clientScriptPath));
        if (clientScriptData == null) {
            throw new IOException("Failed to read the data in the client script file path specified.");
        }

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
            try {
                if (lock != null) {
                    lock.release();
                }
                if (lockFileOut != null) {
                    lockFileOut.close();
                }
            } catch (IOException ex1) {
                if (debug) {
                    Logger.getLogger(PatchDownloader.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
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
                public boolean downloadInterrupted() {
                    listener.downloadPatchesResult(DownloadPatchesListener.DownloadPatchesResult.DOWNLOAD_INTERRUPTED);
                    return true;
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
                    if (currentTime - lastRefreshTime.get() > 10) {
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

            // update storage path
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

                existingUpdates.add(new Patch(update.getId(),
                        update.getVersionFrom(), update.getVersionFromSubsequent(), update.getVersionTo(),
                        null, null, -1,
                        null, null, null,
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
            try {
                if (lock != null) {
                    lock.release();
                }
                if (lockFileOut != null) {
                    lockFileOut.close();
                }
            } catch (IOException ex) {
                if (debug) {
                    Logger.getLogger(PatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
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
        if (clientScriptData == null) {
            throw new IOException("Failed to read the data in the client script file path specified.");
        }

        return getUpdatedCatalog(Client.read(clientScriptData));
    }

    protected static Catalog getUpdatedCatalog(Client client) throws IOException {
        String catalogURL = client.getCatalogUrl();

        RSAPublicKey publicKey = null;
        if (client.getCatalogPublicKeyModulus() != null) {
            publicKey = new RSAPublicKey(new BigInteger(client.getCatalogPublicKeyModulus(), 16), new BigInteger(client.getCatalogPublicKeyExponent(), 16));
        }

        GetCatalogResult getCatalogResult = RemoteContent.getCatalog(catalogURL, client.getCatalogLastUpdated(), publicKey);
        if (getCatalogResult.isNotModified()) {
            return null;
        }
        if (getCatalogResult.getCatalog() == null) {
            throw new IOException("Error occurred when getting the catalog.");
        }
        return getCatalogResult.getCatalog();
    }
}
