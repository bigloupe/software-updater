package updater.launcher;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import updater.crypto.AESKey;
import updater.gui.UpdaterWindow;
import updater.patch.PatchLogReader;
import updater.patch.PatchLogReader.UnfinishedPatch;
import updater.patch.PatchLogWriter;
import updater.patch.Patcher;
import updater.patch.Patcher.Replacement;
import updater.patch.PatcherListener;
import updater.script.Client;
import updater.script.Patch;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class BatchPatcher {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }

    protected BatchPatcher() {
    }

    protected static int getPatchStartIndex(Patch update, PatchLogReader patchLogReader) {
        if (patchLogReader == null) {
            return 0;
        }
        UnfinishedPatch unfinishedPatch = patchLogReader.getUnfinishedPatch();
        if (unfinishedPatch != null && unfinishedPatch.getPatchId() == update.getId()) {
            return unfinishedPatch.getFileIndex();
        }
        return 0;
    }

    public static UpdateResult update(File clientScriptFile, Client clientScript, File tempDir, String windowTitle, Image windowIcon, String title, Image icon) {
        Map<String, Replacement> replacementMap = new HashMap<String, Replacement>();
        UpdateResult returnResult = new UpdateResult(false, false, new ArrayList<Replacement>());

        final AtomicReference<Patcher> patcherRef = new AtomicReference<Patcher>();

        List<Patch> patches = clientScript.getPatches();
        if (patches.isEmpty()) {
            return new UpdateResult(true, true, new ArrayList<Replacement>());
        }

        // action log
        File logFile = new File(tempDir + "/action.log");

        // GUI
        final Thread currentThread = Thread.currentThread();
        final UpdaterWindow updaterGUI = new UpdaterWindow(windowTitle, windowIcon, title, icon);
        updaterGUI.addListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Patcher _patcher = patcherRef.get();
                if (_patcher == null) {
                    updaterGUI.setCancelEnabled(false);
                    currentThread.interrupt();
                } else {
                    _patcher.pause(true);

                    Object[] options = {"Yes", "No"};
                    int result = JOptionPane.showOptionDialog(null, "Are you sure to cancel update?", "Canel Update", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
                    if (result == 0) {
                        updaterGUI.setCancelEnabled(false);
                        currentThread.interrupt();
                    }

                    _patcher.pause(false);
                }
            }
        });
        updaterGUI.setProgress(0);
        updaterGUI.setMessage("Preparing ...");
        JFrame updaterFrame = updaterGUI.getGUI();
        updaterFrame.setVisible(true);

        // patch
        FileOutputStream lockFileOut = null;
        FileLock lock = null;
        PatchLogWriter patchActionLogWriter = null;
        try {
            PatchLogReader patchLogReader = null;
            //<editor-fold defaultstate="collapsed" desc="read log">
            try {
                patchLogReader = new PatchLogReader(logFile);
            } catch (IOException ex) {
                // ignore
            }
            if (patchLogReader != null) {
                boolean rewriteClientXML = false;

                List<Integer> finishedPatches = patchLogReader.getfinishedPatches();
                for (Integer finishedPatch : finishedPatches) {
                    Iterator<Patch> iterator = patches.iterator();
                    while (iterator.hasNext()) {
                        Patch _patch = iterator.next();
                        if (_patch.getId() == finishedPatch) {
                            rewriteClientXML = true;
                            iterator.remove();
                        }
                    }
                }

                if (rewriteClientXML) {
                    clientScript.setPatches(patches);
                    Util.saveClientScript(clientScriptFile, clientScript);
                }

                if (patches.isEmpty()) {
                    return new UpdateResult(true, true, new ArrayList<Replacement>());
                }
            }
            //</editor-fold>

            updaterGUI.setProgress(1);
            updaterGUI.setMessage("Check to see if there is another updater running ...");
            // acquire lock
            lockFileOut = new FileOutputStream(tempDir + "/update.lck");
            lock = lockFileOut.getChannel().tryLock();
            if (lock == null) {
                throw new IOException("There is another updater running.");
            }

            updaterGUI.setProgress(2);
            updaterGUI.setMessage("Clear log ...");
            // truncate log file
            new FileOutputStream(logFile).close();
            // open log file
            patchActionLogWriter = new PatchLogWriter(logFile);

            updaterGUI.setProgress(3);
            updaterGUI.setMessage("Starting ...");
            // iterate patches and do patch
            final float stepSize = 97F / (float) patches.size();
            int count = -1;
            Iterator<Patch> iterator = patches.iterator();
            while (iterator.hasNext()) {
                count++;
                Patch _update = iterator.next();

                // check if the update if not suitable
                if (Util.compareVersion(clientScript.getVersion(), _update.getVersionFrom()) > 0) {
                    // normally should not reach here
                    iterator.remove();
                    // save the client scirpt
                    clientScript.setPatches(patches);
                    Util.saveClientScript(clientScriptFile, clientScript);
                    continue;
                }
                if (!_update.getVersionFrom().equals(clientScript.getVersion())) {
                    // the 'version from' of this update dun match with current version
                    // normally should not happen
                    continue;
                }

                // temporary storage folder for this patch
                File tempDirForPatch = new File(tempDir.getAbsolutePath() + "/" + _update.getId());
                if (!tempDirForPatch.isDirectory() && !tempDirForPatch.mkdirs()) {
                    throw new IOException("Failed to create folder for patches.");
                }

                File patchFile = new File(tempDir.getAbsolutePath() + File.separator + _update.getId() + ".patch");
                File decryptedPatchFile = new File(tempDir.getAbsolutePath() + File.separator + _update.getId() + ".patch.decrypted");
                decryptedPatchFile.deleteOnExit();
                if (!patchFile.exists()) {
                    // if the patch not exist, remove all patches
                    // save the client scirpt
                    clientScript.setPatches(new ArrayList<Patch>());
                    Util.saveClientScript(clientScriptFile, clientScript);
                    throw new IOException("Patch file not found: " + patchFile.getAbsolutePath());
                }

                // patch
                AESKey aesKey = null;
                if (_update.getDownloadEncryptionKey() != null) {
                    aesKey = new AESKey(Util.hexStringToByteArray(_update.getDownloadEncryptionKey()), Util.hexStringToByteArray(_update.getDownloadEncryptionIV()));
                }

                // initialize patcher
                final int _count = count;
                Patcher _patcher = new Patcher(new PatcherListener() {

                    @Override
                    public void patchProgress(int percentage, String message) {
                        float base = 3F + (stepSize * (float) _count);
                        float addition = ((float) percentage / 100F) * stepSize;
                        updaterGUI.setProgress((int) (base + addition));
                        updaterGUI.setMessage(message);
                    }

                    @Override
                    public void patchFinished() {
                    }

                    @Override
                    public void patchEnableCancel(boolean enable) {
                        updaterGUI.setCancelEnabled(enable);
                    }
                }, patchActionLogWriter, new File("." + File.separator), tempDirForPatch);
                patcherRef.set(_patcher);
                List<Replacement> replacementList = _patcher.doPatch(patchFile, _update.getId(), getPatchStartIndex(_update, patchLogReader), aesKey, decryptedPatchFile);
                for (Replacement _replacement : replacementList) {
                    replacementMap.put(_replacement.getDestination(), _replacement);
                }
                patcherRef.set(null);

                // remove 'update' from updates list
                iterator.remove();

                // save the client scirpt
                clientScript.setVersion(_update.getVersionTo());
                clientScript.setPatches(patches);
                Util.saveClientScript(clientScriptFile, clientScript);

//                Util.truncateFolder(tempDirForPatch);
//                tempDirForPatch.delete();

                patchFile.delete();
            }

            List<Replacement> replacementList = new ArrayList<Replacement>();
            for (Replacement _replacement : replacementMap.values()) {
                replacementList.add(_replacement);
            }

            returnResult = new UpdateResult(true, true, replacementList);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(updaterFrame, "Error occurred when updating the software.");

            boolean launchSoftware = false;

            if (updaterGUI.isCancelEnabled()) {
                Object[] options = {"Launch", "Exit"};
                int result = JOptionPane.showOptionDialog(updaterFrame, "Continue to launch the software?", "Continue Action", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (result == 0) {
                    launchSoftware = true;
                } else {
                    JOptionPane.showMessageDialog(updaterFrame, "You can restart the software to try to update software again.");
                }
            }

            returnResult = new UpdateResult(false, launchSoftware, new ArrayList<Replacement>());

            if (debug) {
                Logger.getLogger(BatchPatcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
            updaterFrame.setVisible(false);
            updaterFrame.dispose();
            if (lock != null) {
                try {
                    lock.release();
                } catch (IOException ex) {
                    if (debug) {
                        Logger.getLogger(BatchPatcher.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            Util.closeQuietly(lockFileOut);
            Util.closeQuietly(patchActionLogWriter);
            if (returnResult.isUpdateSucceed()) {
                // remove log
                logFile.delete();
            }
        }

        return returnResult;
    }

    public static class UpdateResult {

        protected boolean updateSucceed;
        protected boolean launchSoftware;
        protected List<Replacement> replacementList;

        public UpdateResult(boolean updateSucceed, boolean launchSoftware, List<Replacement> replacementList) {
            this.updateSucceed = updateSucceed;
            this.launchSoftware = launchSoftware;
            this.replacementList = replacementList == null ? new ArrayList<Replacement>() : new ArrayList<Replacement>(replacementList);
        }

        public boolean isUpdateSucceed() {
            return updateSucceed;
        }

        public boolean isLaunchSoftware() {
            return launchSoftware;
        }

        public List<Replacement> getReplacementList() {
            return new ArrayList<Replacement>(replacementList);
        }
    }
}
