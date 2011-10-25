package updater.patch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchWriteUtil {

    protected PatchWriteUtil() {
    }

    public static void writeHeader(OutputStream out) throws IOException {
        out.write('P');
        out.write('A');
        out.write('T');
        out.write('C');
        out.write('H');
    }

    public static OutputStream writeCompressionMethod(OutputStream out, Compression compression) throws IOException {
        out.write(compression.getValue());
        switch (compression) {
            case GZIP:
                return new GZIPOutputStream(out);
            case LZMA2:
                return new XZOutputStream(out, new LZMA2Options());
            default:
                throw new IOException("Compression method not supported/not exist");
        }
    }

    public static enum Compression {

        GZIP(0), LZMA2(1);
        protected final int value;

        Compression(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static void writeXML(OutputStream out, byte[] content) throws IOException {
        int contentLength = content.length;

        out.write((contentLength >> 16) & 0xff);
        out.write((contentLength >> 8) & 0xff);
        out.write(contentLength & 0xff);

        // XML content, max 16MiB
        out.write(content);
    }

    public static void writePatch(File fromFile, OutputStream toStream) throws IOException {
        FileInputStream fin = null;
        try {
            long fileLength = fromFile.length();

            fin = new FileInputStream(fromFile);

            byte[] b = new byte[8096];
            int byteRead, cumulativeByteRead = 0;
            while ((byteRead = fin.read(b)) != -1) {
                toStream.write(b, 0, byteRead);
                cumulativeByteRead += byteRead;

                if (cumulativeByteRead >= fileLength) {
                    break;
                }
            }

            if (cumulativeByteRead != fileLength) {
                throw new IOException("Number of bytes read not equals to the cumulative number of bytes read, from file: " + fromFile.getAbsolutePath() + ", cumulate: " + cumulativeByteRead + ", expected length: " + fileLength);
            }

            fin.close();
        } finally {
            if (fin != null) {
                fin.close();
            }
        }
    }
}
