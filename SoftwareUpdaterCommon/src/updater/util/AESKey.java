package updater.util;

import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import updater.script.InvalidFormatException;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class AESKey {

    protected byte[] key;
    protected byte[] IV;

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
        this.key = new byte[key.length];
        System.arraycopy(key, 0, this.key, 0, key.length);
    }

    public byte[] getIV() {
        byte[] returnKey = new byte[IV.length];
        System.arraycopy(IV, 0, returnKey, 0, IV.length);
        return returnKey;
    }

    public void setIV(byte[] IV) {
        this.IV = new byte[IV.length];
        System.arraycopy(IV, 0, this.IV, 0, IV.length);
    }

    public static AESKey read(byte[] content) throws InvalidFormatException {
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

    public String output() throws TransformerException {
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
