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
import updater.util.CommonUtil;
import watne.seis720.project.AESForFile;
import watne.seis720.project.AESForFileListener;
import watne.seis720.project.KeySize;
import watne.seis720.project.Mode;
import watne.seis720.project.Padding;

/**
 * Functions for reading the patch.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchReadUtil {

  protected PatchReadUtil() {
  }

  /**
   * Read the header from the stream.
   * @param in the stream to read
   * @throws IOException failed to get/detect a valid header
   */
  public static void readHeader(InputStream in) throws IOException {
    if (in == null) {
      throw new NullPointerException("argument 'in' cannot be null");
    }

    byte[] buf = new byte[5];
    if (in.read(buf, 0, 5) != 5) {
      throw new IOException("Reach the end of stream.");
    }

    if (buf[0] != 'P' || buf[1] != 'A' || buf[2] != 'T' || buf[3] != 'C' || buf[4] != 'H') {
      throw new IOException("Invalid patch header.");
    }
  }

  /**
   * Read the compression method used by the patch.
   * @param in the stream to read
   * @return the decompress stream of {@code in}
   * @throws IOException error occurred when reading from {@code in}
   */
  public static InputStream readCompressionMethod(InputStream in) throws IOException {
    if (in == null) {
      throw new NullPointerException("argument 'in' cannot be null");
    }

    byte[] buf = new byte[1];
    if (in.read(buf, 0, 1) != 1) {
      throw new IOException("Reach the end of stream.");
    }

    int compressionMode = buf[0] & 0xff;
    Compression compression = Compression.getCompression(compressionMode);
    if (compression != null) {
      switch (compression) {
        case GZIP:
          return new GZIPInputStream(in);
        case LZMA2: // XZ/LZMA2
          return new XZInputStream(in);
      }
    }

    throw new IOException("Compression method not supported/not exist");
  }

  /**
   * Read the XML from the stream.
   * @param in the stream to read
   * @return the XML read
   * @throws IOException error occurred when reading
   * @throws InvalidFormatException the format of the XML read is incorrect
   */
  public static Patch readXML(InputStream in) throws IOException, InvalidFormatException {
    if (in == null) {
      throw new NullPointerException("argument 'in' cannot be null");
    }

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

  /**
   * Read from the stream with size {@code length} and save to {@code saveTo}.
   * @param saveTo the file to save to
   * @param in the stream to read
   * @param length the size to read
   * @throws IOException error occurred when reading from {@code in} or 
   * saving to {@code saveTo}
   */
  public static void readToFile(File saveTo, InputStream in, int length) throws IOException {
    if (saveTo == null) {
      throw new NullPointerException("argument 'saveTo' cannot be null");
    }
    if (in == null) {
      throw new NullPointerException("argument 'in' cannot be null");
    }
    if (length <= 0) {
      return;
    }

    FileOutputStream fout = null;
    try {
      fout = new FileOutputStream(saveTo);

      byte[] b = new byte[32768];
      int byteRead, cumulativeByteRead = 0, byteToRead;
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
      CommonUtil.closeQuietly(fout);
    }
  }

  /**
   * Decrypt the {@code patchFile} and save to {@code decryptTo}.
   * @param aesKey the cipher key to use
   * @param listener the progress listener, accept null
   * @param patchFile the file to decrypt
   * @param decryptTo the file to save the decrypted file
   * @throws IOException error occurred when decrypting
   */
  public static void decrypt(AESKey aesKey, AESForFileListener listener, File patchFile, File decryptTo) throws IOException {
    if (aesKey == null) {
      throw new NullPointerException("argument 'aesKey' cannot be null");
    }
    if (patchFile == null) {
      throw new NullPointerException("argument 'patchFile' cannot be null");
    }
    if (decryptTo == null) {
      throw new NullPointerException("argument 'decryptTo' cannot be null");
    }

    decryptTo.delete();

    try {
      AESForFile aesCipher = new AESForFile();
      if (listener != null) {
        aesCipher.setListener(listener);
      }
      aesCipher.setMode(Mode.CBC);
      aesCipher.setPadding(Padding.PKCS5PADDING);
      aesCipher.setKeySize(KeySize.BITS256);
      aesCipher.setKey(aesKey.getKey());
      aesCipher.setInitializationVector(aesKey.getIV());
      aesCipher.decryptFile(patchFile, decryptTo);
    } catch (Exception ex) {
      throw new IOException("Error occurred when decrypting the patch: " + ex.getMessage());
    }
  }
}
