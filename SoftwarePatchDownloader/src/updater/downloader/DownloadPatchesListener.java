// Copyright (c) 2012 Chan Wai Shing
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package updater.downloader;

import java.io.IOException;
import updater.script.Patch;

/**
 * The download patch listener for {@link #downloadPatches(updater.downloader.PatchDownloader.DownloadPatchesListener, java.lang.String, java.util.List)} and {@link #downloadPatches(updater.downloader.PatchDownloader.DownloadPatchesListener, java.io.File, updater.script.Client, java.util.List)}.
 * This is used to listen to download patch progress and result notification.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
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