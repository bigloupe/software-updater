package updater.downloader;

import java.io.IOException;
import updater.script.Patch;

/**
 * The download patch listener for {@link #downloadPatches(updater.downloader.PatchDownloader.DownloadPatchesListener, java.lang.String, java.util.List)} and {@link #downloadPatches(updater.downloader.PatchDownloader.DownloadPatchesListener, java.io.File, updater.script.Client, java.util.List)}.
 * This is used to listen to download patch progress and result notification.
 */
public interface DownloadPatchesListener {

    /**
     * Notify a patch is downloaded.
     * @param patch the patch
     * @throws IOException error occurred when saving the status 
     */
    void downloadPatchesPatchDownloaded(Patch patch) throws IOException;

    /**
     * Notify the download progress.
     * @param progress the progress range from 0 to 100
     */
    void downloadPatchesProgress(int progress);

    /**
     * Notify the change in description of current taking action.
     * @param message the message/description
     */
    void downloadPatchesMessage(String message);
}