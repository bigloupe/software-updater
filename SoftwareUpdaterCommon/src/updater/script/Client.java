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
public class Client {

    protected String version;
    protected String catalogUrl;
    protected String jarPath;
    protected String mainClass;
    protected String storagePath;
    protected Information information;
    protected long lastUpdated;
    protected String publicKey;
    protected List<Update> updates;

    public Client(String version, String catalogUrl, String jarPath, String mainClass, String storagePath, Information information, long lastUpdated, String publicKey, List<Update> updates) {
        this.version = version;
        this.catalogUrl = catalogUrl;
        this.jarPath = jarPath;
        this.mainClass = mainClass;
        this.storagePath = storagePath;
        this.information = information;
        this.lastUpdated = lastUpdated;
        this.publicKey = publicKey;
        this.updates = new ArrayList<Update>(updates);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCatalogUrl() {
        return catalogUrl;
    }

    public void setCatalogUrl(String catalogUrl) {
        this.catalogUrl = catalogUrl;
    }

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Information getInformation() {
        return information;
    }

    public void setInformation(Information information) {
        this.information = information;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public List<Update> getUpdates() {
        return new ArrayList<Update>(updates);
    }

    public void setUpdates(List<Update> updates) {
        this.updates = new ArrayList<Update>(updates);
    }

    public static Client read(byte[] content) throws InvalidFormatException {
        Document doc = XMLUtil.readDocument(content);
        if (doc == null) {
            throw new InvalidFormatException("XML format incorrect.");
        }

        Element _rootNode = doc.getDocumentElement();

        String _version = XMLUtil.getTextContent(_rootNode, "version", true);

        String _catalogUrl = XMLUtil.getTextContent(_rootNode, "catalog-url", true);
        String _jarPath = XMLUtil.getTextContent(_rootNode, "jar-path", true);
        String _mainClass = XMLUtil.getTextContent(_rootNode, "main-class", true);
        String _storagePath = XMLUtil.getTextContent(_rootNode, "storage-path", true);

        Information _information = Information.read(XMLUtil.getElement(_rootNode, "information", true));

        String _lastUpdatedString = XMLUtil.getTextContent(_rootNode, "last-updated", false);
        Long _lastUpdated = _lastUpdatedString != null ? Long.parseLong(_lastUpdatedString) : -1;

        String _publicKey = XMLUtil.getTextContent(_rootNode, "public-key", false);

        List<Update> _updates = new ArrayList<Update>();
        Element _updatesElement = XMLUtil.getElement(_rootNode, "updates", false);
        if (_updatesElement != null) {
            NodeList _updateNodeList = _updatesElement.getElementsByTagName("update");
            for (int i = 0, iEnd = _updateNodeList.getLength(); i < iEnd; i++) {
                Element _updateNode = (Element) _updateNodeList.item(i);
                _updates.add(Update.read(_updateNode));
            }
        }

        return new Client(_version, _catalogUrl, _jarPath, _mainClass, _storagePath, _information, _lastUpdated, _publicKey, _updates);
    }

    public String output() {
        Document doc = XMLUtil.createEmptyDocument();
        if (doc == null) {
            return null;
        }

        Element rootElement = doc.createElement("root");
        doc.appendChild(rootElement);

        Element versionElement = doc.createElement("version");
        versionElement.setTextContent(version);
        rootElement.appendChild(versionElement);

        Element catalogUrlElement = doc.createElement("catalog-url");
        catalogUrlElement.setTextContent(catalogUrl);
        rootElement.appendChild(catalogUrlElement);

        Element jarPathElement = doc.createElement("jar-path");
        jarPathElement.setTextContent(jarPath);
        rootElement.appendChild(jarPathElement);

        Element mainClassElement = doc.createElement("main-class");
        mainClassElement.setTextContent(mainClass);
        rootElement.appendChild(mainClassElement);

        Element storagePathElement = doc.createElement("storage-path");
        storagePathElement.setTextContent(storagePath);
        rootElement.appendChild(storagePathElement);

        Element publicKeyElement = doc.createElement("public-key");
        publicKeyElement.setTextContent(publicKey);
        rootElement.appendChild(publicKeyElement);

        Element informationElement = information.getElement(doc);
        rootElement.appendChild(informationElement);

        Element updatesElement = doc.createElement("updates");
        rootElement.appendChild(updatesElement);
        for (Update update : updates) {
            Element _updateElement = update.getElement(doc);
            if (_updateElement != null) {
                updatesElement.appendChild(_updateElement);
            }
        }

        return XMLUtil.getOutput(doc);
    }

    public static class Update {

        protected int id;
        protected String versionFrom;
        protected String versionTo;
        protected String path;
        protected String encryptionType;
        protected String encryptionKey;
        protected String encryptionIV;

        public Update(int id, String versionFrom, String versionTo, String path, String encryptionType, String encryptionKey, String encryptionIV) {
            this.id = id;
            this.versionFrom = versionFrom;
            this.versionTo = versionTo;
            this.path = path;
            this.encryptionType = encryptionType;
            this.encryptionKey = encryptionKey;
            this.encryptionIV = encryptionIV;
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

        public String getVersionTo() {
            return versionTo;
        }

        public void setVersionTo(String versionTo) {
            this.versionTo = versionTo;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getEncryptionType() {
            return encryptionType;
        }

        public void setEncryptionType(String encryptionType) {
            this.encryptionType = encryptionType;
        }

        public String getEncryptionKey() {
            return encryptionKey;
        }

        public void setEncryptionKey(String encryptionKey) {
            this.encryptionKey = encryptionKey;
        }

        public String getEncryptionIV() {
            return encryptionIV;
        }

        public void setEncryptionIV(String encryptionIV) {
            this.encryptionIV = encryptionIV;
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

            String _path = XMLUtil.getTextContent(_patchElement, "path", true);

            String _encryptionType = null;
            String _encryptionKey = null;
            String _encryptionIV = null;

            Element _encryptionElement = XMLUtil.getElement(_patchElement, "encryption", false);
            if (_encryptionElement != null) {
                _encryptionType = XMLUtil.getTextContent(_encryptionElement, "type", true);
                _encryptionKey = XMLUtil.getTextContent(_encryptionElement, "key", true);
                _encryptionIV = XMLUtil.getTextContent(_encryptionElement, "IV", true);
            }

            return new Update(_id, _versionFrom, _versionTo, _path, _encryptionType, _encryptionKey, _encryptionIV);
        }

        protected Element getElement(Document doc) {
            Element _update = doc.createElement("update");
            _update.setAttribute("id", Integer.toString(id));


            Element _version = doc.createElement("version");
            _update.appendChild(_version);

            Element _versionFrom = doc.createElement("from");
            _versionFrom.appendChild(doc.createTextNode(versionFrom));
            _version.appendChild(_versionFrom);

            Element _versionTo = doc.createElement("to");
            _versionTo.appendChild(doc.createTextNode(versionTo));
            _version.appendChild(_versionTo);


            Element _patch = doc.createElement("patch");
            _update.appendChild(_patch);

            Element _path = doc.createElement("path");
            _path.appendChild(doc.createTextNode(path));
            _patch.appendChild(_path);

            if (encryptionType != null) {
                Element _encryption = doc.createElement("encryption");
                _patch.appendChild(_encryption);

                Element _encryptionType = doc.createElement("type");
                _encryptionType.appendChild(doc.createTextNode(encryptionType));
                _encryption.appendChild(_encryptionType);

                Element _encryptionKey = doc.createElement("key");
                _encryptionKey.appendChild(doc.createTextNode(encryptionKey));
                _encryption.appendChild(_encryptionKey);

                Element _encryptionIV = doc.createElement("IV");
                _encryptionIV.appendChild(doc.createTextNode(encryptionIV));
                _encryption.appendChild(_encryptionIV);
            }

            return _update;
        }
    }

    public static class Information {

        protected String softwareName;
        protected String softwareIconLocation;
        protected String softwareIconPath;
        protected String updaterTitle;
        protected String updaterIconLocation;
        protected String updaterIconPath;

        public Information(String softwareName, String softwareIconLocation, String softwareIconPath, String updaterTitle, String updaterIconLocation, String updaterIconPath) {
            this.softwareName = softwareName;
            this.softwareIconLocation = softwareIconLocation;
            this.softwareIconPath = softwareIconPath;
            this.updaterTitle = updaterTitle;
            this.updaterIconLocation = updaterIconLocation;
            this.updaterIconPath = updaterIconPath;
        }

        public String getSoftwareName() {
            return softwareName;
        }

        public void setSoftwareName(String softwareName) {
            this.softwareName = softwareName;
        }

        public String getSoftwareIconLocation() {
            return softwareIconLocation;
        }

        public void setSoftwareIconLocation(String softwareIconLocation) {
            this.softwareIconLocation = softwareIconLocation;
        }

        public String getSoftwareIconPath() {
            return softwareIconPath;
        }

        public void setSoftwareIconPath(String softwareIconPath) {
            this.softwareIconPath = softwareIconPath;
        }

        public String getUpdaterTitle() {
            return updaterTitle;
        }

        public void setUpdaterTitle(String updaterTitle) {
            this.updaterTitle = updaterTitle;
        }

        public String getUpdaterIconLocation() {
            return updaterIconLocation;
        }

        public void setUpdaterIconLocation(String updaterIconLocation) {
            this.updaterIconLocation = updaterIconLocation;
        }

        public String getUpdaterIconPath() {
            return updaterIconPath;
        }

        public void setUpdaterIconPath(String updaterIconPath) {
            this.updaterIconPath = updaterIconPath;
        }

        protected static Information read(Element informationElement) throws InvalidFormatException {
            if (informationElement == null) {
                throw new NullPointerException();
            }

            Element _softwareNameElement = XMLUtil.getElement(informationElement, "software-name", true);
            String _softwareName = _softwareNameElement.getTextContent();

            Element _softwareIconElement = XMLUtil.getElement(informationElement, "software-icon", true);
            String _softwareIconLocation = XMLUtil.getTextContent(_softwareIconElement, "location", true);
            String _softwareIconPath = XMLUtil.getTextContent(_softwareIconElement, "path", true);

            Element _updaterNameElement = XMLUtil.getElement(informationElement, "updater-title", true);
            String _updaterName = _updaterNameElement.getTextContent();

            Element _updaterIconElement = XMLUtil.getElement(informationElement, "updater-icon", true);
            String _updaterIconLocation = XMLUtil.getTextContent(_updaterIconElement, "location", true);
            String _updaterIconPath = XMLUtil.getTextContent(_updaterIconElement, "path", true);

            return new Information(_softwareName, _softwareIconLocation, _softwareIconPath, _updaterName, _updaterIconLocation, _updaterIconPath);
        }

        protected Element getElement(Document doc) {
            Element _information = doc.createElement("information");

            Element _softwareName = doc.createElement("software-name");
            _softwareName.appendChild(doc.createTextNode(softwareName));
            _information.appendChild(_softwareName);

            Element _softwareIcon = doc.createElement("software-icon");
            _information.appendChild(_softwareIcon);

            Element _softwareIconLocation = doc.createElement("location");
            _softwareIconLocation.appendChild(doc.createTextNode(softwareIconLocation));
            _softwareIcon.appendChild(_softwareIconLocation);

            Element _softwareIconPath = doc.createElement("path");
            _softwareIconPath.appendChild(doc.createTextNode(softwareIconPath));
            _softwareIcon.appendChild(_softwareIconPath);

            Element _updaterTitle = doc.createElement("updater-title");
            _updaterTitle.appendChild(doc.createTextNode(updaterTitle));
            _information.appendChild(_updaterTitle);

            Element _updaterIcon = doc.createElement("updater-icon");
            _information.appendChild(_updaterIcon);

            Element _updaterIconLocation = doc.createElement("location");
            _updaterIconLocation.appendChild(doc.createTextNode(updaterIconLocation));
            _updaterIcon.appendChild(_updaterIconLocation);

            Element _updaterIconPath = doc.createElement("path");
            _updaterIconPath.appendChild(doc.createTextNode(updaterIconPath));
            _updaterIcon.appendChild(_updaterIconPath);

            return _information;
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        File file = new File("updater.xml");
        byte[] content = new byte[(int) file.length()];

        FileInputStream fin = new FileInputStream(file);
        fin.read(content);
        fin.close();

        try {
            Client client = Client.read(content);
            System.out.println(client.output());
        } catch (InvalidFormatException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
