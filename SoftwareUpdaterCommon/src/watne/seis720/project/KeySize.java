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

/**
 * Key sizes for AES_Implementable objects. Also set the plaintext block size,
 * number of rounds, round key size, and expanded key size, since they vary
 * directly with the key size. Note that the plaintext block size and round key
 * size are both 128 bits for all key sizes.
 * @author John Watne
 * 
 */
public enum KeySize
{
    BITS128(128, 10, 176), BITS192(192, 12, 208), BITS256(256, 14, 240);

    private int keySizeBits;

    private int blockSizeBits;

    private int numRounds;

    private int roundKeyBits;

    private int expandedKeyBytes;

    KeySize(int keySizeBits, int numRounds, int expandedKeyBytes)
    {
        this.keySizeBits = keySizeBits;
        this.blockSizeBits = 128;
        this.numRounds = numRounds;
        this.roundKeyBits = 128;
        this.expandedKeyBytes = expandedKeyBytes;
    }

    /**
     * Get the key size to be used, in bits.
     * @return the block size to be used, in bits.
     */
    public int getKeySizeBits()
    {
        return this.keySizeBits;
    }

    /**
     * Get the block size to be used, in bits.
     * @return the block size to be used, in bits.
     */
    public int getBlockSizeBits()
    {
        return blockSizeBits;
    }

    /**
     * Get the expanded key size, in bytes.
     * @return the expanded key size, in bytes.
     */
    public int getExpandedKeyBytes()
    {
        return expandedKeyBytes;
    }

    /**
     * Get the number of rounds to be performed for encryption and decryption
     * operations.
     * @return the number of rounds to be performed for encryption and
     * decryption operations.
     */
    public int getNumRounds()
    {
        return numRounds;
    }

    /**
     * Get the round key size, in bits.
     * @return the round key size, in bits.
     */
    public int getRoundKeyBits()
    {
        return roundKeyBits;
    }
    
    /**
     * Get the key size, in bytes.
     * @return the key size, in bytes.
     */
    public int getKeySizeBytes()
    {
        return this.keySizeBits / 8;
    }
    
    /**
     * Get the key size, in words.
     * @return  the key size, in words.
     */
    public int getKeySizeWords()
    {
        return this.keySizeBits / 32;
    }
    
    /**
     * Get the block size, in bytes.
     * @return the block size, in bytes.
     */
    public int getBlockSizeBytes()
    {
        return this.blockSizeBits / 8;
    }
    
    /**
     * Get the block size, in words.
     * @return the block size, in words.
     */
    public int getBlockSizeWords()
    {
        return this.blockSizeBits / 32;
    }
    
    /**
     * Get the round key size, in bytes.
     * @return the round key size, in bytes.
     */
    public int getRoundKeyBytes()
    {
        return this.roundKeyBits / 8;
    }
    
    /**
     * Get the round key size, in words.
     * @return the round key size, in words.
     */
    public int getRoundKeyWords()
    {
        return this.roundKeyBits / 32;
    }
    
    /**
     * Get the expanded key size, in words.
     * @return the expanded key size, in words.
     */
    public int getExpandedKeyWords()
    {
        return this.expandedKeyBytes / 4;
    }
} // public enum KeySize
