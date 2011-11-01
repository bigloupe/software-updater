package updater.crypto;

import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import updater.script.InvalidFormatException;
import updater.util.CommonUtil;
import updater.util.XMLUtil;

/**
 * The AES key.
 * <p>This read and write the key and IV in XML format.<br />
 * Operations are not thread-safe.</p>
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class AESKey {

    /**
     * The cipher key.
     */
    protected byte[] key;
    /**
     * The initial vector, should be exactly 16 bytes (128 bits).
     */
    protected byte[] IV;

    /**
     * Constructor.
     * @param key see {@link #key}
     * @param IV see {@link #IV}
     */
    public AESKey(byte[] key, byte[] IV) {
        setKey(key);
        setIV(IV);
    }

    public byte[] getKey() {
        byte[] returnKey = new byte[key.length];
        System.arraycopy(key, 0, returnKey, 0, key.length);
        return returnKey;
    }

    public void setKey(byte[] key) {
        if (key == null) {
            throw new NullPointerException("argument 'key' cannot be null");
        }
        this.key = new byte[key.length];
        System.arraycopy(key, 0, this.key, 0, key.length);
    }

    public byte[] getIV() {
        byte[] returnKey = new byte[IV.length];
        System.arraycopy(IV, 0, returnKey, 0, IV.length);
        return returnKey;
    }

    /**
     * Set the initial vector. The length of the IV should be 128 bits (16 bytes).
     * @param IV see {@link #IV}
     */
    public void setIV(byte[] IV) {
        if (IV == null) {
            throw new NullPointerException("argument 'IV' cannot be null");
        }
        if (IV.length != 16) {
            throw new IllegalArgumentException("length of IV should be 128 bits (16 bytes)");
        }
        this.IV = new byte[IV.length];
        System.arraycopy(IV, 0, this.IV, 0, IV.length);
    }

    /**
     * Read the XML file.
     * @param content the content of the XML file
     * @return the {@link AESKey} object with the information read
     * @throws InvalidFormatException the format of the XML file is invalid
     */
    public static AESKey read(byte[] content) throws InvalidFormatException {
        if (content == null) {
            return null;
        }
        Document doc;
        try {
            doc = XMLUtil.readDocument(content);
        } catch (Exception ex) {
            throw new InvalidFormatException("XML format incorrect. " + ex.getMessage());
        }

        Element _aesNode = doc.getDocumentElement();

        String _key = XMLUtil.getTextContent(_aesNode, "key", true);
        String _IV = XMLUtil.getTextContent(_aesNode, "IV", true);

        return new AESKey(CommonUtil.hexStringToByteArray(_key), CommonUtil.hexStringToByteArray(_IV));
    }

    /**
     * Output the object in UTF-8 XML format.
     * @return the content in byte array
     * @throws TransformerException some information is missing
     */
    public byte[] output() throws TransformerException {
        Document doc = XMLUtil.createEmptyDocument();
        if (doc == null) {
            return null;
        }

        Element aesElement = doc.createElement("aes");
        doc.appendChild(aesElement);

        Element keyElement = doc.createElement("key");
        keyElement.setTextContent(CommonUtil.byteArrayToHexString(key));
        aesElement.appendChild(keyElement);

        Element IVElement = doc.createElement("IV");
        IVElement.setTextContent(CommonUtil.byteArrayToHexString(IV));
        aesElement.appendChild(IVElement);

        return XMLUtil.getOutput(doc);
    }
}
