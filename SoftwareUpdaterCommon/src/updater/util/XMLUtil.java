package updater.util;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
import updater.script.InvalidFormatException;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class XMLUtil {

    protected XMLUtil() {
    }

    public static NodeList getNodeList(Element element, String tagName, int minSize, int maxSize) throws InvalidFormatException {
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
        NodeList nodeList = getNodeList(element, tagName, mustExist ? 1 : 0, 1);
        return (Element) nodeList.item(0);
    }

    public static String getTextContent(Element element, String tagName, boolean mustExist) throws InvalidFormatException {
        Element resultElement = getElement(element, tagName, mustExist);
        return resultElement == null ? null : resultElement.getTextContent();
    }

    public static String getOutput(Document doc) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);

            return writer.toString();
        } catch (TransformerException ex) {
            Logger.getLogger(XMLUtil.class.getName()).log(Level.WARNING, null, ex);
        }
        return null;
    }

    public static Document readDocument(byte[] content) {
        Document doc = null;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
            doc = docBuilder.parse(new ByteArrayInputStream(content));
        } catch (Exception ex) {
            Logger.getLogger(XMLUtil.class.getName()).log(Level.INFO, null, ex);
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
            Logger.getLogger(XMLUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return doc;
    }

    public static class XMLElementNodeList implements NodeList {

        protected Node[] nodeList;

        protected XMLElementNodeList(List<Node> nodeList) {
            this.nodeList = nodeList.toArray(new Node[nodeList.size()]);
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
