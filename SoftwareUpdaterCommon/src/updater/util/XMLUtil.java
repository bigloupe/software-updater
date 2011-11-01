package updater.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import updater.script.InvalidFormatException;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class XMLUtil {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }

    protected XMLUtil() {
    }

    public static NodeList getNodeList(Element element, String tagName, int minSize, int maxSize) throws InvalidFormatException {
        if (element == null || tagName == null) {
            return null;
        }
        List<Node> nodeArrayList = new ArrayList<Node>();
        NodeList _nodeList = element.getChildNodes();
        for (int i = 0, iEnd = _nodeList.getLength(); i < iEnd; i++) {
            Node node = _nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element _element = (Element) node;
                if (_element.getTagName().equals(tagName)) {
                    nodeArrayList.add(node);
                }
            }
        }

        NodeList nodeList = new XMLElementNodeList(nodeArrayList);
        if ((minSize != -1 && nodeList.getLength() < minSize) || (maxSize != -1 && nodeList.getLength() > maxSize)) {
            throw new InvalidFormatException("The number of elements <" + tagName + "> in <" + element.getTagName() + "> not meet the size requirement. Size requirement: min: " + minSize + ", max: " + maxSize + ", found: " + nodeList.getLength());
        }
        return nodeList;
    }

    public static Element getElement(Element element, String tagName, boolean mustExist) throws InvalidFormatException {
        if (element == null || tagName == null) {
            return null;
        }
        NodeList nodeList = getNodeList(element, tagName, mustExist ? 1 : 0, 1);
        return (Element) nodeList.item(0);
    }

    public static String getTextContent(Element element, String tagName, boolean mustExist) throws InvalidFormatException {
        if (element == null || tagName == null) {
            return null;
        }
        Element resultElement = getElement(element, tagName, mustExist);
        return resultElement == null ? null : resultElement.getTextContent();
    }

    public static byte[] getOutput(Document doc) throws TransformerException {
        if (doc == null) {
            return null;
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();

        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(bout));

        return bout.toByteArray();
    }

    public static Document readDocument(byte[] content) throws SAXException, IOException {
        if (content == null) {
            return null;
        }
        Document doc = null;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
            doc = docBuilder.parse(new ByteArrayInputStream(content));
        } catch (ParserConfigurationException ex) {
            // should not get this exception
            if (debug) {
                Logger.getLogger(XMLUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return doc;
    }

    public static Document createEmptyDocument() {
        Document doc = null;
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (Exception ex) {
            // create empty document, should not get any exception
            if (debug) {
                Logger.getLogger(XMLUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return doc;
    }

    public static class XMLElementNodeList implements NodeList {

        protected Node[] nodeList;

        protected XMLElementNodeList(List<Node> nodeList) {
            if (nodeList == null) {
                this.nodeList = new Node[0];
            } else {
                this.nodeList = nodeList.toArray(new Node[nodeList.size()]);
            }
        }

        @Override
        public Node item(int index) {
            return index >= nodeList.length ? null : nodeList[index];
        }

        @Override
        public int getLength() {
            return nodeList.length;
        }
    }
}
