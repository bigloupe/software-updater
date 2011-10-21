package updater.script;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import updater.util.XMLUtil;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Catalog {

    protected List<Release> releases;
    protected List<Update> updates;

    public Catalog(List<Release> releases, List<Update> updates) {
        this.releases = new ArrayList<Release>(releases);
        this.updates = new ArrayList<Update>(updates);
    }

    public List<Update> getUpdates() {
        return new ArrayList<Update>(updates);
    }

    public void setUpdates(List<Update> updates) {
        this.updates = new ArrayList<Update>(updates);
    }

    public List<Release> getReleases() {
        return new ArrayList<Release>(releases);
    }

    public void setReleases(List<Release> releases) {
        this.releases = new ArrayList<Release>(releases);
    }

    public static Catalog read(byte[] content) throws InvalidFormatException {
        Document doc = XMLUtil.readDocument(content);
        if (doc == null) {
            throw new InvalidFormatException("XML format incorrect.");
        }

        Element _rootNode = doc.getDocumentElement();


        Element _releasesNode = XMLUtil.getElement(_rootNode, "releases", true);

        List<Release> _releases = new ArrayList<Release>();

        NodeList _releaseNodeList = _releasesNode.getElementsByTagName("release");
        for (int i = 0, iEnd = _releaseNodeList.getLength(); i < iEnd; i++) {
            Element _releaseNode = (Element) _releaseNodeList.item(i);
            _releases.add(Release.read(_releaseNode));
        }


        Element _updatesNode = XMLUtil.getElement(_rootNode, "updates", true);

        List<Update> _updates = new ArrayList<Update>();

        NodeList _updateNodeList = _updatesNode.getElementsByTagName("update");
        for (int i = 0, iEnd = _updateNodeList.getLength(); i < iEnd; i++) {
            Element _updateNode = (Element) _updateNodeList.item(i);
            _updates.add(Update.read(_updateNode));
        }

        return new Catalog(_releases, _updates);
    }

    public String output() {
        Document doc = XMLUtil.createEmptyDocument();
        if (doc == null) {
            return null;
        }

        Element rootElement = doc.createElement("root");
        doc.appendChild(rootElement);


        Element releasesElement = doc.createElement("releases");
        rootElement.appendChild(releasesElement);

        for (Release release : releases) {
            releasesElement.appendChild(release.getElement(doc));
        }


        Element updatesElement = doc.createElement("updates");
        rootElement.appendChild(updatesElement);

        for (Update catalogUpdate : updates) {
            updatesElement.appendChild(catalogUpdate.getElement(doc));
        }

        return XMLUtil.getOutput(doc);
    }

    public static class Release {

        protected int id;
        protected String version;
        protected String packUrl;
        protected String packChecksum;
        protected int packLength;
        protected String packEncryptionType;
        protected String packEncryptionKey;
        protected String packEncryptionIV;

        public Release(int id, String version, String packUrl, String packChecksum, int packLength, String packEncryptionType, String packEncryptionKey, String packEncryptionIV) {
            this.id = id;
            this.version = version;
            this.packUrl = packUrl;
            this.packChecksum = packChecksum;
            this.packLength = packLength;
            this.packEncryptionType = packEncryptionType;
            this.packEncryptionKey = packEncryptionKey;
            this.packEncryptionIV = packEncryptionIV;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getPackChecksum() {
            return packChecksum;
        }

        public void setPackChecksum(String packChecksum) {
            this.packChecksum = packChecksum;
        }

        public String getPackUrl() {
            return packUrl;
        }

        public void setPackUrl(String packUrl) {
            this.packUrl = packUrl;
        }

        public int getPackLength() {
            return packLength;
        }

        public void setPackLength(int packLength) {
            this.packLength = packLength;
        }

        public String getPackEncryptionType() {
            return packEncryptionType;
        }

        public void setPackEncryptionType(String packEncryptionType) {
            this.packEncryptionType = packEncryptionType;
        }

        public String getPackEncryptionKey() {
            return packEncryptionKey;
        }

        public void setPackEncryptionKey(String packEncryptionKey) {
            this.packEncryptionKey = packEncryptionKey;
        }

        public String getPackEncryptionIV() {
            return packEncryptionIV;
        }

        public void setPackEncryptionIV(String packEncryptionIV) {
            this.packEncryptionIV = packEncryptionIV;
        }

        protected static Release read(Element releaseElement) throws InvalidFormatException {
            if (releaseElement == null) {
                throw new NullPointerException();
            }

            int _id = 0;
            try {
                _id = Integer.parseInt(releaseElement.getAttribute("id"));
            } catch (Exception ex) {
                throw new InvalidFormatException("attribute 'id' for 'release' element not exist");
            }

            String _version = XMLUtil.getTextContent(releaseElement, "version", true);

            Element _packElement = XMLUtil.getElement(releaseElement, "pack", true);
            String _packUrl = XMLUtil.getTextContent(_packElement, "url", true);
            String _packChecksum = XMLUtil.getTextContent(_packElement, "checksum", true);
            int _packLength = Integer.parseInt(XMLUtil.getTextContent(_packElement, "length", true));

            String _encryptionType = null;
            String _encryptionKey = null;
            String _encryptionIV = null;
            Element _encryptionElement = XMLUtil.getElement(_packElement, "encryption", false);
            if (_encryptionElement != null) {
                _encryptionType = XMLUtil.getTextContent(_encryptionElement, "type", true);
                _encryptionKey = XMLUtil.getTextContent(_encryptionElement, "key", true);
                _encryptionIV = XMLUtil.getTextContent(_encryptionElement, "IV", true);
            }

            return new Release(_id, _version, _packUrl, _packChecksum, _packLength, _encryptionType, _encryptionKey, _encryptionIV);
        }

        protected Element getElement(Document doc) {
            Element _release = doc.createElement("release");
            _release.setAttribute("id", Integer.toString(id));

            Element _version = doc.createElement("version");
            _version.appendChild(doc.createTextNode(version));
            _release.appendChild(_version);

            //<editor-fold defaultstate="collapsed" desc="pack">
            Element _pack = doc.createElement("pack");
            _release.appendChild(_pack);

            Element _packUrl = doc.createElement("url");
            _packUrl.appendChild(doc.createTextNode(packUrl));
            _pack.appendChild(_packUrl);

            Element _packChecksum = doc.createElement("checksum");
            _packChecksum.appendChild(doc.createTextNode(packChecksum));
            _pack.appendChild(_packChecksum);

            Element _packLength = doc.createElement("length");
            _packLength.appendChild(doc.createTextNode(Integer.toString(packLength)));
            _pack.appendChild(_packLength);

            if (packEncryptionType != null) {
                Element _encryption = doc.createElement("encryption");
                _pack.appendChild(_encryption);

                Element _encryptionType = doc.createElement("type");
                _encryptionType.appendChild(doc.createTextNode(packEncryptionType));
                _encryption.appendChild(_encryptionType);

                Element _encryptionKey = doc.createElement("key");
                _encryptionKey.appendChild(doc.createTextNode(packEncryptionKey));
                _encryption.appendChild(_encryptionKey);

                Element _encryptionIV = doc.createElement("IV");
                _encryptionIV.appendChild(doc.createTextNode(packEncryptionIV));
                _encryption.appendChild(_encryptionIV);
            }
            //</editor-fold>

            return _release;
        }
    }

    public static class Update {

        protected int id;
        protected String versionFrom;
        protected String versionTo;
        protected String patchUrl;
        protected String patchChecksum;
        protected int patchLength;
        protected String patchEncryptionType;
        protected String patchEncryptionKey;
        protected String patchEncryptionIV;

        public Update(int id, String versionFrom, String versionTo, String patchUrl, String patchChecksum, int patchLength, String patchEncryptionType, String patchEncryptionKey, String patchEncryptionIV) {
            this.id = id;
            this.versionFrom = versionFrom;
            this.versionTo = versionTo;
            this.patchUrl = patchUrl;
            this.patchChecksum = patchChecksum;
            this.patchLength = patchLength;
            this.patchEncryptionType = patchEncryptionType;
            this.patchEncryptionKey = patchEncryptionKey;
            this.patchEncryptionIV = patchEncryptionIV;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getVersionFrom() {
            return versionFrom;
        }

        public void setVersionFrom(String versionFrom) {
            this.versionFrom = versionFrom;
        }

        public String getPatchChecksum() {
            return patchChecksum;
        }

        public void setPatchChecksum(String patchChecksum) {
            this.patchChecksum = patchChecksum;
        }

        public String getVersionTo() {
            return versionTo;
        }

        public void setVersionTo(String versionTo) {
            this.versionTo = versionTo;
        }

        public String getPatchUrl() {
            return patchUrl;
        }

        public void setPatchUrl(String patchUrl) {
            this.patchUrl = patchUrl;
        }

        public int getPatchLength() {
            return patchLength;
        }

        public void setPatchLength(int patchLength) {
            this.patchLength = patchLength;
        }

        public String getPatchEncryptionType() {
            return patchEncryptionType;
        }

        public void setPatchEncryptionType(String patchEncryptionType) {
            this.patchEncryptionType = patchEncryptionType;
        }

        public String getPatchEncryptionKey() {
            return patchEncryptionKey;
        }

        public void setPatchEncryptionKey(String patchEncryptionKey) {
            this.patchEncryptionKey = patchEncryptionKey;
        }

        public String getPatchEncryptionIV() {
            return patchEncryptionIV;
        }

        public void setPatchEncryptionIV(String patchEncryptionIV) {
            this.patchEncryptionIV = patchEncryptionIV;
        }

        protected static Update read(Element updateElement) throws InvalidFormatException {
            if (updateElement == null) {
                throw new NullPointerException();
            }

            int _id = 0;
            try {
                _id = Integer.parseInt(updateElement.getAttribute("id"));
            } catch (Exception ex) {
                throw new InvalidFormatException("attribute 'id' for 'update' element not exist");
            }

            Element _versionElement = XMLUtil.getElement(updateElement, "version", true);
            String _versionFrom = XMLUtil.getTextContent(_versionElement, "from", true);
            String _versionTo = XMLUtil.getTextContent(_versionElement, "to", true);

            Element _patchElement = XMLUtil.getElement(updateElement, "patch", true);
            String _patchUrl = XMLUtil.getTextContent(_patchElement, "url", true);
            String _patchChecksum = XMLUtil.getTextContent(_patchElement, "checksum", true);
            int _patchLength = Integer.parseInt(XMLUtil.getTextContent(_patchElement, "length", true));

            String _encryptionType = null;
            String _encryptionKey = null;
            String _encryptionIV = null;
            Element _encryptionElement = XMLUtil.getElement(_patchElement, "encryption", false);
            if (_encryptionElement != null) {
                _encryptionType = XMLUtil.getTextContent(_encryptionElement, "type", true);
                _encryptionKey = XMLUtil.getTextContent(_encryptionElement, "key", true);
                _encryptionIV = XMLUtil.getTextContent(_encryptionElement, "IV", true);
            }

            return new Update(_id, _versionFrom, _versionTo, _patchUrl, _patchChecksum, _patchLength, _encryptionType, _encryptionKey, _encryptionIV);
        }

        protected Element getElement(Document doc) {
            Element _update = doc.createElement("update");
            _update.setAttribute("id", Integer.toString(id));

            //<editor-fold defaultstate="collapsed" desc="version">
            Element _version = doc.createElement("version");
            _update.appendChild(_version);

            Element _versionFrom = doc.createElement("from");
            _versionFrom.appendChild(doc.createTextNode(versionFrom));
            _version.appendChild(_versionFrom);

            Element _versionTo = doc.createElement("to");
            _versionTo.appendChild(doc.createTextNode(versionTo));
            _version.appendChild(_versionTo);
            //</editor-fold>

            //<editor-fold defaultstate="collapsed" desc="patch">
            Element _patch = doc.createElement("patch");
            _update.appendChild(_patch);

            Element _patchUrl = doc.createElement("url");
            _patchUrl.appendChild(doc.createTextNode(patchUrl));
            _patch.appendChild(_patchUrl);

            Element _patchChecksum = doc.createElement("checksum");
            _patchChecksum.appendChild(doc.createTextNode(patchChecksum));
            _patch.appendChild(_patchChecksum);

            Element _patchLength = doc.createElement("length");
            _patchLength.appendChild(doc.createTextNode(Integer.toString(patchLength)));
            _patch.appendChild(_patchLength);

            if (patchEncryptionType != null) {
                Element _encryption = doc.createElement("encryption");
                _patch.appendChild(_encryption);

                Element _encryptionType = doc.createElement("type");
                _encryptionType.appendChild(doc.createTextNode(patchEncryptionType));
                _encryption.appendChild(_encryptionType);

                Element _encryptionKey = doc.createElement("key");
                _encryptionKey.appendChild(doc.createTextNode(patchEncryptionKey));
                _encryption.appendChild(_encryptionKey);

                Element _encryptionIV = doc.createElement("IV");
                _encryptionIV.appendChild(doc.createTextNode(patchEncryptionIV));
                _encryption.appendChild(_encryptionIV);
            }
            //</editor-fold>

            return _update;
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        File file = new File("catalog.xml");
        byte[] content = new byte[(int) file.length()];

        FileInputStream fin = new FileInputStream(file);
        fin.read(content);
        fin.close();

        try {
            Catalog catalog = Catalog.read(content);
            System.out.println(catalog.output());
        } catch (InvalidFormatException ex) {
            Logger.getLogger(Catalog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
