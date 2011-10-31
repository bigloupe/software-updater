package updater.crypto;

import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import updater.script.InvalidFormatException;
import updater.util.CommonUtil;
import updater.util.XMLUtil;

/**
 * The RSA key.
 * <p>This read and write the modulus and exponents in XML format.<br />
 * Operations are not thread-safe.</p>
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class RSAKey {

    /**
     * The modulus.
     */
    protected byte[] modulus;
    /**
     * The public exponent.
     */
    protected byte[] publicExponent;
    /**
     * The private exponent.
     */
    protected byte[] privateExponent;

    /**
     * Constructor.
     * @param modulus see {@link #modulus}
     * @param publicExponent see {@link #publicExponent}
     * @param privateExponent  see {@link #privateExponent}
     */
    public RSAKey(byte[] modulus, byte[] publicExponent, byte[] privateExponent) {
        setModulus(modulus);
        setPublicExponent(publicExponent);
        setPrivateExponent(privateExponent);
    }

    public byte[] getModulus() {
        byte[] returnKey = new byte[modulus.length];
        System.arraycopy(modulus, 0, returnKey, 0, modulus.length);
        return returnKey;
    }

    public void setModulus(byte[] modulus) {
        this.modulus = new byte[modulus.length];
        System.arraycopy(modulus, 0, this.modulus, 0, modulus.length);
    }

    public byte[] getPublicExponent() {
        byte[] returnKey = new byte[publicExponent.length];
        System.arraycopy(publicExponent, 0, returnKey, 0, publicExponent.length);
        return returnKey;
    }

    public void setPublicExponent(byte[] publicExponent) {
        this.publicExponent = new byte[publicExponent.length];
        System.arraycopy(publicExponent, 0, this.publicExponent, 0, publicExponent.length);
    }

    public byte[] getPrivateExponent() {
        byte[] returnKey = new byte[privateExponent.length];
        System.arraycopy(privateExponent, 0, returnKey, 0, privateExponent.length);
        return returnKey;
    }

    public void setPrivateExponent(byte[] privateExponent) {
        this.privateExponent = new byte[privateExponent.length];
        System.arraycopy(privateExponent, 0, this.privateExponent, 0, privateExponent.length);
    }

    /**
     * Read the XML file.
     * @param content the content of the XML file
     * @return the {@link RSAKey} object with the information read
     * @throws InvalidFormatException the format of the XML file is invalid
     */
    public static RSAKey read(byte[] content) throws InvalidFormatException {
        Document doc;
        try {
            doc = XMLUtil.readDocument(content);
        } catch (Exception ex) {
            throw new InvalidFormatException("XML format incorrect. " + ex.getMessage());
        }

        Element _rsaNode = doc.getDocumentElement();

        String _modulus = XMLUtil.getTextContent(_rsaNode, "modulus", true);

        Element _exponentNode = XMLUtil.getElement(_rsaNode, "exponent", true);
        String _publicExponent = XMLUtil.getTextContent(_exponentNode, "public", true);
        String _privateExponent = XMLUtil.getTextContent(_exponentNode, "private", true);

        return new RSAKey(CommonUtil.hexStringToByteArray(_modulus), CommonUtil.hexStringToByteArray(_publicExponent), CommonUtil.hexStringToByteArray(_privateExponent));
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

        Element rsaElement = doc.createElement("rsa");
        doc.appendChild(rsaElement);

        Element modulusElement = doc.createElement("modulus");
        modulusElement.setTextContent(CommonUtil.byteArrayToHexString(modulus));
        rsaElement.appendChild(modulusElement);

        Element exponentElement = doc.createElement("exponent");
        rsaElement.appendChild(exponentElement);

        Element publicExponentElement = doc.createElement("public");
        publicExponentElement.setTextContent(CommonUtil.byteArrayToHexString(publicExponent));
        exponentElement.appendChild(publicExponentElement);

        Element privateExponentElement = doc.createElement("private");
        privateExponentElement.setTextContent(CommonUtil.byteArrayToHexString(privateExponent));
        exponentElement.appendChild(privateExponentElement);

        return XMLUtil.getOutput(doc);
    }
}
