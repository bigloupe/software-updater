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
 * Utilities for XML manipulation.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class XMLUtil {

  private static final Logger LOG = Logger.getLogger(XMLUtil.class.getName());

  protected XMLUtil() {
  }

  /**
   * Get a node list with specified tag name from the element.
   * @param element the element to get the node list from
   * @param tagName the tag name of the node list
   * @param minSize the minimum number of item that must exist in the node list
   * @param maxSize the maximum number of item that can exist in the node list
   * @return the node list
   * @throws InvalidFormatException if the {@code minSize} or 
   * {@code maxSize} condition cannot be fulfilled
   */
  public static NodeList getNodeList(Element element, String tagName, int minSize, int maxSize) throws InvalidFormatException {
    if (element == null) {
      throw new NullPointerException("argument 'element' cannot be null");
    }
    if (tagName == null) {
      throw new NullPointerException("argument 'tagName' cannot be null");
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

    if ((minSize != -1 && nodeArrayList.size() < minSize) || (maxSize != -1 && nodeArrayList.size() > maxSize)) {
      throw new InvalidFormatException(String.format("The number of elements <%1$s> in <%2$s> not meet the size requirement. Size requirement: min: %3$d, max: %4$d, found: %5$d",
              tagName, element.getTagName(), minSize, maxSize, nodeArrayList.size()));
    }

    return new XMLElementNodeList(nodeArrayList);
  }

  /**
   * Get the element with the tag name from the element.
   * @param element the element to get the element from
   * @param tagName the tag name of the element
   * @param mustExist indicate the element must exist or not, if is true but 
   * element not found, an exception will be thrown
   * @return the element
   * @throws InvalidFormatException <code>mustExist</code> is true but 
   * element not found
   */
  public static Element getElement(Element element, String tagName, boolean mustExist) throws InvalidFormatException {
    if (element == null) {
      throw new NullPointerException("argument 'element' cannot be null");
    }
    if (tagName == null) {
      throw new NullPointerException("argument 'tagName' cannot be null");
    }

    NodeList nodeList = getNodeList(element, tagName, mustExist ? 1 : 0, 1);
    return (Element) nodeList.item(0);
  }

  /**
   * Get the text content with the tag name from the element.
   * @param element the element to get the text content from
   * @param tagName the tag name of the text content
   * @param mustExist indicate the text content must exist or not, if is true 
   * but text content not found, an exception will be thrown
   * @return the text content
   * @throws InvalidFormatException <code>mustExist</code> is true but 
   * text content not found/exist
   */
  public static String getTextContent(Element element, String tagName, boolean mustExist) throws InvalidFormatException {
    if (element == null) {
      throw new NullPointerException("argument 'element' cannot be null");
    }
    if (tagName == null) {
      throw new NullPointerException("argument 'tagName' cannot be null");
    }

    Element resultElement = getElement(element, tagName, mustExist);
    return resultElement == null ? null : resultElement.getTextContent(); // didn't check if the return value is null or empty
  }

  /**
   * Generate a storable output from the document.
   * @param doc the document to get the output
   * @return the output
   * @throws TransformerException error occurred when transferring from 
   * document to output
   */
  public static byte[] getOutput(Document doc) throws TransformerException {
    if (doc == null) {
      throw new NullPointerException("argument 'doc' cannot be null");
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

  /** 
   * Generate the document from the input content.
   * @param content the content
   * @return the document
   * @throws SAXException If any parse errors occur.
   * @throws IOException If any IO errors occur.
   */
  public static Document readDocument(byte[] content) throws SAXException, IOException {
    if (content == null) {
      throw new NullPointerException("argument 'content' cannot be null");
    }

    Document doc = null;
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
      doc = docBuilder.parse(new ByteArrayInputStream(content));
    } catch (ParserConfigurationException ex) {
      // should not get this exception
      LOG.log(Level.SEVERE, null, ex);
    }

    return doc;
  }

  /**
   * Create an empty document.
   * @return the empty document
   */
  public static Document createEmptyDocument() {
    Document doc = null;
    try {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      doc = docBuilder.newDocument();
    } catch (Exception ex) {
      // create empty document, should not get any exception
      LOG.log(Level.SEVERE, null, ex);
    }
    return doc;
  }

  /**
   * An implementation for {@link #getNodeList(org.w3c.dom.Element, java.lang.String, int, int)}.
   */
  protected static class XMLElementNodeList implements NodeList {

    /**
     * The node list.
     */
    protected Node[] nodeList;

    /**
     * Constructor.
     * @param nodeList the node list
     */
    protected XMLElementNodeList(List<Node> nodeList) {
      this.nodeList = nodeList == null ? new Node[0] : nodeList.toArray(new Node[nodeList.size()]);
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
