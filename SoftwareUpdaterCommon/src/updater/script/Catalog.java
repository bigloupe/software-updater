package updater.script;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import updater.util.CommonUtil;
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
        Document doc = XMLUtil.readDocument(content);
        if (doc == null) {
            throw new InvalidFormatException("XML format incorrect.");
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

    public String output() {
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

    public static void main(String[] args) {
        try {
            Catalog catalog = Catalog.read(CommonUtil.readFile(new File("catalog.xml")));
            System.out.println(catalog.output());
        } catch (InvalidFormatException ex) {
            Logger.getLogger(Catalog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
