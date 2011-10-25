package updater.builder.util;

import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import updater.script.InvalidFormatException;
import updater.util.XMLUtil;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class RSAKey {

    protected byte[] modulus;
    protected byte[] publicExponent;
    protected byte[] privateExponent;

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

        return new RSAKey(Util.hexStringToByteArray(_modulus), Util.hexStringToByteArray(_publicExponent), Util.hexStringToByteArray(_privateExponent));
    }

    public String output() throws TransformerException {
        Document doc = XMLUtil.createEmptyDocument();
        if (doc == null) {
            return null;
        }

        Element rsaElement = doc.createElement("rsa");
        doc.appendChild(rsaElement);

        Element modulusElement = doc.createElement("modulus");
        modulusElement.setTextContent(Util.byteArrayToHexString(modulus));
        rsaElement.appendChild(modulusElement);

        Element exponentElement = doc.createElement("exponent");
        rsaElement.appendChild(exponentElement);

        Element publicExponentElement = doc.createElement("public");
        publicExponentElement.setTextContent(Util.byteArrayToHexString(publicExponent));
        exponentElement.appendChild(publicExponentElement);

        Element privateExponentElement = doc.createElement("private");
        privateExponentElement.setTextContent(Util.byteArrayToHexString(privateExponent));
        exponentElement.appendChild(privateExponentElement);

        return XMLUtil.getOutput(doc);
    }
}
