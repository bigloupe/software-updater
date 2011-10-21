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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;

/**
 * This class contains logic shared by all AES implementations created for this
 * class project.
 * @author John Watne
 * 
 */
public abstract class AES_Common implements AES_Implementable
{
    /**
     * Number of bytes in each block (16 for all key sizes).
     */
    public static final int BLOCKSIZE = 16;

    protected KeySize keySize = KeySize.BITS128;

    protected Mode mode = Mode.ECB;

    protected Padding padding = Padding.PKCS5PADDING;

    protected String plaintextString = null;

    protected byte[] plaintextBytes = null;

    protected String cipherTextString = null;

    protected byte[] cipherTextBytes = null;

    protected String cipherTextHexadecimal = null;

    protected String cipherTextBase64 = null;

    protected byte[] keyBytes = null;

    protected byte[] initializationVector = null;

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#getInitializationVectorBase64()
     */
    @Override
    public String getInitializationVectorBase64()
    {
        return AES_Utilities.getBase64Text(this.initializationVector);
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#setInitializationVectorBase64(java.lang.String)
     */
    @Override
    public void setInitializationVectorBase64(String ivBase64)
                                                              throws IOException,
                                                              InvalidAlgorithmParameterException
    {
        this
            .setInitializationVector(AES_Utilities.getBytesFromBase64(ivBase64));

        if (this.initializationVector.length != AES_Common.BLOCKSIZE)
        {
            throw new InvalidAlgorithmParameterException(
                "Invalid initialization vector length "
                                + this.initializationVector.length
                                + " bytes, should be " + AES_Common.BLOCKSIZE);
        } // if (this.initializationVector.length != AES_Common.BLOCKSIZE)
    } // public void setInitializationVectorBase64(...)

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#getCipherBase64()
     */
    public String getCipherBase64()
    {
        return this.cipherTextBase64;
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#getCipherBytes()
     */
    public byte[] getCipherBytes()
    {
        return this.cipherTextBytes;
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#getCipherHexadecimal()
     */
    public String getCipherHexadecimal()
    {
        return this.cipherTextHexadecimal;
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#getKey()
     */
    public byte[] getKey()
    {
        return this.keyBytes;
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#getKeySize()
     */
    public KeySize getKeySize()
    {
        return this.keySize;
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#getMode()
     */
    public Mode getMode()
    {
        return this.mode;
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#getPadding()
     */
    public Padding getPadding()
    {
        return this.padding;
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#getPlainText()
     */
    public String getPlainText()
    {
        return this.plaintextString;
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#getPlainTextBytes()
     */
    public byte[] getPlainTextBytes()
    {
        return this.plaintextBytes;
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#setCipherBase64(java.lang.String)
     */
    public void setCipherBase64(String base64CipherText) throws IOException
    {
        this.cipherTextBase64 = base64CipherText;
        this.cipherTextBytes =
            AES_Utilities.getBytesFromBase64(base64CipherText);
        this.cipherTextString = new String(this.cipherTextBytes);
        this.cipherTextHexadecimal =
            AES_Utilities.getHexString(this.cipherTextBytes);
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#setCipherBytes(byte[])
     */
    public void setCipherBytes(byte[] cipherText)
    {
        this.cipherTextBytes = cipherText;
        this.cipherTextString = new String(this.cipherTextBytes);
        this.cipherTextHexadecimal =
            AES_Utilities.getHexString(this.cipherTextBytes);
        this.cipherTextBase64 =
            AES_Utilities.getBase64Text(this.cipherTextBytes);
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#setCipherHexadecimal(java.lang.String)
     */
    public void setCipherHexadecimal(String hexCipherText)
    {
        this.cipherTextHexadecimal = hexCipherText;
        this.cipherTextBytes = AES_Utilities.getBytesFromHex(hexCipherText);
        this.cipherTextString = new String(this.cipherTextBytes);
        this.cipherTextBase64 =
            AES_Utilities.getBase64Text(this.cipherTextBytes);
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#setKey(byte[])
     */
    public void setKey(byte[] key)
    {
        this.keyBytes = key;
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#setKeySize(watne.seis720.project.KeySize)
     */
    public void setKeySize(KeySize keySize)
    {
        this.keySize = keySize;

    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#setMode(watne.seis720.project.Mode)
     */
    public void setMode(Mode mode)
    {
        this.mode = mode;
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#setPadding(watne.seis720.project.Padding)
     */
    public void setPadding(Padding pad)
    {
        this.padding = pad;
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#setPlainText(java.lang.String)
     */
    public void setPlainText(String text) throws UnsupportedEncodingException
    {
        this.plaintextString = text;
        // Get bytes, assuming UTF8 encoding.
        this.setPlainText(text.getBytes("UTF8"));
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#setPlainText(byte[])
     */
    public void setPlainText(byte[] text)
    {
        this.plaintextBytes = text;
        this.plaintextString = new String(text);
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#getCipherTextString()
     */
    public String getCipherTextString()
    {
        return this.cipherTextString;
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#setCipherTextString(java.lang.String)
     */
    public void setCipherTextString(String text)
    {
        this.cipherTextString = text;
        this.cipherTextBytes = text.getBytes();
        this.cipherTextHexadecimal =
            AES_Utilities.getHexString(this.cipherTextBytes);
        this.cipherTextBase64 =
            AES_Utilities.getBase64Text(this.cipherTextBytes);
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#encryptFile(java.io.File,
     * java.io.File)
     */
    @Override
    public void encryptFile(File inputFile, File outputFile) throws Exception
    {
        byte[] originalIV = null; // Used for CBC mode.

        if (this.getMode() == Mode.CBC)
        {
            if (this.getInitializationVector() == null)
            {
                throw new InvalidAlgorithmParameterException(
                    "Uninitialized initialization vector.");
            }

            originalIV = new byte[AES_Common.BLOCKSIZE];
            System.arraycopy(this.getInitializationVector(), 0, originalIV, 0,
                AES_Common.BLOCKSIZE);
        } // if (this.getMode() == Mode.CBC)

        // Create BufferedInputStream for reading inputFile.
        BufferedInputStream reader =
            new BufferedInputStream(new FileInputStream(inputFile));

        if (outputFile.exists() && !AES_Utilities.wishToOverwrite(outputFile))
        {
            throw new Exception("No encryption performed.");
        }
        else
        {
            // Create BufferedOUtputStream for writing to output file.
            BufferedOutputStream writer =
                new BufferedOutputStream(new FileOutputStream(outputFile));
            // Create array for storing current block read from input.
            byte[] currBlock = new byte[BLOCKSIZE];
            // operation.
            Mode mode = this.getMode(); // Save the requested mode.
            // Temporarily set mode to ECB, and handle any additional XOR logic
            // for other modes in this method.
            this.setMode(Mode.ECB);
            Padding padding = this.getPadding(); // Save requested padding.
            // Temporarily set padding to no padding, for all but the last
            // block.
            this.setPadding(Padding.NOPADDING);
            int bytesRead = reader.read(currBlock, 0, BLOCKSIZE);
            // Bytes read from file.

            while (bytesRead >= 0)
            {
                // Save reference to current currBlock array.
                byte[] save = currBlock;

                // Keep reading the file until get to the end.
                if (reader.available() <= 0)
                {
                    // If there are no bytes left to read from the input file,
                    // then this is the last block, so pad it.
                    this.setPadding(padding); // Restore original padding.

                    // Only read bytesRead bytes from file -- resize currBlock
                    // accordingly.
                    byte[] tmp = new byte[bytesRead];
                    // Copy the bytes read to the temporary array.
                    System.arraycopy(currBlock, 0, tmp, 0, bytesRead);
                    currBlock = tmp; // Point currBlock to the resized array.

                    // currBlock =
                    // AES_Utilities.getPadded(currBlock, this.getPadding(),
                    // this.getKeySize());
                } // if (reader.available() <= 0)

                // Encrypt the block.
                this.setPlainText(currBlock);
                this.encrypt();

                // Update encrypted[], based on Mode.
                switch (mode)
                {
                    case ECB:
                        // Just write the most recently encrypted block to
                        // output.
                        writer.write(this.getCipherBytes(), 0, this
                            .getCipherBytes().length);
                        break;
                    case CBC:
                        // Write the block.
                        writer.write(this.getCipherBytes(), 0, this
                            .getCipherBytes().length);

                        // Reset IV to cipher block if more bytes left to read.
                        if (reader.available() > 0)
                        {
                            this.setInitializationVector(this.getCipherBytes());
                        }

                        break;
                    default:
                        throw new InvalidAlgorithmParameterException(mode
                                        + " mode not supported at this time.");
                } // switch (mode)

                // Restore original currBlock, to ensure array of size BLOCKSIZE
                // bits.
                currBlock = save;
                // Read next block.
                bytesRead = reader.read(currBlock, 0, BLOCKSIZE);
            } // while [bytesRead >= 0]

            if (mode == Mode.CBC)
            {
                // Restore original initialization vector.
                this.setInitializationVector(originalIV);
            } // if (mode == Mode.CBC)

            this.setMode(mode); // Restore original mode.
            writer.close(); // Close output stream.
        } // else [not overwriting or ok to overwrite existing file]

        reader.close(); // Close input stream.
    } // public void encryptFile(File inputFile, File outputFile)

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#decryptFile(java.io.File,
     * java.io.File)
     */
    @Override
    public void decryptFile(File inputFile, File outputFile)
                                                            throws HeadlessException,
                                                            IOException,
                                                            Exception
    {
        byte[] originalIV = null; // Used for CBC mode.
        byte[] saveBlock = new byte[AES_Common.BLOCKSIZE]; // Store most
        // recently used
        // encrypted block.

        if (this.getMode() == Mode.CBC)
        {
            if (this.getInitializationVector() == null)
            {
                throw new InvalidAlgorithmParameterException(
                    "Uninitialized initialization vector.");
            }

            originalIV = new byte[AES_Common.BLOCKSIZE];
            System.arraycopy(this.getInitializationVector(), 0, originalIV, 0,
                AES_Common.BLOCKSIZE);
        } // if (this.getMode() == Mode.CBC)

        // Create BufferedInputStream for reading inputFile.
        BufferedInputStream reader =
            new BufferedInputStream(new FileInputStream(inputFile));

        if (outputFile.exists() && !AES_Utilities.wishToOverwrite(outputFile))
        {
            System.out.println("\nNo decryption performed.");
        }
        else
        {
            // Create BufferedOUtputStream for writing to output file.
            BufferedOutputStream writer =
                new BufferedOutputStream(new FileOutputStream(outputFile));
            // Create array for storing current block read from input.
            byte[] currBlock = new byte[BLOCKSIZE];
            // operation.
            Mode mode = this.getMode(); // Save the requested mode.
            // Temporarily set mode to ECB, and handle any additional XOR logic
            // for other modes in this method.
            this.setMode(Mode.ECB);
            Padding padding = this.getPadding(); // Save requested padding.
            // Temporarily set padding to no padding, for all but the last
            // block.
            this.setPadding(Padding.NOPADDING);

            while (reader.read(currBlock, 0, BLOCKSIZE) >= 0)
            {
                // Keep reading the file until get to the end.
                if (reader.available() <= 0)
                {
                    // If there are no bytes left to read from the input file,
                    // then this is the last block, so unpad it.
                    this.setPadding(padding); // Restore original padding.
                } // if (reader.available() <= 0)

                // Encrypt the block.
                this.setCipherBytes(currBlock);

                if (this.getMode() == Mode.CBC)
                {
                    // Save a copy of the current encrypted block.
                    System.arraycopy(currBlock, 0, saveBlock, 0,
                        AES_Common.BLOCKSIZE);
                }

                this.decrypt();

                // Update encrypted[], based on Mode.
                switch (mode)
                {
                    case ECB:
                        // Just write the most recently decrypted block to
                        // output.
                        writer.write(this.getPlainTextBytes(), 0, this
                            .getPlainTextBytes().length);
                        break;
                    case CBC:
                        // Write the most recently decrypted block.
                        writer.write(this.getPlainTextBytes(), 0, this
                            .getPlainTextBytes().length);
                        // Reset initialization vector to last encrypted block.
                        this.setInitializationVector(saveBlock);
                        break;
                    default:
                        throw new InvalidAlgorithmParameterException(mode
                                        + " mode not supported at this time.");
                } // switch (mode)
            } // while [bytesRead >= 0]

            this.setMode(mode); // Restore original mode.

            if (mode == Mode.CBC)
            {
                // Restore original initialization vector.
                this.setInitializationVector(originalIV);
            } // if (mode == Mode.CBC)

            writer.close(); // Close output stream.
        } // else [not overwriting or ok to overwrite existing file]

        reader.close(); // Close input stream.
    } // public void decryptFile(File inputFile, File outputFile)

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#getInitializationVector()
     */
    @Override
    public byte[] getInitializationVector()
    {
        return this.initializationVector;
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#setInitializationVector(byte[])
     */
    @Override
    public void setInitializationVector(byte[] iv)
                                                  throws InvalidAlgorithmParameterException
    {
        if (iv.length != AES_Common.BLOCKSIZE)
        {
            throw new InvalidAlgorithmParameterException(
                "Invalid iv blocksize " + iv.length + "; should be "
                                + AES_Common.BLOCKSIZE);
        }

        // Copy, rather than point to same copy of iv.
        this.initializationVector = new byte[AES_Common.BLOCKSIZE];
        System.arraycopy(iv, 0, this.initializationVector, 0,
            AES_Common.BLOCKSIZE);
    }

    /*
     * (non-Javadoc)
     * @see watne.seis720.project.AES_Implementable#generateInitializationVector()
     */
    @Override
    public void generateInitializationVector()
                                              throws InvalidAlgorithmParameterException
    {
        byte[] iv = new byte[AES_Common.BLOCKSIZE]; // Create array for
        // initialization vector.
        SecureRandom generator = new SecureRandom();
        generator.nextBytes(iv); // Populate iv with random bytes.
        this.setInitializationVector(iv); // Set the initialization vector.
    } // public void generateInitializationVector()
} // public abstract class AES_Common implements AES_Implementable
