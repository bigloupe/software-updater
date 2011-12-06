package updater.script;

import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import updater.util.XMLUtil;

/**
 * This script includes information for software to decide which patch(es) to 
 * download to update itself to latest available version.
 * It also contain the download URL, size and checksum of the patch.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Catalog {

  protected List<Patch> patches;

  public Catalog(List<Patch> patches) {
    this.patches = patches != null ? new ArrayList<Patch>(patches) : new ArrayList<Patch>();
  }

  public List<Patch> getPatchs() {
    return new ArrayList<Patch>(patches);
  }

  public void setPatchs(List<Patch> patches) {
    if (patches == null) {
      this.patches = new ArrayList<Patch>();
      return;
    }
    this.patches = new ArrayList<Patch>(patches);
  }

  public static Catalog read(byte[] content) throws InvalidFormatException {
    if (content == null) {
      throw new NullPointerException("argument 'content' cannot be null");
    }

    Document doc;
    try {
      doc = XMLUtil.readDocument(content);
    } catch (Exception ex) {
      throw new InvalidFormatException("XML format incorrect. " + ex.getMessage());
    }

    Element _patchesNode = doc.getDocumentElement();

    List<Patch> _patches = new ArrayList<Patch>();

    NodeList _patchNodeList = _patchesNode.getElementsByTagName("patch");
    for (int i = 0, iEnd = _patchNodeList.getLength(); i < iEnd; i++) {
      Element _patchNode = (Element) _patchNodeList.item(i);
      _patches.add(Patch.read(_patchNode));
    }

    return new Catalog(_patches);
  }

  public byte[] output() throws TransformerException {
    Document doc = XMLUtil.createEmptyDocument();
    if (doc == null) {
      return null;
    }

    Element rootElement = doc.createElement("patches");
    doc.appendChild(rootElement);

    for (Patch patch : patches) {
      rootElement.appendChild(patch.getElement(doc));
    }

    return XMLUtil.getOutput(doc);
  }
}
