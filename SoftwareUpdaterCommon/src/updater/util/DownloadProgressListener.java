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
package updater.util;

/**
 * Progress Listener for {@link updater.util.HTTPDownloader}.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public interface DownloadProgressListener {

  /**
   * Notify which position to start the download from.
   * This should be called only once or less, and be called before first 
   * notifying {@link #byteDownloaded(int)}.
   * @param pos the position
   */
  void byteStart(long pos);

  /**
   * Notify the total length of the file to download.
   * This should be called only once or less, and be called before first 
   * notifying {@link #byteDownloaded(int)}.
   * @param total the size in byte, -1 means length not known
   */
  void byteTotal(long total);

  /**
   * Notify the byte downloaded since last notification.
   * @param numberOfBytes the bytes downloaded
   */
  void byteDownloaded(int numberOfBytes);

  /**
   * The downloader is going to retry download. This will be invoked every time 
   * before the downloader is going to retry.
   * @param result the reason for why the downloader will retry download
   */
  void downloadRetry(DownloadResult result);
}
