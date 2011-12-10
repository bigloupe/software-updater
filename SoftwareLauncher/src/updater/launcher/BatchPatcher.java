package updater.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import updater.crypto.AESKey;
import updater.patch.PatchRecord;
import updater.patch.Patcher;
import updater.patch.PatcherListener;
import updater.patch.ReplacementRecord;
import updater.script.Patch;
import updater.util.Pausable;

/**
 * Patcher that do apply patches sequentially.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class BatchPatcher implements Pausable {

  /**
   * The reference to the current-using patcher, it may change to other patcher when apply current patch finished and proceed to apply next patch.
   */
  protected Patcher patcher;

  /**
   * Constructor.
   */
  public BatchPatcher() {
  }

  /**
   * Pause or resume the patching.
   * @param pause true to pause, false to resume
   */
  @Override
  public void pause(boolean pause) {
    if (patcher != null) {
      patcher.pause(pause);
    }
  }

  /**
   * Apply patches to {@code applyToFolder}. 
   * @param listener the listener
   * @param applyToFolder the root directory of the software
   * @param tempDir temporary folder to store temporary generated files while patching
   * @param fromVersion the current version of the software
   * @param patches the patches to apply, must be in sequence
   * @return 
   * @throws IOException 
   */
  public List<PatchRecord> doPatch(final BatchPatchListener listener, File applyToFolder, File tempDir, String fromVersion, List<Patch> patches) throws IOException {
    if (listener == null) {
      throw new NullPointerException("argument 'listener' cannot be null");
    }
    if (applyToFolder == null) {
      throw new NullPointerException("argument 'applyToFolder' cannot be null");
    }
    if (tempDir == null) {
      throw new NullPointerException("argument 'tempDir' cannot be null");
    }
    if (fromVersion == null) {
      throw new NullPointerException("argument 'fromVersion' cannot be null");
    }
    if (patches == null) {
      throw new NullPointerException("argument 'patches' cannot be null");
    }
    if (!applyToFolder.isDirectory()) {
      throw new IOException("argument 'applyToFolder' is not a valid folder");
    }
    if (!tempDir.isDirectory()) {
      throw new IOException("argument 'tempDir' is not a valid folder");
    }
    List<Patch> _patches = new ArrayList<Patch>(patches);

    List<PatchRecord> returnList = new ArrayList<PatchRecord>();

    if (_patches.isEmpty()) {
      return returnList;
    }

    listener.patchProgress(1, "Starting ...");
    // iterate patches and do patch
    final float stepSize = 100F / (float) _patches.size();

    int count = -1;
    boolean previousPatchingAllSucceed = true;
    String currentVersion = fromVersion;
    Map<String, String> destinationReplacement = new HashMap<String, String>();

    for (Patch _patch : _patches) {
      count++;

      // check if the update if not suitable
      if (!currentVersion.equals(_patch.getVersionFrom())) {
        // normally should not reach here
        listener.patchInvalid(_patch);
        continue;
      }

      // temporary storage folder for this patch
      File tempDirForPatch = new File(tempDir.getAbsolutePath() + File.separator + _patch.getId());
      if (!tempDirForPatch.isDirectory() && !tempDirForPatch.mkdirs()) {
        throw new IOException("Failed to create folder for patches.");
      }

      AESKey aesKey = null;
      if (_patch.getDownloadEncryptionKey() != null) {
        aesKey = new AESKey(Util.hexStringToByteArray(_patch.getDownloadEncryptionKey()), Util.hexStringToByteArray(_patch.getDownloadEncryptionIV()));
      }

      File patchFile = new File(tempDir.getAbsolutePath() + File.separator + _patch.getId() + ".patch");
      File decryptedPatchFile = new File(tempDir.getAbsolutePath() + File.separator + _patch.getId() + ".patch.decrypted");
      decryptedPatchFile.deleteOnExit();
      if (!patchFile.exists()) {
        listener.patchInvalid(_patch);
        throw new IOException("Patch file not found: " + patchFile.getAbsolutePath());
      }

      // initialize patcher
      final int _count = count;
      patcher = new Patcher(new File(tempDirForPatch + File.separator + "action.log"));
      List<ReplacementRecord> replacementList = patcher.doPatch(new PatcherListener() {

        @Override
        public void patchProgress(int percentage, String message) {
          float base = stepSize * (float) _count;
          float addition = ((float) percentage / 100F) * stepSize;
          listener.patchProgress((int) (base + addition), message);
        }

        @Override
        public void patchEnableCancel(boolean enable) {
          listener.patchEnableCancel(enable);
        }
      }, patchFile, _patch.getId(), aesKey, applyToFolder, tempDirForPatch, destinationReplacement);
      for (ReplacementRecord _replacement : replacementList) {
        switch (_replacement.getOperationType()) {
          case REMOVE:
            destinationReplacement.put(_replacement.getDestinationFilePath(), _replacement.getBackupFilePath());
            break;
          case REPLACE:
          case PATCH:
          case FORCE:
            destinationReplacement.put(_replacement.getDestinationFilePath(), _replacement.getNewFilePath());
            break;
          case NEW:
            if (!_replacement.getNewFilePath().isEmpty() && !_replacement.getDestinationFilePath().isEmpty()) {
              destinationReplacement.put(_replacement.getDestinationFilePath(), _replacement.getNewFilePath());
            }
            break;
        }
      }
      if (!replacementList.isEmpty()) {
        previousPatchingAllSucceed = false;
      }

      currentVersion = _patch.getVersionTo();

      if (previousPatchingAllSucceed) {
        listener.patchFinished(_patch);
        patcher.clearBackup();
        patchFile.delete();
      }

      patcher = null;
    }

    return returnList;
  }
}
