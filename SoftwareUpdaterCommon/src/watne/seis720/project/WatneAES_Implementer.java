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

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Objects of this class implement AES encryption using code written by John
 * Watne, based on the outline of the AES algorithm in &quot;Cryptography and
 * Network Security: Principles and Practices, Fourth Edition&quot; by William
 * Stallings, Chapter 5. Default to 128-bit keys, ECB mode, and PKCS5PADDING,
 * all of which may be overridden by calls to the appropriate setter methods.
 * <p>
 * To be consistent with the methodology outlined in the Stallings text,
 * two-dimensional arrays are referenced such that the first index specifies the
 * row, and the second index specifies the column. Bytes are loaded from the
 * plaintext one-dimensional array into two-dimensional state arrays going down
 * columns first, then across rows.
 * </p>
 * <p>
 * To speed implementation of hard-coded S-box and inverse S-box values, the
 * values were copied from &quot;The Laws of Cryptography: AES: S-Boxes&quot; by
 * Neil R. Wagner, <a
 * href="http://www.cs.utsa.edu/~wagner/laws/SBoxes.html">http://www.cs.utsa.edu/~wagner/laws/SBoxes.html</a>.
 * The file was imported into Microsoft Excel, and then text formulas were used
 * to reformat each table of values into a set of 2-dimensional byte array
 * initializers.
 * </p>
 * @author John Watne
 * 
 */
public class WatneAES_Implementer extends AES_Common implements
                AES_Implementable
{
    /**
     * The irreducible polynomial x<sup>8</sup> + x<sup>4</sup> + x<sup>3</sup> +
     * x + 1 value, used in multiplication over Galois Field GF(2<sup>8</sup>).
     */
    public static final int GF_POLYNOMIAL = 0x11B;

    /**
     * If two numbers, multiplied together in GF(2<sup>8</sup>), produce a
     * value greater than this value, then take the remainder of their product
     * divided using GF_POLYNOMIAL.
     */
    private static final int MAX_BYTE_VAL = 0xff;

    /**
     * At each step in the division process, dividing the product by this number
     * indicates the value by which GF_POLYNOMIAL must be multiplied, with the
     * product XORed against the current step's division value.
     */
    private static final int TWO_TO_THE_EIGHTH = 0x100;

    /**
     * The array of coefficients to use for the forward mix colulmns operation.
     */
    private static byte[][] fwMixCol = {
        {
            (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01 }, {
            (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01 }, {
            (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03 }, {
            (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x02 } };

    private static byte[][] invMixCol = {
        {
            (byte) 0x0e, (byte) 0x0b, (byte) 0x0d, (byte) 0x09 }, {
            (byte) 0x09, (byte) 0x0e, (byte) 0x0b, (byte) 0x0d }, {
            (byte) 0x0d, (byte) 0x09, (byte) 0x0e, (byte) 0x0b }, {
            (byte) 0x0b, (byte) 0x0d, (byte) 0x09, (byte) 0x0e } };

    // To speed instantiation of objects of this class, though slow down the
    // time it takes to write this code, hard-code the S-box and inverse S-box
    // arrays. 1st index = row; 2nd index - column.
    private static byte[][] sBox =
        {
            {
                (byte) 0x63, (byte) 0x7c, (byte) 0x77, (byte) 0x7b,
                (byte) 0xf2, (byte) 0x6b, (byte) 0x6f, (byte) 0xc5,
                (byte) 0x30, (byte) 0x01, (byte) 0x67, (byte) 0x2b,
                (byte) 0xfe, (byte) 0xd7, (byte) 0xab, (byte) 0x76 },
            {
                (byte) 0xca, (byte) 0x82, (byte) 0xc9, (byte) 0x7d,
                (byte) 0xfa, (byte) 0x59, (byte) 0x47, (byte) 0xf0,
                (byte) 0xad, (byte) 0xd4, (byte) 0xa2, (byte) 0xaf,
                (byte) 0x9c, (byte) 0xa4, (byte) 0x72, (byte) 0xc0 },
            {
                (byte) 0xb7, (byte) 0xfd, (byte) 0x93, (byte) 0x26,
                (byte) 0x36, (byte) 0x3f, (byte) 0xf7, (byte) 0xcc,
                (byte) 0x34, (byte) 0xa5, (byte) 0xe5, (byte) 0xf1,
                (byte) 0x71, (byte) 0xd8, (byte) 0x31, (byte) 0x15 },
            {
                (byte) 0x04, (byte) 0xc7, (byte) 0x23, (byte) 0xc3,
                (byte) 0x18, (byte) 0x96, (byte) 0x05, (byte) 0x9a,
                (byte) 0x07, (byte) 0x12, (byte) 0x80, (byte) 0xe2,
                (byte) 0xeb, (byte) 0x27, (byte) 0xb2, (byte) 0x75 },
            {
                (byte) 0x09, (byte) 0x83, (byte) 0x2c, (byte) 0x1a,
                (byte) 0x1b, (byte) 0x6e, (byte) 0x5a, (byte) 0xa0,
                (byte) 0x52, (byte) 0x3b, (byte) 0xd6, (byte) 0xb3,
                (byte) 0x29, (byte) 0xe3, (byte) 0x2f, (byte) 0x84 },
            {
                (byte) 0x53, (byte) 0xd1, (byte) 0x00, (byte) 0xed,
                (byte) 0x20, (byte) 0xfc, (byte) 0xb1, (byte) 0x5b,
                (byte) 0x6a, (byte) 0xcb, (byte) 0xbe, (byte) 0x39,
                (byte) 0x4a, (byte) 0x4c, (byte) 0x58, (byte) 0xcf },
            {
                (byte) 0xd0, (byte) 0xef, (byte) 0xaa, (byte) 0xfb,
                (byte) 0x43, (byte) 0x4d, (byte) 0x33, (byte) 0x85,
                (byte) 0x45, (byte) 0xf9, (byte) 0x02, (byte) 0x7f,
                (byte) 0x50, (byte) 0x3c, (byte) 0x9f, (byte) 0xa8 },
            {
                (byte) 0x51, (byte) 0xa3, (byte) 0x40, (byte) 0x8f,
                (byte) 0x92, (byte) 0x9d, (byte) 0x38, (byte) 0xf5,
                (byte) 0xbc, (byte) 0xb6, (byte) 0xda, (byte) 0x21,
                (byte) 0x10, (byte) 0xff, (byte) 0xf3, (byte) 0xd2 },
            {
                (byte) 0xcd, (byte) 0x0c, (byte) 0x13, (byte) 0xec,
                (byte) 0x5f, (byte) 0x97, (byte) 0x44, (byte) 0x17,
                (byte) 0xc4, (byte) 0xa7, (byte) 0x7e, (byte) 0x3d,
                (byte) 0x64, (byte) 0x5d, (byte) 0x19, (byte) 0x73 },
            {
                (byte) 0x60, (byte) 0x81, (byte) 0x4f, (byte) 0xdc,
                (byte) 0x22, (byte) 0x2a, (byte) 0x90, (byte) 0x88,
                (byte) 0x46, (byte) 0xee, (byte) 0xb8, (byte) 0x14,
                (byte) 0xde, (byte) 0x5e, (byte) 0x0b, (byte) 0xdb },
            {
                (byte) 0xe0, (byte) 0x32, (byte) 0x3a, (byte) 0x0a,
                (byte) 0x49, (byte) 0x06, (byte) 0x24, (byte) 0x5c,
                (byte) 0xc2, (byte) 0xd3, (byte) 0xac, (byte) 0x62,
                (byte) 0x91, (byte) 0x95, (byte) 0xe4, (byte) 0x79 },
            {
                (byte) 0xe7, (byte) 0xc8, (byte) 0x37, (byte) 0x6d,
                (byte) 0x8d, (byte) 0xd5, (byte) 0x4e, (byte) 0xa9,
                (byte) 0x6c, (byte) 0x56, (byte) 0xf4, (byte) 0xea,
                (byte) 0x65, (byte) 0x7a, (byte) 0xae, (byte) 0x08 },
            {
                (byte) 0xba, (byte) 0x78, (byte) 0x25, (byte) 0x2e,
                (byte) 0x1c, (byte) 0xa6, (byte) 0xb4, (byte) 0xc6,
                (byte) 0xe8, (byte) 0xdd, (byte) 0x74, (byte) 0x1f,
                (byte) 0x4b, (byte) 0xbd, (byte) 0x8b, (byte) 0x8a },
            {
                (byte) 0x70, (byte) 0x3e, (byte) 0xb5, (byte) 0x66,
                (byte) 0x48, (byte) 0x03, (byte) 0xf6, (byte) 0x0e,
                (byte) 0x61, (byte) 0x35, (byte) 0x57, (byte) 0xb9,
                (byte) 0x86, (byte) 0xc1, (byte) 0x1d, (byte) 0x9e },
            {
                (byte) 0xe1, (byte) 0xf8, (byte) 0x98, (byte) 0x11,
                (byte) 0x69, (byte) 0xd9, (byte) 0x8e, (byte) 0x94,
                (byte) 0x9b, (byte) 0x1e, (byte) 0x87, (byte) 0xe9,
                (byte) 0xce, (byte) 0x55, (byte) 0x28, (byte) 0xdf },
            {
                (byte) 0x8c, (byte) 0xa1, (byte) 0x89, (byte) 0x0d,
                (byte) 0xbf, (byte) 0xe6, (byte) 0x42, (byte) 0x68,
                (byte) 0x41, (byte) 0x99, (byte) 0x2d, (byte) 0x0f,
                (byte) 0xb0, (byte) 0x54, (byte) 0xbb, (byte) 0x16 } };

    private static byte[][] inverseSBox =
        {
            {
                (byte) 0x52, (byte) 0x09, (byte) 0x6a, (byte) 0xd5,
                (byte) 0x30, (byte) 0x36, (byte) 0xa5, (byte) 0x38,
                (byte) 0xbf, (byte) 0x40, (byte) 0xa3, (byte) 0x9e,
                (byte) 0x81, (byte) 0xf3, (byte) 0xd7, (byte) 0xfb },
            {
                (byte) 0x7c, (byte) 0xe3, (byte) 0x39, (byte) 0x82,
                (byte) 0x9b, (byte) 0x2f, (byte) 0xff, (byte) 0x87,
                (byte) 0x34, (byte) 0x8e, (byte) 0x43, (byte) 0x44,
                (byte) 0xc4, (byte) 0xde, (byte) 0xe9, (byte) 0xcb },
            {
                (byte) 0x54, (byte) 0x7b, (byte) 0x94, (byte) 0x32,
                (byte) 0xa6, (byte) 0xc2, (byte) 0x23, (byte) 0x3d,
                (byte) 0xee, (byte) 0x4c, (byte) 0x95, (byte) 0x0b,
                (byte) 0x42, (byte) 0xfa, (byte) 0xc3, (byte) 0x4e },
            {
                (byte) 0x08, (byte) 0x2e, (byte) 0xa1, (byte) 0x66,
                (byte) 0x28, (byte) 0xd9, (byte) 0x24, (byte) 0xb2,
                (byte) 0x76, (byte) 0x5b, (byte) 0xa2, (byte) 0x49,
                (byte) 0x6d, (byte) 0x8b, (byte) 0xd1, (byte) 0x25 },
            {
                (byte) 0x72, (byte) 0xf8, (byte) 0xf6, (byte) 0x64,
                (byte) 0x86, (byte) 0x68, (byte) 0x98, (byte) 0x16,
                (byte) 0xd4, (byte) 0xa4, (byte) 0x5c, (byte) 0xcc,
                (byte) 0x5d, (byte) 0x65, (byte) 0xb6, (byte) 0x92 },
            {
                (byte) 0x6c, (byte) 0x70, (byte) 0x48, (byte) 0x50,
                (byte) 0xfd, (byte) 0xed, (byte) 0xb9, (byte) 0xda,
                (byte) 0x5e, (byte) 0x15, (byte) 0x46, (byte) 0x57,
                (byte) 0xa7, (byte) 0x8d, (byte) 0x9d, (byte) 0x84 },
            {
                (byte) 0x90, (byte) 0xd8, (byte) 0xab, (byte) 0x00,
                (byte) 0x8c, (byte) 0xbc, (byte) 0xd3, (byte) 0x0a,
                (byte) 0xf7, (byte) 0xe4, (byte) 0x58, (byte) 0x05,
                (byte) 0xb8, (byte) 0xb3, (byte) 0x45, (byte) 0x06 },
            {
                (byte) 0xd0, (byte) 0x2c, (byte) 0x1e, (byte) 0x8f,
                (byte) 0xca, (byte) 0x3f, (byte) 0x0f, (byte) 0x02,
                (byte) 0xc1, (byte) 0xaf, (byte) 0xbd, (byte) 0x03,
                (byte) 0x01, (byte) 0x13, (byte) 0x8a, (byte) 0x6b },
            {
                (byte) 0x3a, (byte) 0x91, (byte) 0x11, (byte) 0x41,
                (byte) 0x4f, (byte) 0x67, (byte) 0xdc, (byte) 0xea,
                (byte) 0x97, (byte) 0xf2, (byte) 0xcf, (byte) 0xce,
                (byte) 0xf0, (byte) 0xb4, (byte) 0xe6, (byte) 0x73 },
            {
                (byte) 0x96, (byte) 0xac, (byte) 0x74, (byte) 0x22,
                (byte) 0xe7, (byte) 0xad, (byte) 0x35, (byte) 0x85,
                (byte) 0xe2, (byte) 0xf9, (byte) 0x37, (byte) 0xe8,
                (byte) 0x1c, (byte) 0x75, (byte) 0xdf, (byte) 0x6e },
            {
                (byte) 0x47, (byte) 0xf1, (byte) 0x1a, (byte) 0x71,
                (byte) 0x1d, (byte) 0x29, (byte) 0xc5, (byte) 0x89,
                (byte) 0x6f, (byte) 0xb7, (byte) 0x62, (byte) 0x0e,
                (byte) 0xaa, (byte) 0x18, (byte) 0xbe, (byte) 0x1b },
            {
                (byte) 0xfc, (byte) 0x56, (byte) 0x3e, (byte) 0x4b,
                (byte) 0xc6, (byte) 0xd2, (byte) 0x79, (byte) 0x20,
                (byte) 0x9a, (byte) 0xdb, (byte) 0xc0, (byte) 0xfe,
                (byte) 0x78, (byte) 0xcd, (byte) 0x5a, (byte) 0xf4 },
            {
                (byte) 0x1f, (byte) 0xdd, (byte) 0xa8, (byte) 0x33,
                (byte) 0x88, (byte) 0x07, (byte) 0xc7, (byte) 0x31,
                (byte) 0xb1, (byte) 0x12, (byte) 0x10, (byte) 0x59,
                (byte) 0x27, (byte) 0x80, (byte) 0xec, (byte) 0x5f },
            {
                (byte) 0x60, (byte) 0x51, (byte) 0x7f, (byte) 0xa9,
                (byte) 0x19, (byte) 0xb5, (byte) 0x4a, (byte) 0x0d,
                (byte) 0x2d, (byte) 0xe5, (byte) 0x7a, (byte) 0x9f,
                (byte) 0x93, (byte) 0xc9, (byte) 0x9c, (byte) 0xef },
            {
                (byte) 0xa0, (byte) 0xe0, (byte) 0x3b, (byte) 0x4d,
                (byte) 0xae, (byte) 0x2a, (byte) 0xf5, (byte) 0xb0,
                (byte) 0xc8, (byte) 0xeb, (byte) 0xbb, (byte) 0x3c,
                (byte) 0x83, (byte) 0x53, (byte) 0x99, (byte) 0x61 },
            {
                (byte) 0x17, (byte) 0x2b, (byte) 0x04, (byte) 0x7e,
                (byte) 0xba, (byte) 0x77, (byte) 0xd6, (byte) 0x26,
                (byte) 0xe1, (byte) 0x69, (byte) 0x14, (byte) 0x63,
                (byte) 0x55, (byte) 0x21, (byte) 0x0c, (byte) 0x7d }

        };

    /**
     * The expanded key, as an array of words (int in Java).
     */
    private int[] expandedKey;

    /**
     * Round constant used in key expansion.
     */
    private int[] roundConstant;

    /**
     * Do implementation-specific initializations.
     */
    public WatneAES_Implementer()
    {
        // Initialize the roundConstant array. From Stallings, p. 155: "The
        // round constant is a word in which the three rightmost bits are always
        // 0... The round constant is different for each round and is defined as
        // Rcon[j] = (RC[j], 0, 0, 0), with RC[1] = 1, RC[j] = 2 * RC[j-1] and
        // with multiplication defined over the field GF(2^8)."
        // Since Java uses 0-based array indices, let the element for round n be
        // in array element (n-1).
        byte[] RC = new byte[KeySize.BITS256.getNumRounds()];
        // Define for maximum possible # of rounds.
        RC[0] = (byte) 1;

        for (int i = 1; i < RC.length; i++)
        {
            RC[i] = WatneAES_Implementer.getGFProduct(RC[i - 1], (byte) 2);
        } // for (int i = 1; i < RC.length; i++)

        // for (int i = 0; i < RC.length; i++)
        // {
        // System.out.printf("%2X ", RC[i]);
        // }
        //
        // System.out.println();

        // Initialize the roundConstant array.
        this.roundConstant = new int[RC.length];

        for (int i = 0; i < RC.length; i++)
        {
            this.roundConstant[i] = ((RC[i] << 24) & 0xff000000);
            // System.out.printf("%08X ", this.roundConstant[i]);
        } // for (int i = 0; i < RC.length; i++)

        // System.out.println();

    } // public WatneAES_Implementer()

    /**
     * Perform the matrix multiplication a * s, where addition is defined as the
     * XOR operation, and multiplication is over GF(8). Assume the first index
     * indicates the row, and the second indicates the column.
     * @param a the matrix on the left side of the multiplication operator.
     * @param s the matrix on the right side of the multiplication operator.
     * @return the result of the matrix multiplication a * s, where addition is
     * defined as the XOR operation, and multiplication is over GF(8).
     */
    public static byte[][] matrixMult(byte[][] a, byte[][] s)
    {
        // Define dimension of the result assuming a and s are both square
        // matrices.
        byte[][] result = new byte[s.length][s[0].length];

        for (int row = 0; row < s.length; row++)
        {
            // System.out.println("\nRow " + row);

            for (int col = 0; col < s[0].length; col++)
            {
                // System.out.println("\nCol" + col);

                result[row][col] = (byte) 0; // Initialize.

                for (int i = 0; i < result.length; i++)
                {
                    // System.out.printf("a[%d][%d]: %2X s[%d][%d]: %2X\n", row,
                    // i, a[row][i], i, row, s[i][col]);
                    // System.out.printf("product: %2X\n", WatneAES_Implementer
                    // .getGFProduct(a[row][i], s[i][row]));
                    result[row][col] ^=
                        WatneAES_Implementer.getGFProduct(a[row][i], s[i][col]);
                } // for (int i = 0; i < result.length; i++)

                // System.out.printf("Result[%d][%d]: %2X\n", row, col,
                // result[row][col]);
            } // for (int col = 0; col < s[0].length; col++)
        } // for (int row = 0; row < s.length; row++)

        return result;
    } // public static byte[][] matrixMult(byte[][] a, byte[][] s)

    /**
     * Get the round constant to use for key expansion for the specified round
     * number.
     * @param round the round for which the round constant is requested.
     * @return the round constant to use for key expansion for the specified
     * round number.
     */
    public int getRoundConstant(int round)
    {
        if ((round < 1) || (round > this.roundConstant.length))
            throw new ArrayIndexOutOfBoundsException();

        return this.roundConstant[round - 1];
    } // public int getRoundConstant(int round)

    /**
     * Given a byte value, give the corresponding value from the S-box. Per the
     * Stallings text (p. 147), "The leftmost 4 bits of the byte are used as a
     * row value and the rightmost 4 bits are used as a column value. These row
     * and column values serve as indexes into the S-box to select a unique
     * 8-bit output value.
     * @param original the byte to be mapped to a new byte value by the S-box.
     * @return the value read from the specified row and column in the S-box.
     */
    public static byte getSBoxValue(byte original)
    {
        // System.out.printf("original: %2x", original);

        // Leftmost 4 bits = row value.
        int row = ((original & 0xf0) >> 4);

        // System.out.printf("row: %2X\n", row);

        // Rightmost 4 bits = column value.
        int col = (original & 0x0f);

        // System.out.printf("col: %2X\n", col);
        // System.out.printf("S-box: %2X\n",
        // WatneAES_Implementer.sBox[row][col]);
        return WatneAES_Implementer.sBox[row][col];
    } // public byte getSBoxValue(byte original)

    /**
     * Given a byte value, give the byte value to which it is mapped by the
     * inverse S-box. Rows, columns, and values are determined in the same
     * manner as for the S-box; see getSBoxValue() documentation.
     * @param original the byte to be mapped to a new byte value by the inverse
     * S-box.
     * @return the value read from the specified row and column in the inverse
     * S-box.
     */
    public static byte getInverseSBoxValue(byte original)
    {
        // Leftmost 4 bits = row value.
        int row = ((original & 0xf0) >> 4);
        // Rightmost 4 bits = column value.
        int col = (original & 0x0f);
        return WatneAES_Implementer.inverseSBox[row][col];
    } // public static byte getInverseSBoxValue(byte original)

    /**
     * Given two numbers, give their product when multiplying in Galois Field
     * GF(2<sup>8</sup>), using irreducible polynomial x<sup>8</sup> + x<sup>4</sup> +
     * x<sup>3</sup> + x + 1.
     * @param num1
     * @param num2
     * @return
     */
    public static byte getGFProduct(byte num1, byte num2)
    {
        // Work with ints until returning final byte value. Bitwise and with
        // 0xff to ensure positive values. (Actually change num1 and num2,
        // rather than just in the product calculation, to aid debugging.)
        int num1int = num1 & 0xff;
        int num2int = num2 & 0xff;
        int product = 0;

        // Check ith bit of num1, left-shift num2 by i if bit is 1, and XOR
        // against product.
        for (int i = 0, j = 1; i < 8; i++, j *= 2)
        {
            if ((num1int & j & 0xff) > 0)
            {
                product ^= (num2int << i);
            } // if ((num1int & j & 0xff) > 0)
        } // for (int i = 0; i < 8; i++)

        // int product = num1int * num2int; << Erroneous first attempt.

        while (product > WatneAES_Implementer.MAX_BYTE_VAL)
        {
            int multiplierCheck = WatneAES_Implementer.TWO_TO_THE_EIGHTH;
            int multiplier = product / multiplierCheck;
            // Get whole number division of product by highest bit of
            // GF_POLYNOMIAL.
            int divisor = WatneAES_Implementer.GF_POLYNOMIAL;

            while (multiplier > 1)
            {
                divisor <<= 1; // Left shift 1 byte
                multiplierCheck <<= 1; // Also the multiplier check.
                multiplier = product / multiplierCheck;
            } // while (multiplier > 1)

            product ^= divisor; // Subtract / XOR divisor from product.
        } // while (product > WatneAES_Implementer.MAX_BYTE_VAL)

        return (byte) product;
    } // public static byte getGFProduct(byte num1, byte num2)

    /**
     * Given the current state of the block being processed, convert the block
     * to a 4x4 array of bytes and map &quot;each byte of a column... into a new
     * value that is a function of all four bytes in that column,&quot; per
     * Stallings, p. 151.
     * @param state the block of bytes before the forward mix columns
     * transformation.
     * @return the block of bytes after the forward mix columns transformation.
     */
    public static byte[] mixColumns(byte[] state)
    {
        byte[][] resultGrid =
            WatneAES_Implementer.matrixMult(WatneAES_Implementer.fwMixCol,
                AES_Utilities.makeFourByFourByteArray(state));

        return AES_Utilities.linearizeFourByFourByteArray(resultGrid);
    } // public static byte[] mixColumns(byte[] state)

    /**
     * Perform the inverse mix columns transformation.
     * @param state the block of bytes before the inverse mix columns
     * transformation.
     * @return the block of bytes after the inverse mix columns transformation.
     */
    public static byte[] inverseMixColumns(byte[] state)
    {
        byte[][] resultGrid =
            WatneAES_Implementer.matrixMult(WatneAES_Implementer.invMixCol,
                AES_Utilities.makeFourByFourByteArray(state));

        return AES_Utilities.linearizeFourByFourByteArray(resultGrid);
    } // public static byte[] inverseMixColumns(byte[] state)

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#decrypt()
     */
    public void decrypt() throws Exception
    {
        byte[] priorEncryptedBlock = null; // Used for CBC mode.

        if (this.getMode() == Mode.CBC)
        {
            if (this.getInitializationVector() == null)
            {
                throw new InvalidAlgorithmParameterException(
                    "ERROR: Uninitialized Initialization Vector.");
            }

            priorEncryptedBlock = new byte[AES_Common.BLOCKSIZE];
            System.arraycopy(this.getInitializationVector(), 0,
                priorEncryptedBlock, 0, AES_Common.BLOCKSIZE);
        } // if (this.getMode() == Mode.CBC)

        // Create work array for storing encrypted text.
        byte[] work = new byte[this.getCipherBytes().length];
        System.arraycopy(this.getCipherBytes(), 0, work, 0, this
            .getCipherBytes().length);

        // Loop through the blocks in encrypted text.
        for (int i = 0; i < work.length; i +=
            this.getKeySize().getBlockSizeBytes())
        {
            // Create array for processing current block.
            byte[] currBlock = new byte[this.getKeySize().getBlockSizeBytes()];

            // Get the current block to process, depending on mode used.
            switch (this.getMode())
            {
                case ECB:
                case CBC: /* Same as ECB. */
                default: /* Same as for ECB & CBC. */
                    // Copy the current block to currBlock.
                    System.arraycopy(work, i, currBlock, 0, this.getKeySize()
                        .getBlockSizeBytes());
            } // switch (this.getMode())

            // Create a copy of the current encrypted block.
            byte[] saveBlock = new byte[AES_Common.BLOCKSIZE];
            System.arraycopy(currBlock, 0, saveBlock, 0, AES_Common.BLOCKSIZE);

            // Process the current block.
            currBlock = this.decryptBlock(currBlock);

            // Update work[], based on Mode.
            switch (this.getMode())
            {
                case ECB:
                    // Just append current block.
                    System.arraycopy(currBlock, 0, work, i, this.getKeySize()
                        .getBlockSizeBytes());
                    break; // case ECB
                case CBC:
                    // XOR with previous encrypted block.
                    for (int b = 0; b < currBlock.length; b++)
                    {
                        currBlock[b] ^= (0xff & priorEncryptedBlock[b]);
                    } // for (int b = 0; b < currBlock.length; b++)
                    // Append current block.
                    System.arraycopy(currBlock, 0, work, i, this.getKeySize()
                        .getBlockSizeBytes());
                    // Update priorEncryptedBlock for next round.
                    priorEncryptedBlock = saveBlock;
                    break; // case CBC
                default:
                    throw new NoSuchAlgorithmException("ERROR: "
                                    + this.getMode()
                                    + " mode not supported at this time.");
            } // switch (this.getMode())
        } // for (int i = 0...getBlockSizeBytes()

        // Remove padding from last block and store final decrypted array.
        this.setPlainText(AES_Utilities.getUnpadded(work, this.getPadding()));
    } // public void decrpt()

    /**
     * Helper method that allows using common code for decrypting blocks of text
     * from either byte arrays or files.
     * @param currBlock the current block to be decrypted.
     * @return the decrypted block. The calling method must handle additional
     * operations if using a mode other than ECB.
     */
    private byte[] decryptBlock(byte[] currBlock)
    {
        // Process the current block.

        // Note Java int = word in Stallings. (Source:
        // http://en.wikipedia.org/wiki/Integer_(computer_science))

        // Decryption: count rounds down from maximum to 0.
        currBlock = addRoundKey(currBlock, this.getKeySize().getNumRounds());

        for (int round = this.getKeySize().getNumRounds() - 2; round > -1; round--)
        {
            // For the first n-1 rounds, do all four steps:
            currBlock = WatneAES_Implementer.inverseShiftRows(currBlock);
            currBlock = WatneAES_Implementer.inverseSubstituteBytes(currBlock);
            currBlock = addRoundKey(currBlock, (round + 1));
            currBlock = WatneAES_Implementer.inverseMixColumns(currBlock);
        } // for (int round = getNumRounds() - 1... 0)

        // For the final round, do only three steps:
        currBlock = WatneAES_Implementer.inverseShiftRows(currBlock);
        currBlock = WatneAES_Implementer.inverseSubstituteBytes(currBlock);
        currBlock = addRoundKey(currBlock, 0);
        return currBlock;
    } // private byte[] decryptBlock(byte[] currBlock)

    /**
     * Expand the key for use in multiple rounds. Algorithm based on pseudocode
     * in Stallings textbook, Chapter 5, page 154.
     */
    private void expandKey()
    {
        // Create a new array of the appropriate size for the given key size.
        this.expandedKey = new int[this.getKeySize().getExpandedKeyWords()];
        int temp;

        /** * TESTING: CHECK ROUND 0 ** */
        // byte[] b = this.getKey();
        // System.out.printf("\n%2X %2X %2X %2X\n", b[0], b[1], b[2], b[3]);
        /** * END TESTING ** */

        for (int i = 0; i < 4; i++)
        {
            this.expandedKey[i] =
                AES_Utilities.getWordFromBytes(this.getKey()[4 * i], this
                    .getKey()[4 * i + 1], this.getKey()[4 * i + 2], this
                    .getKey()[4 * i + 3]);
        } // for (int i = 0; i < 4; i++)

        /** * TESTING: SHOW ROUND 0 KEY ** */
        // for (int i = 0; i < 4; i++)
        // System.out.printf("%1d %8X\n", i, this.expandedKey[i]);
        /** * END TESTING ** */

        for (int i = 4; i < this.getKeySize().getExpandedKeyWords(); i++)
        {
            // System.out.println("\ni = " + i);
            temp = (0xffffffff & this.expandedKey[i - 1]);

            if (i % 4 == 0)
            {
                // System.out.printf("rotWord: %08X\n", WatneAES_Implementer
                // .rotWord(temp));
                // System.out.printf("subWord: %08X\n", WatneAES_Implementer
                // .subWord(WatneAES_Implementer.rotWord(temp)));
                // System.out.printf("roundConstant: %08X\n", this
                // .getRoundConstant(i / 4));
                temp =
                    WatneAES_Implementer.subWord(WatneAES_Implementer
                        .rotWord(temp))
                                    ^ this.getRoundConstant(i / 4);
                // System.out.printf("temp: %08X\n", temp);
                // System.out.printf("expandedKey[%d]: %08X\n", (i - 4),
                // this.expandedKey[i - 4]);
                // System.out.printf("temp: %08X\n", temp);
            } // if (i % 4 == 0)

            this.expandedKey[i] = this.expandedKey[i - 4] ^ temp;
            // System.out
            // .printf("expandedKey[%d]: %08X\n", i, this.expandedKey[i]);
        } // for (int i = 4; i < this.getKeySize().getExpandedKeyWords(); i++)
    } // private void expandKey()

    /**
     * Perform a one-byte circular left shift on the given int/word.
     * @param word the original int/word.
     * @return the word circularly shifted one byte to the left.
     */
    public static int rotWord(int word)
    {
        return ((0xff & ((word & 0xff000000) >> 24)) | ((0x00ffffff & word) << 8));
    } // public int rotWord(int word)

    /**
     * Per Stallings text, p. 155, perform &quot;a byte substitution on each
     * byte of its input word, using the S-box&quot;.
     * @param word the int/word whose bytes are to be changed using the S-box.
     * @return the byte/word with each byte substituted using the S-box.
     */
    public static int subWord(int word)
    {
        word &= 0xffffffff; // Make positive value.

        // System.out.printf("\n%8X\n", word);

        byte byte1 = (byte) ((word & 0xff000000) >> 24);
        byte byte2 = (byte) ((word & 0x00ff0000) >> 16);
        byte byte3 = (byte) ((word & 0x0000ff00) >> 8);
        byte byte4 = (byte) (word & 0x000000ff);

        // System.out.printf("%2X %2X %2X %2X\n", byte1, byte2, byte3, byte4);

        byte1 = WatneAES_Implementer.getSBoxValue(byte1);
        byte2 = WatneAES_Implementer.getSBoxValue(byte2);
        byte3 = WatneAES_Implementer.getSBoxValue(byte3);
        byte4 = WatneAES_Implementer.getSBoxValue(byte4);

        // System.out.printf("%2X %2X %2X %2X\n", byte1, byte2, byte3, byte4);
        // System.out.printf("%8X\n", (0xff000000 & (byte1 << 24))
        // | (0x00ff0000 & (byte2 << 16))
        // | (0x0000ff00 & (byte3 << 8)) | (0x000000ff & byte4));

        return (0xff000000 & (byte1 << 24)) | (0x00ff0000 & (byte2 << 16))
                        | (0x0000ff00 & (byte3 << 8)) | (0x000000ff & byte4);
    } // public static int subWord(int word)

    /**
     * Add the round key for the specified round to the current block.
     * @param currBlock a block of data being encrypted or decrypted.
     * @param round the round number, ranging from 0 for round 1 to (numRounds -
     * 1) for the last round in encryption, and (numRounds - 1) for the first
     * round to 0 for the last round when decrypting.
     * @return the currBlock with the round key added to it.
     */
    public byte[] addRoundKey(byte[] currBlock, int round)
    {
        // Translate currBlock to an array of ints (words), then XOR the
        // corresponding round key. Plaintext block is
        // always 128 bits = 4 words, so use fixed array size of 4 ints.
        int[] blockWords = new int[4]; // currBlock translated to words/ints.

        for (int i = 0; i < 4; i++)
        {
            blockWords[i] =
                AES_Utilities.getWordFromBytes(currBlock[4 * i],
                    currBlock[4 * i + 1], currBlock[4 * i + 2],
                    currBlock[4 * i + 3]);
            // XOR the portion of the round key.
            blockWords[i] ^= this.expandedKey[round * 4 + i];

            /** * TEST ** */
            // System.out.println("\nRound " + round);
            // System.out.printf("blockWords[%1d]: %08X\n", i, blockWords[i]);
            /** * END TEST ** */

            // Convert ints back to words and put in appropriate portion of
            // currBlock.
            System.arraycopy(AES_Utilities.getBytesFromWord(blockWords[i]), 0,
                currBlock, i * 4, 4);
        } // for (int i = 0; i < 4; i++)

        return currBlock;
    } // public byte[] addRoundKey(byte[] currBlock, int round)

    /**
     * Perform the byte substitution for the given state block, using the S-box.
     * @param block the block of bytes on which the byte substitution is to be
     * performed.
     * @return the block of bytes, with each byte replaced by the appropriate
     * value from the S-box.
     */
    public static byte[] substituteBytes(byte[] block)
    {
        for (int i = 0; i < block.length; i++)
            block[i] = WatneAES_Implementer.getSBoxValue(block[i]);

        return block;
    } // public static byte[] substituteBytes(byte[] block)

    /**
     * Perform the byte substitution for the given state block, using the
     * inverse S-box.
     * @param block the block of byhtes on which the byte substitution is to be
     * performed.
     * @return the block of bytes, with each byte replaced by the appropriate
     * value from the inverse S-box.
     */
    public static byte[] inverseSubstituteBytes(byte[] block)
    {
        for (int i = 0; i < block.length; i++)
        {
            block[i] = WatneAES_Implementer.getInverseSBoxValue(block[i]);
        }

        return block;
    } // public static byte[] inverseSubstituteBytes(byte[] block)

    /**
     * Perform a series of row shifts on the given block, once it is transformed
     * to a 4x4 matrix of bytes. The rows are treated the same as by the
     * shiftRows() method, but with right shifts performed in place of left
     * shifts.
     * @param block the current state array.
     * @return the new state, after performing the row shifts.
     */
    public static byte[] inverseShiftRows(byte[] block)
    {
        // Convert to 4x4 grid of bytes.
        byte[][] grid = AES_Utilities.makeFourByFourByteArray(block);

        // Row 1 (2nd row): 1-byte circular right shift.
        byte temp = grid[1][3]; // Save last value.

        for (int col = 3; col > 0; col--)
        {
            grid[1][col] = grid[1][col - 1];
        }

        grid[1][0] = temp; // Copy original rightmost byte.

        // Row 2 (3rd row): 2-byte circular right shift.
        temp = grid[2][3];
        byte temp2 = grid[2][2];

        for (int col = 3; col > 1; col--)
        {
            grid[2][col] = grid[2][col - 2];
        }

        // Restore remaining bytes.
        grid[2][1] = temp;
        grid[2][0] = temp2;

        // Row 3 (4th row): 3-byte circular right shift = 1-byte circular left
        // shift.
        temp = grid[3][0]; // Save.

        for (int col = 0; col < 3; col++)
        {
            grid[3][col] = grid[3][col + 1];
        }

        grid[3][3] = temp; // Restore last byte.

        return AES_Utilities.linearizeFourByFourByteArray(grid);
        // Convert back to linear array and return.
    } // public static byte[] inverseShiftRows(byte[] block)

    /**
     * Perform a series of row shifts on the given block, once it is transformed
     * to a 4x4 matrix of bytes. Per Stallings, p. 150, &quot;The first row...
     * is not altered. For the second row, a 1-byte circular left shift is
     * performed. For the third row, a 2-byte circular left shift is performed.
     * For the fourth row, a 3-byte circular left shift is performed.&quot;
     * @param block the current state array.
     * @return the new state, after performing the row shifts.
     */
    public static byte[] shiftRows(byte[] block)
    {
        // Convert to 4x4 grid of bytes.
        byte[][] grid = AES_Utilities.makeFourByFourByteArray(block);

        // Row 1 (2nd row): 1-byte circular left shift.
        byte temp = grid[1][0]; // Save first value.

        for (int col = 0; col < 3; col++)
        {
            grid[1][col] = grid[1][col + 1];
        }

        grid[1][3] = temp; // Copy original leftmost byte.

        // Row 2 (3rd row): 2-byte circular left shift.
        temp = grid[2][0];
        byte temp2 = grid[2][1];

        for (int col = 0; col < 2; col++)
        {
            grid[2][col] = grid[2][col + 2];
        }

        // Restore remaining bytes.
        grid[2][2] = temp;
        grid[2][3] = temp2;

        // Row 3 (4th row): 3-byte circular left shift = 1-byte circular right
        // shift.
        temp = grid[3][3]; // Save.

        for (int col = 3; col > 0; col--)
        {
            grid[3][col] = grid[3][col - 1];
        }

        grid[3][0] = temp; // Restore last byte.

        return AES_Utilities.linearizeFourByFourByteArray(grid);
        // Convert back to linear array and return.
    } // public static byte[] shiftRows(byte[] block)

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#encrypt()
     */
    public void encrypt() throws Exception
    {
        byte[] priorEncryptedBlock = null; // Used for CBC mode.

        if (this.getMode() == Mode.CBC)
        {
            if (this.getInitializationVector() == null)
            {
                throw new InvalidAlgorithmParameterException(
                    "Uninitialized initialization vector.");
            }

            priorEncryptedBlock = new byte[AES_Common.BLOCKSIZE];
            System.arraycopy(this.getInitializationVector(), 0,
                priorEncryptedBlock, 0, AES_Common.BLOCKSIZE);
        } // if (this.getMode() == Mode.CBC)

        byte[] paddedPlaintext =
            AES_Utilities.getPadded(this.getPlainTextBytes(),
                this.getPadding(), this.getKeySize());
        // Get the padded version of the plaintext.

        // System.out.println("paddedPlaintext:\n"
        // + AES_Utilities.getBase64Text(paddedPlaintext));

        // Create work array for storing encrypted text.
        byte[] encrypted = new byte[paddedPlaintext.length];

        // Loop through the blocks in paddedPlainText.
        for (int i = 0; i < paddedPlaintext.length; i +=
            this.getKeySize().getBlockSizeBytes())
        {
            // Create array for processing current block.
            byte[] currBlock = new byte[this.getKeySize().getBlockSizeBytes()];

            // Copy the current block to currBlock.
            System.arraycopy(paddedPlaintext, i, currBlock, 0, this
                .getKeySize().getBlockSizeBytes());

            if (this.getMode() == Mode.CBC)
            {
                // Encrypt the XOR of the current plaintext block and the
                // previous encrypted block.
                for (int b = 0; b < currBlock.length; b++)
                {
                    currBlock[b] ^= (0xff & priorEncryptedBlock[b]);
                } // for (int b = 0; b < currBlock.length; b++)
            } // if (this.getMode() == Mode.CBC)

            currBlock = this.encryptBlock(currBlock);

            switch (this.getMode())
            {
                case ECB:
                    // Just append current block.
                    System.arraycopy(currBlock, 0, encrypted, i, this
                        .getKeySize().getBlockSizeBytes());
                    break; // case ECB
                case CBC:
                    // Append current block, then save current block for the
                    // next round.
                    System.arraycopy(currBlock, 0, encrypted, i, this
                        .getKeySize().getBlockSizeBytes());
                    System.arraycopy(currBlock, 0, priorEncryptedBlock, 0,
                        currBlock.length);
                    break;
                default:
                    throw new NoSuchAlgorithmException(this.getMode()
                                    + " mode not supported at this time.");
            } // switch (this.getMode())
        } // for (int i = 0...getBlockSizeBytes()

        // Store final encrypted array.
        this.setCipherBytes(encrypted);
    } // public void encrypt() throws Exception

    /**
     * Helper method that allows using common code for encrypting blocks of text
     * from either byte arrays or files.
     * @param currBlock the current block to be encrypted.
     * @return the encrypted block. The calling method must handle additional
     * operations if using a mode other than ECB.
     */
    private byte[] encryptBlock(byte[] currBlock)
    {
        // Process the current block.

        // Note Java int = word in Stallings. (Source:
        // http://en.wikipedia.org/wiki/Integer_(computer_science))

        currBlock = addRoundKey(currBlock, 0);

        // System.out.println("addRoundKey 0:\n"
        // + AES_Utilities.getBase64Text(currBlock) + "\n");

        for (int round = 0; round < this.getKeySize().getNumRounds() - 1; round++)
        {
            // For the first n-1 rounds, do all four steps:
            currBlock = WatneAES_Implementer.substituteBytes(currBlock);
            currBlock = WatneAES_Implementer.shiftRows(currBlock);
            currBlock = WatneAES_Implementer.mixColumns(currBlock);
            currBlock = addRoundKey(currBlock, (round + 1));

            // System.out
            // .println("addRoundKey " + (round + 1) + ":\n"
            // + AES_Utilities.getBase64Text(currBlock)
            // + "\n");

        } // for (int round = 0...getNumRounds() - 1)

        // For the final round, do only three steps:
        currBlock = WatneAES_Implementer.substituteBytes(currBlock);
        currBlock = WatneAES_Implementer.shiftRows(currBlock);
        currBlock = addRoundKey(currBlock, this.getKeySize().getNumRounds());

        // System.out.println("addRoundKey "
        // + (this.getKeySize().getNumRounds()) + ":\n"
        // + AES_Utilities.getBase64Text(currBlock) + "\n");
        //
        return currBlock;
    } // private byte[] encryptBlock(byte[] currBlock)

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#generateKey()
     */
    public void generateKey() throws Exception
    {
        // Very simple key-generation algorithm, just setting each byte required
        // to a random number.
        int numBytes = this.getKeySize().getKeySizeBytes();
        // Number of bytes that need to be populated.
        byte[] key = new byte[numBytes]; // Create numBytes-element array of
        // bytes.

        SecureRandom generator = new SecureRandom();
        // Create "a cryptographically strong random number generator (RNG)" -
        // Java Platform SE 6 documentation,
        // http://java.sun.com/javase/6/docs/api/
        generator.nextBytes(key); // Populate the key with random bytes.
        this.setKey(key); // Store the key as this object's key.
    } // public void generateKey() throws Exception

    /**
     * Override the default setKey() method to also perform the key expansion at
     * the same time.
     */
    public void setKey(byte[] key)
    {
        super.setKey(key);
        this.expandKey();
    } // public void setKey(byte[] key)

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#getImplementationType()
     */
    public String getImplementationType()
    {
        return "AES implementation using code by John Watne, based on "
                        + "the outline in the Stallings text";
    }
} // public class WatneAES_Implementer
