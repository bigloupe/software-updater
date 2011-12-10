package updater.downloader;

/**
 * The return result used by {@link #downloadPatches(updater.downloader.PatchDownloader.DownloadPatchesListener, java.io.File, updater.script.Client, java.util.List, int, int)}.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public enum DownloadPatchesResult {

  SAVE_TO_CLIENT_SCRIPT_FAIL, DOWNLOAD_INTERRUPTED, ERROR, COMPLETED
}