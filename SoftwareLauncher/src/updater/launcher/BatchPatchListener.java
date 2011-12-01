package updater.launcher;

import java.io.IOException;
import updater.patch.PatcherListener;
import updater.script.Patch;

/**
 * The batch patch listener.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public interface BatchPatchListener extends PatcherListener {

    /**
     * Notify the invalid patch.
     * @param patch the patch
     * @throws IOException error occurred when recording
     */
    void patchInvalid(Patch patch) throws IOException;

    /**
     * Notify the patch has been applied successfully.
     * @param patch the patch
     * @throws IOException error occurred when recording
     */
    void patchFinished(Patch patch) throws IOException;
}
