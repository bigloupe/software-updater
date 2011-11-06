package updater.downloader;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import updater.downloader.PatchDownloader.DownloadPatchesListener;
import updater.downloader.PatchDownloader.DownloadPatchesListener.DownloadPatchesResult;
import updater.gui.UpdaterWindow;
import updater.script.Catalog;
import updater.script.Client;
import updater.script.Client.Information;
import updater.script.InvalidFormatException;
import updater.script.Patch;
import updater.util.CommonUtil.GetClientScriptResult;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwarePatchDownloader {

    static {
        // set debug mode
        System.setProperty("SyntaxHighlighterDebugMode", "false");
    }

    protected SoftwarePatchDownloader() {
    }

    public static void checkForUpdates(String clientScriptPath) throws InvalidFormatException, IOException {
        byte[] clientScriptData = Util.readFile(new File(clientScriptPath));
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
                Object[] options = {"Yes", "No"};
                int result = JOptionPane.showOptionDialog(null, "Are you sure to cancel download?", "Canel Download", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
                if (result == 0) {
                    updaterGUI.setCancelEnabled(false);
                    currentThread.interrupt();
                }
            }
        });
        updaterGUI.setProgress(0);
        updaterGUI.setMessage("Getting patches catalog ...");
        final JFrame updaterFrame = updaterGUI.getGUI();
        updaterFrame.setVisible(true);

        Catalog catalog = null;
        try {
            catalog = PatchDownloader.getUpdatedCatalog(clientScript);
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

        List<Patch> updatePatches = PatchDownloader.getSuitablePatches(catalog, clientScript.getVersion());
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

        PatchDownloader.downloadPatches(new DownloadPatchesListener() {

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

    public static void main(String[] args) throws IOException {
        try {
            Util.setLookAndFeel();
        } catch (Exception ex) {
            Logger.getLogger(SoftwarePatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
        }

        GetClientScriptResult result = null;
        try {
            result = Util.getClientScript(args.length > 0 ? args[0] : null);
        } catch (IOException ex) {
            Logger.getLogger(SoftwarePatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Fail to load config file.");
            return;
        } catch (InvalidFormatException ex) {
            Logger.getLogger(SoftwarePatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Config file format invalid.");
            return;
        }

        if (result.getClientScript() != null) {
            checkForUpdates(new File(result.getClientScriptPath()), result.getClientScript());
        } else {
            JOptionPane.showMessageDialog(null, "Config file not found, is empty or is invalid.");
        }
    }
}
