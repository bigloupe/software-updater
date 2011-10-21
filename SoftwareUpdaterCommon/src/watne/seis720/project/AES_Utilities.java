/**
 * The Software and related technology may not be downloaded or otherwise 
 * exported or reexported (i) into (or to a national or resident of) Cuba, Iraq, Libya, 
 * North Korea, Iran, or any other country to which the U.S. has embargoed goods; 
 * or (ii) to anyone on the U.S. Treasury Department's list of Specially Designated 
 * Nationals or the U.S. Commerce Department's Table of Denial Orders. By installing 
 * or using the Software, Licensee is agreeing to the foregoing and representing 
 * and warranting that they are not located in, under the control of, or a national or 
 * resident of any such country or on any such list. In addition, The Licensee agrees 
 * to comply with any other applicable U.S. export control laws and any local laws
 * in their jurisdiction that may impact the right to import, export, or use the Software. 
 * By installing or using the Software, Licensee is also representing and warranting
 * that they will not use, or permit or authorize others to use, the Software in connection 
 * with the design, development, production, stockpiling or use of any chemical or 
 * biological weapons.
 */
package watne.seis720.project;

import java.awt.HeadlessException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import javax.crypto.NoSuchPaddingException;
import javax.swing.JOptionPane;

import org.apache.commons.codec.binary.Base64;

/**
 * This class holds helper utility methods for objects implementing the
 * AES_Implementable interface.
 * @author John Watne
 * 
 */
public class AES_Utilities
{
    /**
     * Get an array containing the bytes represented by the given Base64 text.
     * @param base64Text the Base64 representation of the bytes to be obtained.
     * @return an array containing the bytes represented by the given Base64
     * text.
     * @throws IOException if an I/O error occurs when attempting to read the
     * text.
     */
    public static byte[] getBytesFromBase64(String base64Text)
                                                              throws IOException
    {
        return Base64.decodeBase64(base64Text);
    } // public static byte[] getBytesFromBase64(String base64Text)

    /**
     * Get the Base64 text representation of the given byte sequence.
     * @param bytes the byte sequence for which the Base64 text representation
     * is to be given.
     * @return the Base64 text representation of the given byte sequence.
     */
    public static String getBase64Text(byte[] bytes)
    {
        return Base64.encodeBase64URLSafeString(bytes);
    }

    /**
     * Get a String representing the given array of bytes as a hexadecimal
     * number. Code adapted from <a
     * href="http://www.exampledepot.com/egs/java.math/Bytes2Str.html">Parsing
     * and Formatting a Byte Array into Binary, Octal, and Hexadecimal (Java
     * Developers Almanac Example)</a>
     * @param bytes the sequence of bytes for which the hexadecimal String
     * representation will be given.
     * @return a String representing the given array of bytes as a hexadecimal
     * number.
     */
    public static String getHexString(byte[] bytes)
    {
        String hexString = null;
        BigInteger bigInt = new BigInteger(bytes);
        hexString = bigInt.toString(16); // Get base 16 (hexadecimal)
        // representation.

        if (hexString.length() % 2 != 0)
        {
            // Make sure an even number of characters: start with a zero.
            hexString = "0" + hexString;
        }

        return hexString;
    } // public static String getHexString(byte[] bytes)

    /**
     * Get the sequence of bytes represented by the given String. Code adapted
     * from <a
     * href="http://www.exampledepot.com/egs/java.math/Bytes2Str.html">Parsing
     * and Formatting a Byte Array into Binary, Octal, and Hexadecimal (Java
     * Developers Almanac Example)</a>
     * @param hexString the hexadecimal representation of the number.
     * @return the sequence of bytes represented by the given String.
     */
    public static byte[] getBytesFromHex(String hexString)
    {
        BigInteger bigInt = new BigInteger(hexString, 16); // Parse the string.
        byte[] hexBytes = bigInt.toByteArray();
        return hexBytes;
    }

    /**
     * Given an array of bytes padded using the specified method, return an
     * unpadded array of bytes.
     * @param padded the padded array of bytes.
     * @param padding the padding methodology used.
     * @return an unpadded array of bytes.
     * @throws NoSuchPaddingException if the padded array is not of the proper
     * length for the given Padding type.
     */
    public static byte[] getUnpadded(byte[] padded, Padding padding)
                                                                    throws NoSuchPaddingException
    {
        byte[] unpadded = padded; // Initialize to padded.

        switch (padding)
        {
            case PKCS5PADDING:
                // Get number of bytes padding.
                if (padded.length > 0)
                {
                    // Last padding bytes have number of padding bytes, so just
                    // look at last byte in array and convert to int.
                    int numPaddingBytes = padded[padded.length - 1];

                    if (numPaddingBytes < 1)
                    {
                        throw new NoSuchPaddingException(
                            "AES_Utilities.getUnpadded() error: Invalid number of padding bytes: "
                                            + numPaddingBytes);
                    } // if (numPaddingBytes < 1)
                    else if (numPaddingBytes > padded.length)
                    {
                        throw new NoSuchPaddingException(
                            "AES_Utilities.getUnpadded() error: padding "
                                            + numPaddingBytes
                                            + " bytes exceeds array size "
                                            + padded.length);
                    } // else if (numPaddingBytes > padded.length)

                    // Create new unpadded array without numPaddingBytes
                    // elements at end.
                    unpadded = new byte[padded.length - numPaddingBytes];
                    // Copy the elements before the padding.
                    System.arraycopy(padded, 0, unpadded, 0, padded.length
                                    - numPaddingBytes);
                } // if (padded.length > 0)
                else
                {
                    throw new NoSuchPaddingException(
                        "AES_Utilities.getUnpadded() error: array has no padding");
                } // else [padded.length <= 0]
                break; // case PKCS5PADDING
            case NOPADDING:
                /* Unpadded = padded; do nothing. */
                break;
            default:
                throw new NoSuchPaddingException("Padding mode " + padding
                                + " is not supported.");
        } // switch (padding)

        return unpadded; // Return unpadded array.
    } // public static byte[] getUnpadded(byte[], Padding)

    /**
     * Given an array of bytes to pad and the KeySize object from which the
     * block size may be determined, return a new array of bytes padded using
     * the specified algorithm.
     * @param unpadded the original, unpadded array of bytes.
     * @param padding the padding methodology to use.
     * @param keySize the key size.
     * @return a new array of bytes padded using the specified algorithm.
     */
    public static byte[] getPadded(byte[] unpadded, Padding padding,
                                   KeySize keySize)
    {
        byte[] padded = unpadded; // Initialize to unpadded array.

        // TESTING
        // System.out.println("unpadded size (# bytes): " + unpadded.length);
        // System.out.println("Bytes:");
        //
        // for (int i = 0; i < unpadded.length; i++)
        // System.out.println("" + Byte.toString(unpadded[i]) + " ");
        //
        // System.out.println();
        // END TESTING

        switch (padding)
        {
            case PKCS5PADDING:
                int numBytesPadding =
                    keySize.getBlockSizeBytes()
                                    - (unpadded.length % keySize
                                        .getBlockSizeBytes());
                // Get number of bytes to append to end.

                // System.out.println("Blocksize: " +
                // keySize.getBlockSizeBytes()
                // + " bytes");
                // System.out.println("numBytesPadding: " + numBytesPadding);

                padded = new byte[unpadded.length + numBytesPadding];
                // Make new array with extra bits for padding at the end.
                System.arraycopy(unpadded, 0, padded, 0, unpadded.length);
                // Copy bytes from unpadded to padded.

                for (int i = 0; i < numBytesPadding; i++)
                {
                    padded[i + unpadded.length] = (byte) numBytesPadding;
                    // Each of the padding bits is set to the number of bits of
                    // padding.
                } // for (int i = 0; i < numBitsPadding; i++)
                break; // end PKCS50ADDING
            case NOPADDING:
                /* DO nothing; padded = unpadded. */
                break;
            default:
                System.out
                    .println("AES_Utilities.getPadded() error: unsupported padding type "
                                    + padding);
        } // switch (padding)

        // System.out.println("Padded bytes: ");
        //
        // for (int i = 0; i < padded.length; i++)
        // System.out.print("" + Byte.toString(padded[i]) + " ");
        //
        // System.out.println();

        return padded; // Return the padded array.
    } // public static byte[] getPadded(byte[], Padding, KeySize)

    /**
     * Rotate the given number left by the given number of bits. Code from
     * &quot;MsTingle&quot;
     * (http://forum.java.sun.com/profile.jspa?userID=313283), at &quot;Java
     * Programming [Archive] - using rotate and XOR..plz help&quot;,
     * http://forum.java.sun.com/thread.jspa?threadID=389286&messageID=1679453,
     * with minor adaptation.
     * @param number the number whose bits are to be rotated left.
     * @param bits the number of bits to rotate left.
     * @return the number circularly shifted left by the specified number of
     * bits.
     */
    public static int rotateLeft(int number, int bits)
    {
        return (number << bits) | (bits >>> (32 - bits));
    } // public static int RotateLeft(int number, int bits)

    /**
     * Construct an int (&quot;word&quot; in the Stallings text) from the given
     * four bytes, listed in high- to low-order.
     * @param b1 the highest-order byte of the int
     * @param b2 the second-highest-order byte of the int
     * @param b3 the third-highest-order byte of the int
     * @param b4 the lowest-order byte of the int.
     * @return an int constructed from the set of bytes.
     */
    public static int getWordFromBytes(byte b1, byte b2, byte b3, byte b4)
    {
        // System.out.printf("%2X %2X %2X %2X\n", b1, b2, b3, b4);
        // System.out
        // .printf("%8X\n", (0xff000000 & ((0xff & b1) << 24))
        // | (0x00ff0000 & ((0xff & b2) << 16))
        // | (0x0000ff00 & ((0xff & b3) << 8))
        // | (0x000000ff & b4));
        return (0xff000000 & ((0xff & b1) << 24))
                        | (0x00ff0000 & ((0xff & b2) << 16))
                        | (0x0000ff00 & ((0xff & b3) << 8)) | (0x000000ff & b4);
    } // public static int getWordFromBytes(byte, byte, byte, byte)

    /**
     * Given an int (&quot;word&quot; in the Stallings text), return an array of
     * the bytes within, high-order byte first.
     * @param word the int from which the bytes are to be extracted.
     * @return the set of four bytes within the int, highest order to lowest
     * order.
     */
    public static byte[] getBytesFromWord(int word)
    {
        // System.out.printf("\n%8X\n", word);
        byte[] values = new byte[4];
        values[0] = (byte) ((word & 0xff000000) >> 24);
        values[1] = (byte) ((word & 0xff0000) >> 16);
        values[2] = (byte) ((word & 0xff00) >> 8);
        values[3] = (byte) (word & 0xff);

        // for (int i = 0; i < 4; i++)
        // System.out.printf("%2X\n", values[i]);

        return values;
    } // public static byte[] getBytesFromWord(int word)

    /**
     * Given a 16-byte one-dimensional array, create a 4x4 array of bytes. The
     * first index is the row, the second is the column. Bytes are entered down
     * columns first, then across rows.
     * @param linearArray the one-dimensional array of bytes.
     * @return the bytes rearranged as a 4x4 array.
     */
    public static byte[][] makeFourByFourByteArray(byte[] linearArray)
    {
        byte[][] grid = new byte[4][4];

        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 4; col++)
                grid[row][col] = linearArray[row + 4 * col];

        return grid;
    } // static byte[][] makeFourByFourByteArray(byte[] linearArray)

    /**
     * Given a 4x4 array of bytes, with the first index indicating the row and
     * the second the column, construct a one-dimensional array, reading down
     * columns first, then across rows.
     * @param grid the 4x4 array of bytes.
     * @return a 16-byte one-dimensional array, with data extracted from grid as
     * noted.
     */
    public static byte[] linearizeFourByFourByteArray(byte[][] grid)
    {
        byte[] linear = new byte[16];

        for (int i = 0; i < linear.length; i++)
        {
            linear[i] = grid[i - 4 * (i / 4)][i / 4];
        }
        return linear;
    } // static byte[] linearizeFourByFourByteArray(byte[][] grid)

    /**
     * Show a dialog asking the user if they wish to overwrite the specified
     * existing file and return their answer.
     * @param f the existing file.
     * @return true if the user wishes to overwrite the file, false otherwise.
     * @throws IOException if unable to read the file.
     * @throws HeadlessException if run in an environment that doesn't allow
     * displaying windows.
     */
    public static boolean wishToOverwrite(File f) throws HeadlessException,
                                                 IOException
    {
        int rc =
            JOptionPane.showConfirmDialog(null,
                "Do you wish to overwrite the existing file?", "The file "
                                + f.getName() + " already exists",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return (rc == JOptionPane.YES_OPTION);
    } // public static boolean wishToOverwrite(File f)

    /**
     * Compute the Adler-32 checksum of the specified file. Code based on
     * &quot;e457. Calculating the Checksum of a File&quot;, &quot;The Java
     * Developers Almanac 1.4&quot;, <a
     * href="http://www.exampledepot.com/egs/java.util.zip/ChecksumFile.html">http://www.exampledepot.com/egs/java.util.zip/ChecksumFile.html</a>
     * @param checkFile the file to be checked.
     * @return the Adler-32 checkusm of the specified file.
     * @throws IOException if an error occurs reading checkFile.
     */
    public static long getFileChecksum(File checkFile) throws IOException
    {
        long checksum = -1;

        CheckedInputStream cis =
            new CheckedInputStream(new FileInputStream(checkFile),
                new Adler32());
        byte[] tempBuf = new byte[128];

        while (cis.read(tempBuf) >= 0)
        {

        }
        checksum = cis.getChecksum().getValue();
        return checksum;
    } // public static long getFileChecksum(File checkFile)
} // public class AES_Utilities
