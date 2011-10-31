package updater.patch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.tukaani.xz.XZInputStream;
import updater.crypto.AESKey;
import updater.script.InvalidFormatException;
import updater.script.Patch;
import watne.seis720.project.KeySize;
import watne.seis720.project.Mode;
import watne.seis720.project.Padding;
import watne.seis720.project.WatneAES_Implementer;

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

    public static void readToFile(File saveTo, InputStream in, int length) throws IOException {
        FileOutputStream fout = null;
        try {
            byte[] b = new byte[32768];
            int byteRead, cumulativeByteRead = 0, byteToRead;
            fout = new FileOutputStream(saveTo);

            byteToRead = length > b.length ? b.length : length;

            while ((byteRead = in.read(b, 0, byteToRead)) != -1) {
                fout.write(b, 0, byteRead);

                cumulativeByteRead += byteRead;
                if (cumulativeByteRead >= length) {
                    break;
                }

                byteToRead = length - cumulativeByteRead > b.length ? b.length : length - cumulativeByteRead;
            }
        } finally {
            if (fout != null) {
                fout.close();
            }
        }
    }

    public static void decrypt(AESKey aesKey, File patchFile, File tempFileForDecryption) throws IOException {
        tempFileForDecryption.delete();

        try {
            WatneAES_Implementer aesCipher = new WatneAES_Implementer();
            aesCipher.setMode(Mode.CBC);
            aesCipher.setPadding(Padding.PKCS5PADDING);
            aesCipher.setKeySize(KeySize.BITS256);
            aesCipher.setKey(aesKey.getKey());
            aesCipher.setInitializationVector(aesKey.getIV());
            aesCipher.decryptFile(patchFile, tempFileForDecryption);
        } catch (Exception ex) {
            throw new IOException("Error occurred when decrypting the patch: " + ex.getMessage());
        }
    }
}
