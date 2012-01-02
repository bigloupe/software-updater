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
package updater.crypto;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import updater.script.InvalidFormatException;
import updater.util.CommonUtil;

/**
 * The cipher key generator.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class KeyGenerator {

  private static final Logger LOG = Logger.getLogger(KeyGenerator.class.getName());

  protected KeyGenerator() {
  }

  /**
   * Generate a RSA key and save to file.
   * 
   * @param keySize the key size in bits, must >= 512 (required by Java).
   * @param saveTo the place to save the generated file
   * 
   * @throws IOException the keySize is invalid or error occurred when writing 
   * to file
   * @throws InvalidParameterException {@code keySize} is invalid
   */
  public static void generateRSA(int keySize, File saveTo) throws IOException, InvalidParameterException {
    if (saveTo == null) {
      return;
    }
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(keySize);
      KeyPair keyPair = keyPairGenerator.genKeyPair();

      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      RSAPublicKeySpec publicKeySpec = keyFactory.getKeySpec(keyPair.getPublic(), RSAPublicKeySpec.class);
      RSAPrivateKeySpec privateKeySpec = keyFactory.getKeySpec(keyPair.getPrivate(), RSAPrivateKeySpec.class);

      RSAKey rsaKey = new RSAKey(privateKeySpec.getModulus().toByteArray(), publicKeySpec.getPublicExponent().toByteArray(), privateKeySpec.getPrivateExponent().toByteArray());

      CommonUtil.writeFile(saveTo, rsaKey.output());
    } catch (InvalidKeySpecException ex) {
      LOG.log(Level.SEVERE, null, ex);
    } catch (NoSuchAlgorithmException ex) {
      LOG.log(Level.SEVERE, null, ex);
    } catch (UnsupportedEncodingException ex) {
      LOG.log(Level.SEVERE, null, ex);
    } catch (TransformerException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Generate a AES key and save to file.
   * 
   * @param keySize the key size in bits
   * @param saveTo the place to save the generated file
   * 
   * @throws IllegalArgumentException {@code keySize} < 8
   * @throws IOException error occurred when writing to file
   */
  public static void generateAES(int keySize, File saveTo) throws IOException {
    if (keySize < 1) {
      throw new IllegalArgumentException("argument 'keySize' must >= 8");
    }
    if (saveTo == null) {
      return;
    }
    byte[] key = generateRandom(keySize / 8);
    byte[] IV = generateRandom(16);

    AESKey aesKey = new AESKey(key, IV);
    try {
      CommonUtil.writeFile(saveTo, aesKey.output());
    } catch (TransformerException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Renew the 128-bit initial vector in the AES key file.
   * 
   * @param file the AES key file
   * 
   * @throws IOException error occurred when reading/writing to the key file
   * @throws InvalidFormatException the format of the content in the key file 
   * is invalid
   */
  public static void renewAESIV(File file) throws IOException, InvalidFormatException {
    if (file == null) {
      return;
    }
    byte[] IV = generateRandom(16);

    AESKey aesKey = AESKey.read(CommonUtil.readFile(file));
    aesKey.setIV(IV);

    try {
      CommonUtil.writeFile(file, aesKey.output());
    } catch (TransformerException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Generate a random byte array with specified length in byte. (Low 
   * efficiency)
   * 
   * @param length the length in byte, not allow <= 0
   * 
   * @return the random generated byte array
   */
  public static byte[] generateRandom(int length) {
    byte[] b = new byte[length];
    (new Random()).nextBytes(b);
    return b;
  }
}
