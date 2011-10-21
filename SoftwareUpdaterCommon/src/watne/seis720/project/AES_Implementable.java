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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;

/**
 * Interface for implementations of AES encryption, to allow comparisons of the
 * various implementations.
 * @author John Watne
 * 
 */
public interface AES_Implementable
{
    /**
     * Constant used for describing the AES encryption algorithm used.
     */
    public static String AES = "AES";

    /**
     * Set the plain text to be encrypted.
     * @param text the plain text to be encrypted.
     * @throws UnsupportedEncodingException if attempting to translate the
     * String to bytes using an unsupported encoding method.
     */
    public void setPlainText(String text) throws UnsupportedEncodingException;

    /**
     * Set the plain text to be encrypted, using an array with the bytes of the
     * message to be encrypted.
     * @param text the plain text to be encrypted, using an array with the bytes
     * of the message to be encrypted.
     */
    public void setPlainText(byte[] text);

    /**
     * Set the key size, from which the block size, expanded key size, round key
     * size, and number of rounds can also be determined.
     * @param keySize the key size.
     */
    public void setKeySize(KeySize keySize);

    /**
     * Get the key size, from which the block size, expanded key size, round key
     * size, and number of rounds can also be determined.
     * @return the key size.
     */
    public KeySize getKeySize();

    /**
     * Generate the secret key to be used by the AES_Implemtable object for
     * encryption and decryption.
     * @throws Exception if an error occurs in the given implementation.
     * 
     */
    public void generateKey() throws Exception;

    /**
     * Set the secret key to be used by the AES_Implemtable object for
     * encryption and decryption.
     * @param key the secret key to be used by the AES_Implemtable object for
     * encryption and decryption, as an array of bytes.
     */
    public void setKey(byte[] key);

    /**
     * Get the secret key to be used by the AES_Implemtable object for
     * encryption and decryption.
     * @return the secret key to be used by the AES_Implemtable object for
     * encryption and decryption, as an array of bytes.
     */
    public byte[] getKey();

    /**
     * Decrypt the given ciphertext into plaintext, which may then be obtained
     * by a call to one of getPlainText() or getPlainTextBytes.
     * @throws Exception if an error occurs in the specific implementation.
     * 
     */
    public void decrypt() throws Exception;

    /**
     * Encrypt the given plaintext into ciphertext, which may then be obtained
     * by a call to one of getCipherBytes(), getCipherHexadecimal(), or
     * getCipherBase64().
     * @throws Exception if an error occurs in the given implementation.
     * 
     */
    public void encrypt() throws Exception;

    /**
     * Get the plaintext as a String.
     * @return the plaintext as a String.
     */
    public String getPlainText();

    /**
     * Get the plaintext as an array of bytes.
     * @return the plaintext as an array of bytes.
     */
    public byte[] getPlainTextBytes();

    /**
     * Set the encryption mode.
     * @param mode the encryption mode.
     */
    public void setMode(Mode mode);

    /**
     * Get the encryption mode.
     * @return the encryption mode.
     */
    public Mode getMode();

    /**
     * Set the padding method used.
     * @param pad the padding method used.
     */
    public void setPadding(Padding pad);

    /**
     * Set the padding method used.
     * @return the padding method used.
     */
    public Padding getPadding();

    /**
     * Get the ciphertext as an array of bytes.
     * @return the ciphertext as an array of bytes.
     */
    public byte[] getCipherBytes();

    /**
     * Set the ciphertext as an array of bytes.
     * @param cipherText the ciphertext as an array of bytes.
     */
    public void setCipherBytes(byte[] cipherText);

    /**
     * Get the ciphertext as a String consisting of the 2-digit hexadecimal
     * values for each word.
     * @return the ciphertext as a String consisting of the 2-digit hexadecimal
     * values for each word.
     */
    public String getCipherHexadecimal();

    /**
     * Set the ciphertext as a String consisting of the 2-digit hexadecimal
     * values for each word.
     * @param hexCipherText the ciphertext as a String consisting of the 2-digit
     * hexadecimal values for each word.
     */
    public void setCipherHexadecimal(String hexCipherText);

    /**
     * Get the cipher text, represented by a Base64 String.
     * @return the cipher text, represented by a Base64 String.
     */
    public String getCipherBase64();

    /**
     * Set the cipher text, represented by a Base64 String.
     * @param base64CipherText the cipher text, represented by a Base64 String.
     * @throws IOException if an error occurs reading the bytes from the text.
     */
    public void setCipherBase64(String base64CipherText) throws IOException;

    /**
     * Get a description of the specific implementation of the AES algorithm.
     * @return a description of the specific implementation of the AES
     * algorithm.
     */
    public String getImplementationType();

    /**
     * Get the ciphertext as a String. Note that it is not recommended to use
     * this method to get the ciphertext, since the text may contain control
     * characters that could cause erratic behavior if printed to standard
     * output.
     * @return the ciphertext as a String.
     */
    public String getCipherTextString();

    /**
     * Set the ciphertext to the specified text value.
     * @param text the ciphertext as text.
     */
    public void setCipherTextString(String text);

    /**
     * Encrypt the given file.
     * @param inputFile the file to be encrypted.
     * @param outputFile the encrypted file.
     * @throws IOException if an error occurs reading from the input file or
     * writing to the output file.
     * @throws FileNotFoundException if the specified input file does not exist.
     * @throws Exception for Exceptions encountered during encryption.
     */
    public void encryptFile(File inputFile, File outputFile)
                                                            throws FileNotFoundException,
                                                            IOException,
                                                            Exception;

    /**
     * Decrypt the given file.
     * @param inputFile the file to be decrypted.
     * @param outputFile the decrypted file.
     * @throws FileNotFoundException if the specified input file does not exist.
     * @throws HeadlessException if run in an environment that does not support
     * Swing dialogs.
     * @throws IOException if an error occurs reading from the input file or
     * writing to the output file.
     * @throws Exception for Exceptions encountered during decryption.
     */
    public void decryptFile(File inputFile, File outputFile)
                                                            throws FileNotFoundException,
                                                            HeadlessException,
                                                            IOException,
                                                            Exception;

    /**
     * Get the initialization vector used by the AES_Implementable object for
     * those Modes requiring one.
     * @return the initialization vector used by the AES_Implementable object
     * for those Modes requiring one.
     */
    public byte[] getInitializationVector();

    /**
     * Set the initialization vector used by the AES_Implementable object for
     * those Modes requiring one.
     * @param iv the initialization vector used by the AES_Implementable object
     * for those Modes requiring one.
     * @throws InvalidAlgorithmParameterException if one of the parameters
     * required for the cipher is not correct.
     */
    public void setInitializationVector(byte[] iv)
                                                  throws InvalidAlgorithmParameterException;

    /**
     * Get the Base64 text representation of the initialization vector.
     * @return the Base64 text representation of the initialization vector.
     */
    public String getInitializationVectorBase64();

    /**
     * Set the Base64 text representation of the initialization vector.
     * @param ivBase64 the Base64 text representation of the initialization
     * vector.
     * @throws IOException if the text is not in correct Base64 format.
     * @throws InvalidAlgorithmParameterException if any of the parameters
     * required for the cipher are incorrect.
     */
    public void setInitializationVectorBase64(String ivBase64)
                                                              throws IOException,
                                                              InvalidAlgorithmParameterException;

    /**
     * Generate an initialization vector for those modes requiring one.
     * @throws InvalidAlgorithmParameterException if one of the required
     * parameters for the cipher is incorrect.
     */
    public void generateInitializationVector()
                                              throws InvalidAlgorithmParameterException;
} // public interface AES_Implementable
