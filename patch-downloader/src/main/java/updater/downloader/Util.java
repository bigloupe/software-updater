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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import updater.util.CommonUtil;

/**
 * Utilities.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Util extends CommonUtil {

  protected Util() {
  }

  /**
   * Decompress a GZIP compressed content.
   * @param compressedData the compressed content
   * @return the decompressed data
   * @throws IOException the {@code compressedData} is not GZIP format
   */
  public static byte[] GZipDecompress(byte[] compressedData) throws IOException {
    ByteArrayOutputStream decompressedOut = new ByteArrayOutputStream();
    ByteArrayInputStream compressedIn = new ByteArrayInputStream(compressedData);
    GZIPInputStream decompressedGIn = new GZIPInputStream(compressedIn);
    int byteRead;
    byte[] b = new byte[32768];
    while ((byteRead = decompressedGIn.read(b)) != -1) {
      decompressedOut.write(b, 0, byteRead);
    }
    return decompressedOut.toByteArray();
  }

  /**
   * Convert the number of bytes to human readable string.
   * @param bytes the number of bytes
   * @param si use internation system of units of not, true to use 1000, 
   * false to use 1024
   * @return the human readable string of the 'number of bytes'
   */
  public static String humanReadableByteCount(long bytes, boolean si) {
    // http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
    int unit = si ? 1000 : 1024;
    if (bytes < unit) {
      return bytes + " B";
    }
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }

  /**
   * Convert the time (in second) to human readable string.
   * @param timeInSecond the time to convert
   * @param maxDisplay indicate the max number of item to display, max: 6, 
   * e.g. timeInSecond = 3661, maxDisplay: 2 => 1h 1m
   * @return the human readable string of the time
   */
  public static String humanReadableTimeCount(int timeInSecond, int maxDisplay) {
    int buf = timeInSecond, count = 0;
    StringBuilder sb = new StringBuilder();

    if (buf >= 31536000) {
      int year = buf / 31536000;
      buf %= 31536000;

      sb.append(year);
      sb.append(" yr");
      sb.append(year > 1 ? 's' : "");

      count++;
    }
    if (count < maxDisplay && (buf >= 2592000 || count != 0)) {
      sb.append(count != 0 ? ", " : "");

      int month = buf / 2592000;
      buf %= 2592000;

      sb.append(month);
      sb.append(" mth");
      sb.append(month > 1 ? 's' : "");

      count++;
    }
    if (count < maxDisplay && (buf >= 86400 || count != 0)) {
      sb.append(count != 0 ? ", " : "");

      int day = buf / 86400;
      buf %= 86400;

      sb.append(day);
      sb.append(" day");
      sb.append(day > 1 ? 's' : "");

      count++;
    }
    if (count < maxDisplay && (buf >= 3600 || count != 0)) {
      sb.append(count != 0 ? ", " : "");

      int hour = buf / 3600;
      buf %= 3600;

      sb.append(hour);
      sb.append('h');

      count++;
    }
    if (count < maxDisplay && (buf >= 60 || count != 0)) {
      sb.append(count != 0 ? ' ' : "");

      int minute = buf / 60;
      buf %= 60;

      sb.append(minute);
      sb.append('m');

      count++;
    }
    if (count < maxDisplay) {
      sb.append(count != 0 ? ' ' : "");

      sb.append(buf);
      sb.append('s');
    }

    return sb.toString();
  }
}
