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
public class Client {

    protected String version;
    //
    protected String storagePath;
    //
    protected Information information;
    //
    protected String launchType;
    protected String launchAfterLaunch;
    protected String launchCommand;
    protected String launchJarPath;
    protected String launchMainClass;
    //
    protected String catalogUrl;
    protected String catalogPublicKeyModulus;
    protected String catalogPublicKeyExponent;
    protected long catalogLastUpdated;
    //
    protected List<Patch> patches;

    public Client(String version,
            String storagePath,
            Information information,
            String launchType, String launchAfterLaunch, String launchCommand, String launchJarPath, String launchMainClass,
            String catalogUrl, String catalogPublicKeyModulus, String catalogPublicKeyExponent, long catalogLastUpdated,
            List<Patch> patches) {
        this.version = version;

        this.storagePath = storagePath;

        this.information = information;

        this.launchType = launchType;
        this.launchAfterLaunch = launchAfterLaunch;
        this.launchCommand = launchCommand;
        this.launchJarPath = launchJarPath;
        this.launchMainClass = launchMainClass;

        this.catalogUrl = catalogUrl;
        this.catalogLastUpdated = catalogLastUpdated;
        this.catalogPublicKeyModulus = catalogPublicKeyModulus;
        this.catalogPublicKeyExponent = catalogPublicKeyExponent;

        this.patches = new ArrayList<Patch>(patches);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public String getLaunchType() {
        return launchType;
    }

    public void setLaunchType(String launchType) {
        this.launchType = launchType;
    }

    public String getLaunchAfterLaunch() {
        return launchAfterLaunch;
    }

    public void setLaunchAfterLaunch(String launchAfterLaunch) {
        this.launchAfterLaunch = launchAfterLaunch;
    }

    public String getLaunchCommand() {
        return launchCommand;
    }

    public void setLaunchCommand(String launchCommand) {
        this.launchCommand = launchCommand;
    }

    public String getLaunchJarPath() {
        return launchJarPath;
    }

    public void setJarLaunchPath(String launchJarPath) {
        this.launchJarPath = launchJarPath;
    }

    public String getLaunchMainClass() {
        return launchMainClass;
    }

    public void setLaunchMainClass(String launchMainClass) {
        this.launchMainClass = launchMainClass;
    }

    public String getCatalogUrl() {
        return catalogUrl;
    }

    public void setCatalogUrl(String catalogUrl) {
        this.catalogUrl = catalogUrl;
    }

    public String getCatalogPublicKeyModulus() {
        return catalogPublicKeyModulus;
    }

    public void setCatalogPublicKeyModulus(String catalogPublicKeyModulus) {
        this.catalogPublicKeyModulus = catalogPublicKeyModulus;
    }

    public String getCatalogPublicKeyExponent() {
        return catalogPublicKeyExponent;
    }

    public void setCatalogPublicKeyExponent(String catalogPublicKeyExponent) {
        this.catalogPublicKeyExponent = catalogPublicKeyExponent;
    }

    public long getCatalogLastUpdated() {
        return catalogLastUpdated;
    }

    public void setCatalogLastUpdated(long catalogLastUpdated) {
        this.catalogLastUpdated = catalogLastUpdated;
    }

    public List<Patch> getPatches() {
        return new ArrayList<Patch>(patches);
    }

    public void setPatches(List<Patch> updates) {
        this.patches = new ArrayList<Patch>(updates);
    }

    public static Client read(byte[] content) throws InvalidFormatException {
        Document doc = XMLUtil.readDocument(content);
        if (doc == null) {
            throw new InvalidFormatException("XML format incorrect.");
        }

        Element _rootNode = doc.getDocumentElement();

        String _version = XMLUtil.getTextContent(_rootNode, "version", true);

        String _storagePath = XMLUtil.getTextContent(_rootNode, "storage-path", true);

        Element _informationNode = XMLUtil.getElement(_rootNode, "information", false);
        Information _information = null;
        if (_informationNode != null) {
            _information = Information.read(_informationNode);
        }

        String _launchType = null;
        String _launchAfterLaunch = null;
        String _launchCommand = null;
        String _launchJarPath = null;
        String _launchMainClass = null;
        Element _launchNode = XMLUtil.getElement(_rootNode, "launch", false);
        if (_launchNode != null) {
            _launchType = XMLUtil.getTextContent(_launchNode, "type", true);
            _launchAfterLaunch = XMLUtil.getTextContent(_launchNode, "after-launch", false);
            _launchCommand = XMLUtil.getTextContent(_launchNode, "command", false);
            _launchJarPath = XMLUtil.getTextContent(_launchNode, "jar-path", false);
            _launchMainClass = XMLUtil.getTextContent(_launchNode, "main-class", false);

            if (_launchType.equals("jar") && (_launchJarPath == null || _launchMainClass == null)) {
                throw new InvalidFormatException("Launch type if 'jar', <jar-path> and <main-class> must exist under <launch>.");
            }
            if (_launchType.equals("command") && _launchCommand == null) {
                throw new InvalidFormatException("Launch type if 'command', <command> must exist under <launch>.");
            }
        }

        String _catalogUrl = null;
        String _catalogPublicKeyModulus = null;
        String _catalogPublicKeyExponent = null;
        Long _catalogLastUpdated = -1L;
        Element _catalogNode = XMLUtil.getElement(_rootNode, "catalog", false);
        if (_catalogNode != null) {
            _catalogUrl = XMLUtil.getTextContent(_catalogNode, "url", true);

            Element catalogPublicKeyElement = XMLUtil.getElement(_catalogNode, "public-key", false);
            if (catalogPublicKeyElement != null) {
                _catalogPublicKeyModulus = XMLUtil.getTextContent(catalogPublicKeyElement, "modulus", true);
                _catalogPublicKeyExponent = XMLUtil.getTextContent(catalogPublicKeyElement, "exponent", true);
            }

            String _catalogLastUpdatedString = XMLUtil.getTextContent(_catalogNode, "last-updated", false);
            _catalogLastUpdated = _catalogLastUpdatedString != null ? Long.parseLong(_catalogLastUpdatedString) : -1;
        }

        List<Patch> patches = new ArrayList<Patch>();
        Element _patchesElement = XMLUtil.getElement(_rootNode, "patches", false);
        if (_patchesElement != null) {
            NodeList _patchNodeList = _patchesElement.getElementsByTagName("patch");
            for (int i = 0, iEnd = _patchNodeList.getLength(); i < iEnd; i++) {
                Element _patchNode = (Element) _patchNodeList.item(i);
                patches.add(Patch.read(_patchNode));
            }
        }

        return new Client(_version,
                _storagePath, _information,
                _launchType, _launchAfterLaunch, _launchCommand, _launchJarPath, _launchMainClass,
                _catalogUrl, _catalogPublicKeyModulus, _catalogPublicKeyExponent, _catalogLastUpdated,
                patches);
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

        Element storagePathElement = doc.createElement("storage-path");
        storagePathElement.setTextContent(storagePath);
        rootElement.appendChild(storagePathElement);

        if (information != null) {
            Element informationElement = information.getElement(doc);
            rootElement.appendChild(informationElement);
        }

        if (launchType != null) {
            Element launchElement = doc.createElement("launch");
            rootElement.appendChild(launchElement);

            Element launchTypeElement = doc.createElement("type");
            launchTypeElement.setTextContent(launchType);
            launchElement.appendChild(launchTypeElement);

            if (launchAfterLaunch != null) {
                Element launchAfterLaunchElement = doc.createElement("after-launch");
                launchAfterLaunchElement.setTextContent(launchAfterLaunch);
                launchElement.appendChild(launchAfterLaunchElement);
            }
            if (launchType.equals("command")) {
                Element launchCommandElement = doc.createElement("command");
                launchCommandElement.setTextContent(launchCommand);
                launchElement.appendChild(launchCommandElement);
            }
            if (launchType.equals("jar")) {
                Element launchJarPathElement = doc.createElement("jar-path");
                launchJarPathElement.setTextContent(launchJarPath);
                launchElement.appendChild(launchJarPathElement);

                Element launchMainClassElement = doc.createElement("main-class");
                launchMainClassElement.setTextContent(launchMainClass);
                launchElement.appendChild(launchMainClassElement);
            }
        }

        if (catalogUrl != null) {
            Element catalogElement = doc.createElement("catalog");
            rootElement.appendChild(catalogElement);

            Element catalogUrlElement = doc.createElement("url");
            catalogUrlElement.setTextContent(catalogUrl);
            catalogElement.appendChild(catalogUrlElement);
            if (catalogPublicKeyModulus != null) {
                Element catalogPublicKeyElement = doc.createElement("public-key");
                catalogElement.appendChild(catalogPublicKeyElement);

                Element catalogPublicKeyModulusElement = doc.createElement("modulus");
                catalogPublicKeyModulusElement.setTextContent(catalogPublicKeyModulus);
                catalogPublicKeyElement.appendChild(catalogPublicKeyModulusElement);

                Element catalogPublicKeyExponentElement = doc.createElement("exponent");
                catalogPublicKeyExponentElement.setTextContent(catalogPublicKeyExponent);
                catalogPublicKeyElement.appendChild(catalogPublicKeyExponentElement);
            }
            if (catalogLastUpdated != -1) {
                Element catalogLastUpdatedElement = doc.createElement("last-updated");
                catalogLastUpdatedElement.setTextContent(Long.toString(catalogLastUpdated));
                catalogElement.appendChild(catalogLastUpdatedElement);
            }
        }

        if (!patches.isEmpty()) {
            Element patchesElement = doc.createElement("patches");
            rootElement.appendChild(patchesElement);
            for (Patch patch : patches) {
                Element _patchElement = patch.getElement(doc);
                if (_patchElement != null) {
                    patchesElement.appendChild(_patchElement);
                }
            }
        }

        return XMLUtil.getOutput(doc);
    }

    public static class Information {

        protected String softwareName;
        protected String softwareIconLocation;
        protected String softwareIconPath;
        protected String launcherName;
        protected String launcherIconLocation;
        protected String launcherIconPath;
        protected String downloaderName;
        protected String downloaderIconLocation;
        protected String downloaderIconPath;

        public Information(String softwareName, String softwareIconLocation, String softwareIconPath,
                String launcherTitle, String launcherIconLocation, String launcherIconPath,
                String downloaderTitle, String downloaderIconLocation, String downloaderIconPath) {
            this.softwareName = softwareName;
            this.softwareIconLocation = softwareIconLocation;
            this.softwareIconPath = softwareIconPath;
            this.launcherName = launcherTitle;
            this.launcherIconLocation = launcherIconLocation;
            this.launcherIconPath = launcherIconPath;
            this.downloaderName = downloaderTitle;
            this.downloaderIconLocation = downloaderIconLocation;
            this.downloaderIconPath = downloaderIconPath;
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

        public String getLauncherTitle() {
            return launcherName;
        }

        public void setLauncherName(String launcherName) {
            this.launcherName = launcherName;
        }

        public String getLauncherIconLocation() {
            return launcherIconLocation;
        }

        public void setLauncherIconLocation(String launcherIconLocation) {
            this.launcherIconLocation = launcherIconLocation;
        }

        public String getLauncherIconPath() {
            return launcherIconPath;
        }

        public void setLauncherIconPath(String launcherIconPath) {
            this.launcherIconPath = launcherIconPath;
        }

        public String getDownloaderName() {
            return downloaderName;
        }

        public void setDownloaderName(String downloaderName) {
            this.downloaderName = downloaderName;
        }

        public String getDownloaderIconLocation() {
            return downloaderIconLocation;
        }

        public void setDownloaderIconLocation(String downloaderIconLocation) {
            this.downloaderIconLocation = downloaderIconLocation;
        }

        public String getDownloaderIconPath() {
            return downloaderIconPath;
        }

        public void setDownloaderIconPath(String downloaderIconPath) {
            this.downloaderIconPath = downloaderIconPath;
        }

        protected static Information read(Element informationElement) throws InvalidFormatException {
            if (informationElement == null) {
                throw new NullPointerException();
            }

            String _softwareName = null;
            String _softwareIconLocation = null;
            String _softwareIconPath = null;
            Element _softwareElement = XMLUtil.getElement(informationElement, "software", false);
            if (_softwareElement != null) {
                _softwareName = XMLUtil.getTextContent(_softwareElement, "name", true);

                Element _softwareIconElement = XMLUtil.getElement(_softwareElement, "icon", true);
                _softwareIconLocation = XMLUtil.getTextContent(_softwareIconElement, "location", true);
                _softwareIconPath = XMLUtil.getTextContent(_softwareIconElement, "path", true);
            }

            String _launcherName = null;
            String _launcherIconLocation = null;
            String _launcherIconPath = null;
            Element _launcherElement = XMLUtil.getElement(informationElement, "launcher", false);
            if (_launcherElement != null) {
                _launcherName = XMLUtil.getTextContent(_launcherElement, "name", true);

                Element _launcherIconElement = XMLUtil.getElement(_launcherElement, "icon", true);
                _launcherIconLocation = XMLUtil.getTextContent(_launcherIconElement, "location", true);
                _launcherIconPath = XMLUtil.getTextContent(_launcherIconElement, "path", true);
            }

            String _downloaderName = null;
            String _downloaderIconLocation = null;
            String _downloaderIconPath = null;
            Element _downloaderElement = XMLUtil.getElement(informationElement, "downloader", false);
            if (_downloaderElement != null) {
                _downloaderName = XMLUtil.getTextContent(_downloaderElement, "name", true);

                Element _downloaderIconElement = XMLUtil.getElement(_downloaderElement, "icon", true);
                _downloaderIconLocation = XMLUtil.getTextContent(_downloaderIconElement, "location", true);
                _downloaderIconPath = XMLUtil.getTextContent(_downloaderIconElement, "path", true);
            }

            return new Information(_softwareName, _softwareIconLocation, _softwareIconPath,
                    _launcherName, _launcherIconLocation, _launcherIconPath,
                    _downloaderName, _downloaderIconLocation, _downloaderIconPath);
        }

        protected Element getElement(Document doc) {
            Element _information = doc.createElement("information");


            if (softwareName != null) {
                Element _software = doc.createElement("software");
                _information.appendChild(_software);

                Element _softwareName = doc.createElement("name");
                _softwareName.appendChild(doc.createTextNode(softwareName));
                _software.appendChild(_softwareName);

                Element _softwareIcon = doc.createElement("icon");
                _software.appendChild(_softwareIcon);

                Element _softwareIconLocation = doc.createElement("location");
                _softwareIconLocation.appendChild(doc.createTextNode(softwareIconLocation));
                _softwareIcon.appendChild(_softwareIconLocation);

                Element _softwareIconPath = doc.createElement("path");
                _softwareIconPath.appendChild(doc.createTextNode(softwareIconPath));
                _softwareIcon.appendChild(_softwareIconPath);
            }


            if (launcherName != null) {
                Element _launcher = doc.createElement("launcher");
                _information.appendChild(_launcher);

                Element _launcherName = doc.createElement("name");
                _launcherName.appendChild(doc.createTextNode(launcherName));
                _launcher.appendChild(_launcherName);

                Element _launcherIcon = doc.createElement("icon");
                _launcher.appendChild(_launcherIcon);

                Element _launcherIconLocation = doc.createElement("location");
                _launcherIconLocation.appendChild(doc.createTextNode(launcherIconLocation));
                _launcherIcon.appendChild(_launcherIconLocation);

                Element _launcherIconPath = doc.createElement("path");
                _launcherIconPath.appendChild(doc.createTextNode(launcherIconPath));
                _launcherIcon.appendChild(_launcherIconPath);
            }


            if (downloaderName != null) {
                Element _downloader = doc.createElement("downloader");
                _information.appendChild(_downloader);

                Element _downloaderName = doc.createElement("name");
                _downloaderName.appendChild(doc.createTextNode(downloaderName));
                _downloader.appendChild(_downloaderName);

                Element _downloaderIcon = doc.createElement("icon");
                _downloader.appendChild(_downloaderIcon);

                Element _downloaderIconLocation = doc.createElement("location");
                _downloaderIconLocation.appendChild(doc.createTextNode(downloaderIconLocation));
                _downloaderIcon.appendChild(_downloaderIconLocation);

                Element _downloaderIconPath = doc.createElement("path");
                _downloaderIconPath.appendChild(doc.createTextNode(downloaderIconPath));
                _downloaderIcon.appendChild(_downloaderIconPath);
            }


            return _information;
        }
    }

    public static void main(String[] args) {
        try {
            Client client = Client.read(CommonUtil.readFile(new File("client.xml")));
            System.out.println(client.output());
        } catch (InvalidFormatException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
