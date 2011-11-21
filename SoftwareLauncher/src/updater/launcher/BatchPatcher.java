package updater.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import updater.crypto.AESKey;
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
    protected Patcher patcher;

    public BatchPatcher() {
    }

    /**
     * Get the starting file index of the <code>patch</code> from the <code>patchLogReader</code>.
     * If last time the update failed (during doReplacement), the updater will start from that failed position and resume, that position is this 'patch start index'.
     * @param patch the patch
     * @param patchLogReader the log to read
     * @return the index, start from 0
     */
    protected static int getPatchStartIndex(Patch patch, PatchLogReader patchLogReader) {
        if (patch == null) {
            throw new NullPointerException("argument 'patch' cannot be null");
        }

        if (patchLogReader == null) {
            return 0;
        }

        UnfinishedPatch unfinishedPatch = patchLogReader.getUnfinishedPatch();
        if (unfinishedPatch != null && unfinishedPatch.getPatchId() == patch.getId()) {
            return unfinishedPatch.getFileIndex();
        }

        return 0;
    }

    public void pause(boolean pause) {
        if (patcher != null) {
            patcher.pause(pause);
        }
    }

    public UpdateResult update(File clientScriptFile, Client clientScript, File tempDir, final PatcherListener listener) throws Exception {
        if (clientScriptFile == null) {
            throw new NullPointerException("argument 'clientScriptFile' cannot be null");
        }
        if (clientScript == null) {
            throw new NullPointerException("argument 'clientScript' cannot be null");
        }
        if (tempDir == null) {
            throw new NullPointerException("argument 'tempDir' cannot be null");
        }
        if (listener == null) {
            throw new NullPointerException("argument 'listener' cannot be null");
        }

        UpdateResult returnResult = new UpdateResult(false, new ArrayList<Replacement>());

        List<Patch> patches = clientScript.getPatches();
        if (patches.isEmpty()) {
            return new UpdateResult(true, new ArrayList<Replacement>());
        }

        // action log
        File logFile = new File(tempDir + "/action.log");

        // patch
        FileOutputStream lockFileOut = null;
        FileLock lock = null;
        PatchLogWriter patchActionLogWriter = null;
        try {
            PatchLogReader patchLogReader = null;
            Map<String, Replacement> replacementMap = new HashMap<String, Replacement>(); // use map to prevent duplication
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
                    return new UpdateResult(true, new ArrayList<Replacement>());
                }
            }
            //</editor-fold>

            listener.patchProgress(1, "Check to see if there is another updater running ...");
            // acquire lock
            lockFileOut = new FileOutputStream(tempDir + "/update.lck");
            lock = lockFileOut.getChannel().tryLock();
            if (lock == null) {
                throw new IOException("There is another updater running.");
            }

            listener.patchProgress(2, "Clear log ...");
            // truncate log file
            Util.truncateFile(logFile);
            // open log file
            patchActionLogWriter = new PatchLogWriter(logFile);

            listener.patchProgress(3, "Starting ...");
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
                patcher = new Patcher(new PatcherListener() {

                    @Override
                    public void patchProgress(int percentage, String message) {
                        float base = 3F + (stepSize * (float) _count);
                        float addition = ((float) percentage / 100F) * stepSize;
                        listener.patchProgress((int) (base + addition), message);
                    }

                    @Override
                    public void patchFinished() {
                    }

                    @Override
                    public void patchEnableCancel(boolean enable) {
                        listener.patchEnableCancel(enable);
                    }
                }, patchActionLogWriter, new File("." + File.separator), tempDirForPatch);
                List<Replacement> replacementList = patcher.doPatch(patchFile, _update.getId(), getPatchStartIndex(_update, patchLogReader), aesKey, decryptedPatchFile);
                for (Replacement _replacement : replacementList) {
                    replacementMap.put(_replacement.getDestination(), _replacement);
                }
                patcher = null;

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

            returnResult = new UpdateResult(true, new ArrayList<Replacement>(replacementMap.values()));
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (IOException ex) {
                }
            }
            Util.closeQuietly(lockFileOut);
            Util.closeQuietly(patchActionLogWriter);
            if (returnResult.isUpdateSucceed()) {
                // remove log
                logFile.delete();
            }
        }

        listener.patchFinished();

        return returnResult;
    }

    /**
     * The update result for {@link #update(java.io.File, updater.script.Client, java.io.File, java.lang.String, java.awt.Image, java.lang.String, java.awt.Image)}.
     */
    public static class UpdateResult {

        /**
         * Indicate if the update succeed or not.
         */
        protected boolean updateSucceed;
        /**
         * The list of file that failed do the replace operation due to possibly file locking.
         */
        protected List<Replacement> replacementList;

        /**
         * Constructor.
         * @param updateSucceed true if the update succeed, false if not
         * @param replacementList the list of file that failed do the replace operation due to possibly file locking
         */
        public UpdateResult(boolean updateSucceed, List<Replacement> replacementList) {
            this.updateSucceed = updateSucceed;
            this.replacementList = replacementList == null ? new ArrayList<Replacement>() : new ArrayList<Replacement>(replacementList);
        }

        /**
         * Check if the update succeed or not.
         * @return true if the update succeed, false if not
         */
        public boolean isUpdateSucceed() {
            return updateSucceed;
        }

        /**
         * Get the list of file that failed do the replace operation due to possibly file locking.
         * @return the list
         */
        public List<Replacement> getReplacementList() {
            return new ArrayList<Replacement>(replacementList);
        }
    }
}
