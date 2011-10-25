package updater.patch;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.tukaani.xz.XZInputStream;
import updater.script.InvalidFormatException;
import updater.script.Patch;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchReadUtil {

    protected PatchReadUtil() {
    }

    public static void readHeader(InputStream in) throws IOException {
        byte[] buf = new byte[5];
        if (in.read(buf, 0, 5) != 5) {
            throw new IOException("Reach the end of stream.");
        }
        if (buf[0] != 'P' || buf[1] != 'A' || buf[2] != 'T' || buf[3] != 'C' || buf[4] != 'H') {
            throw new IOException("Invalid patch header.");
        }
    }

    public static InputStream readCompressionMethod(InputStream in) throws IOException {
        byte[] buf = new byte[1];
        if (in.read(buf, 0, 1) != 1) {
            throw new IOException("Reach the end of stream.");
        }
        int compressionMode = buf[0] & 0xff;
        switch (compressionMode) {
            case 0: //gzip
                return new GZIPInputStream(in);
            case 1: // XZ/LZMA2
                return new XZInputStream(in);
            default:
                throw new IOException("Compression method not supported/not exist");
        }
    }

    public static Patch readXML(InputStream in) throws IOException, InvalidFormatException {
        byte[] buf = new byte[3];
        if (in.read(buf, 0, 3) != 3) {
            throw new IOException("Reach the end of stream.");
        }
        int xmlLength = ((buf[0] & 0xff) << 16) | ((buf[1] & 0xff) << 8) | (buf[2] & 0xff);
        byte[] xmlData = new byte[xmlLength];
        if (in.read(xmlData) != xmlLength) {
            throw new IOException("Reach the end of stream.");
        }
        return Patch.read(xmlData);
    }
}
