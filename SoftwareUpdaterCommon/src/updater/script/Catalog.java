package updater.script;

import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import updater.util.XMLUtil;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Catalog {

    protected List<Patch> patches;

    public Catalog(List<Patch> patches) {
        this.patches = new ArrayList<Patch>(patches);
    }

    public List<Patch> getPatchs() {
        return new ArrayList<Patch>(patches);
    }

    public void setPatchs(List<Patch> patches) {
        this.patches = new ArrayList<Patch>(patches);
    }

    public static Catalog read(byte[] content) throws InvalidFormatException {
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
