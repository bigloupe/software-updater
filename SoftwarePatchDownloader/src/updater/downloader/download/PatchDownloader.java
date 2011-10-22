package updater.downloader.download;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import updater.downloader.SoftwareUpdater;
import updater.downloader.download.RemoteContent.GetCatalogResult;
import updater.downloader.download.RemoteContent.GetPatchListener;
import updater.downloader.download.RemoteContent.GetPatchResult;
import updater.downloader.download.RemoteContent.RSAPublicKey;
import updater.gui.UpdaterWindow;
import updater.script.Catalog;
import updater.script.Client;
import updater.script.Client.Information;
import updater.script.InvalidFormatException;
import updater.downloader.util.DownloadProgessUtil;
import updater.downloader.util.Util;
import updater.script.Patch;
import updater.script.Patch.Operation;
import updater.script.Patch.ValidationFile;
import updater.util.CommonUtil.ObjectReference;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchDownloader {

    protected PatchDownloader() {
    }

    public static void checkForUpdates(String clientScriptPath) throws InvalidFormatException, IOException {
        byte[] clientScriptData = Util.readFile(new File(clientScriptPath));
        if (clientScriptData != null) {
            throw new IOException("Failed to read the data in the client script file path specified.");
        }

        checkForUpdates(new File(clientScriptPath), Client.read(clientScriptData));
    }

    public static void checkForUpdates(File clientScriptFile, Client clientScript) throws InvalidFormatException, IOException {
        Information clientInfo = clientScript.getInformation();

        // acquire lock
        FileOutputStream lockFileOut = new FileOutputStream(clientScript.getStoragePath() + "/update.lck");
        FileLock lock = lockFileOut.getChannel().tryLock();
        if (lock == null) {
            throw new IOException("There is another updater running.");
        }

        Image softwareIcon;
        if (clientInfo.getSoftwareIconLocation().equals("jar")) {
            URL resourceURL = SoftwareUpdater.class.getResource(clientInfo.getSoftwareIconPath());
            if (resourceURL != null) {
                softwareIcon = Toolkit.getDefaultToolkit().getImage(resourceURL);
            } else {
                throw new IOException("Resource not found: " + clientInfo.getSoftwareIconPath());
            }
        } else {
            softwareIcon = ImageIO.read(new File(clientInfo.getSoftwareIconPath()));
        }
        Image updaterIcon;
        if (clientInfo.getDownloaderIconLocation().equals("jar")) {
            URL resourceURL = SoftwareUpdater.class.getResource(clientInfo.getDownloaderIconPath());
            if (resourceURL != null) {
                updaterIcon = Toolkit.getDefaultToolkit().getImage(resourceURL);
            } else {
                throw new IOException("Resource not found: " + clientInfo.getDownloaderIconPath());
            }
        } else {
            updaterIcon = ImageIO.read(new File(clientInfo.getDownloaderIconPath()));
        }

        final Thread currentThread = Thread.currentThread();
        final UpdaterWindow updaterGUI = new UpdaterWindow(clientInfo.getSoftwareName(), softwareIcon, clientInfo.getDownloaderName(), updaterIcon);
        updaterGUI.addListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // user press cancel
                updaterGUI.setEnableCancel(false);
                currentThread.interrupt();
            }
        });
        updaterGUI.setProgress(0);
        updaterGUI.setMessage("Getting patches catalog ...");
        final JFrame updaterFrame = updaterGUI.getGUI();
        updaterFrame.setVisible(true);

        if (!clientScript.getPatches().isEmpty()) {
            JOptionPane.showMessageDialog(updaterFrame, "You have to restart the application to make the update take effect.");
            disposeWindow(updaterFrame);
            return;
        }

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
            Logger.getLogger(SoftwareUpdater.class.getName()).log(Level.SEVERE, null, ex);
            disposeWindow(updaterFrame);
            return;
        }

        List<Patch> updatePatches = getPatches(catalog, clientScript.getVersion());
        if (updatePatches.isEmpty()) {
            JOptionPane.showMessageDialog(updaterFrame, "There are no updates available.");
            disposeWindow(updaterFrame);
            return;
        }

        final ObjectReference<Integer> downloadedSize = new ObjectReference<Integer>(0);
        final ObjectReference<Long> lastRefreshTime = new ObjectReference<Long>(0L);
        final ObjectReference<Integer> downloadedSizeSinceLastRefresh = new ObjectReference<Integer>(0);
        final long totalDownloadSize = calculateTotalLength(updatePatches);
        final DownloadProgessUtil downloadProgress = new DownloadProgessUtil();
        downloadProgress.setTotalSize(totalDownloadSize);
        GetPatchListener listener = new GetPatchListener() {

            @Override
            public boolean downloadInterrupted() {
                Object[] options = {"Yes", "No"};
                int result = JOptionPane.showOptionDialog(updaterFrame, "Are you sure to cancel download?", "Cancel Download", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
                return result == 0;
            }

            @Override
            public void byteStart(long pos) {
                downloadedSize.setObj(downloadedSize.getObj() + (int) pos);
                downloadProgress.setDownloadedSize(downloadProgress.getDownloadedSize() + pos);
                updaterGUI.setProgress((int) ((float) downloadedSize.getObj() * 100F / (float) totalDownloadSize));
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                downloadedSizeSinceLastRefresh.setObj(downloadedSizeSinceLastRefresh.getObj() + numberOfBytes);

//                // for test purpose - slow down the download speed (from localhost) to observe or do operation
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(SoftwareUpdater.class.getName()).log(Level.SEVERE, null, ex);
//                }

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRefreshTime.getObj() > 10) {
                    lastRefreshTime.setObj(currentTime);

                    downloadedSize.setObj(downloadedSize.getObj() + downloadedSizeSinceLastRefresh.getObj());
                    downloadProgress.feed(downloadedSizeSinceLastRefresh.getObj());
                    downloadedSizeSinceLastRefresh.setObj(0);
                    updaterGUI.setProgress((int) ((float) downloadedSize.getObj() * 100F / (float) totalDownloadSize));
                    // Downloading: 1.6 MiB / 240 MiB, 2.6 MiB/s, 1m 32s remaining
                    updaterGUI.setMessage("Downloading: "
                            + Util.humanReadableByteCount(downloadedSize.getObj(), false) + " / " + Util.humanReadableByteCount(totalDownloadSize, false) + ", "
                            + Util.humanReadableByteCount(downloadProgress.getSpeed(), false) + "/s" + ", "
                            + Util.humanReadableTimeCount(downloadProgress.getTimeRemaining(), 3) + " remaining");
                }
            }
        };

        List<Patch> existingUpdates = clientScript.getPatches();

        // update storage path
        for (Patch update : updatePatches) {
            File saveToFile = new File(clientScript.getStoragePath() + File.separator + update.getId() + ".patch");
            long saveToFileLength = saveToFile.length();
            GetPatchResult updateResult = RemoteContent.getPatch(listener, update.getDownloadUrl(), saveToFile, update.getDownloadChecksum(), update.getDownloadLength());
            if (!updateResult.isInterrupted() && !updateResult.getResult() && saveToFileLength != 0) {
                // if download failed and saveToFile is not empty, delete it and download again
                if (saveToFile.exists() && saveToFile.length() > saveToFileLength) {
                    downloadedSize.setObj(downloadedSize.getObj() - (int) (saveToFile.length() - saveToFileLength));
                }
                saveToFile.delete();
                updateResult = RemoteContent.getPatch(listener, update.getDownloadUrl(), saveToFile, update.getDownloadChecksum(), update.getDownloadLength());
            }
            if (updateResult.isInterrupted() || !updateResult.getResult()) {
                if (!updateResult.isInterrupted()) {
                    JOptionPane.showMessageDialog(updaterFrame, "Error occurred when getting the update patch.");
                }
                disposeWindow(updaterFrame);
                return;
            }

            existingUpdates.add(new Patch(update.getId(),
                    update.getVersionFrom(), null, update.getVersionTo(),
                    saveToFile.getAbsolutePath(), update.getDownloadChecksum(), update.getDownloadLength(),
                    update.getDownloadEncryptionType(), update.getDownloadEncryptionKey(), update.getDownloadEncryptionIV(),
                    new ArrayList<Operation>(), new ArrayList<ValidationFile>()));
            clientScript.setPatches(existingUpdates);
            Util.saveClientScript(clientScriptFile, clientScript);
        }

        updaterGUI.setProgress(100);
        updaterGUI.setMessage("Finished");
        JOptionPane.showMessageDialog(updaterFrame, "Download patches finished.");
        JOptionPane.showMessageDialog(updaterFrame, "You have to restart the application to make the update take effect.");
        disposeWindow(updaterFrame);
    }

    protected static void disposeWindow(JFrame frame) {
        if (frame != null) {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    protected static List<Patch> getPatches(Catalog catalog, String currentVersion) {
        return getPatches(catalog.getPatchs(), currentVersion);
    }

    protected static List<Patch> getPatches(List<Patch> allPatches, String fromVersion) {
        List<Patch> returnResult = new ArrayList<Patch>();

        String maxVersion = fromVersion;
        for (Patch patch : allPatches) {
            if (patch.getVersionFrom().equals(fromVersion)) {
                List<Patch> tempResult = new ArrayList<Patch>();

                tempResult.add(patch);

                List<Patch> _allUpdates = getPatches(allPatches, patch.getVersionTo());
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
        if (clientScriptData != null) {
            throw new IOException("Failed to read the data in the client script file path specified.");
        }

        return getUpdatedCatalog(Client.read(clientScriptData));
    }

    protected static Catalog getUpdatedCatalog(Client client) throws IOException {
        String catalogURL = client.getCatalogUrl();

        RSAPublicKey publicKey = null;
        if (client.getCatalogPublicKeyModulus() != null) {
            publicKey = new RSAPublicKey(new BigInteger(client.getCatalogPublicKeyModulus()), new BigInteger(client.getCatalogPublicKeyExponent(), 16));
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
