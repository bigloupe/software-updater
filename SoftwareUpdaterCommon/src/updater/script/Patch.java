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
public class Patch {

    protected int id;
    protected String versionFrom;
    protected String versionFromSubsequent;
    protected String versionTo;
    protected String downloadUrl;
    protected String downloadChecksum;
    protected int downloadLength;
    protected String downloadEncryptionType;
    protected String downloadEncryptionKey;
    protected String downloadEncryptionIV;
    protected List<Operation> operations;
    protected List<ValidationFile> validations;

    public Patch(int id,
            String versionFrom, String versionFromSubsequent, String versionTo,
            String downloadUrl, String downloadChecksum, int downloadLength,
            String downloadEncryptionType, String downloadEncryptionKey, String downloadEncryptionIV,
            List<Operation> operations, List<ValidationFile> validations) {
        this.id = id;

        this.versionFrom = versionFrom;
        this.versionFromSubsequent = versionFromSubsequent;
        this.versionTo = versionTo;

        this.downloadUrl = downloadUrl;
        this.downloadChecksum = downloadChecksum;
        this.downloadLength = downloadLength;
        this.downloadEncryptionType = downloadEncryptionType;
        this.downloadEncryptionKey = downloadEncryptionKey;
        this.downloadEncryptionIV = downloadEncryptionIV;

        this.operations = new ArrayList<Operation>(operations);
        this.validations = new ArrayList<ValidationFile>(validations);
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

    public String getVersionFromSubsequent() {
        return versionFromSubsequent;
    }

    public void setVersionFromSubsequent(String versionFromSubsequent) {
        this.versionFromSubsequent = versionFromSubsequent;
    }

    public String getVersionTo() {
        return versionTo;
    }

    public void setVersionTo(String versionTo) {
        this.versionTo = versionTo;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getDownloadChecksum() {
        return downloadChecksum;
    }

    public void setDownloadChecksum(String downloadChecksum) {
        this.downloadChecksum = downloadChecksum;
    }

    public int getDownloadLength() {
        return downloadLength;
    }

    public void setDownloadLength(int downloadLength) {
        this.downloadLength = downloadLength;
    }

    public String getDownloadEncryptionType() {
        return downloadEncryptionType;
    }

    public void setDownloadEncryptionType(String downloadEncryptionType) {
        this.downloadEncryptionType = downloadEncryptionType;
    }

    public String getDownloadEncryptionKey() {
        return downloadEncryptionKey;
    }

    public void setDownloadEncryptionKey(String downloadEncryptionKey) {
        this.downloadEncryptionKey = downloadEncryptionKey;
    }

    public String getDownloadEncryptionIV() {
        return downloadEncryptionIV;
    }

    public void setDownloadEncryptionIV(String downloadEncryptionIV) {
        this.downloadEncryptionIV = downloadEncryptionIV;
    }

    public List<Operation> getOperations() {
        return new ArrayList<Operation>(operations);
    }

    public void setOperations(List<Operation> files) {
        this.operations = new ArrayList<Operation>(files);
    }

    public List<ValidationFile> getValidations() {
        return new ArrayList<ValidationFile>(validations);
    }

    public void setValidations(List<ValidationFile> validations) {
        this.validations = new ArrayList<ValidationFile>(validations);
    }

    public static Patch read(byte[] content) throws InvalidFormatException {
        Document doc = XMLUtil.readDocument(content);
        if (doc == null) {
            throw new InvalidFormatException("XML format incorrect.");
        }

        return read(doc.getDocumentElement());
    }

    public static Patch read(Element patchElement) throws InvalidFormatException {
        int _id = 0;
        try {
            _id = Integer.parseInt(patchElement.getAttribute("id"));
        } catch (Exception ex) {
            throw new InvalidFormatException("attribute 'id' for 'update' element not exist");
        }

        Element _versionElement = XMLUtil.getElement(patchElement, "version", true);
        String _versionFrom = XMLUtil.getTextContent(_versionElement, "from", false);
        String _versionFromSubsequent = XMLUtil.getTextContent(_versionElement, "from-subsequent", false);
        String _versionTo = XMLUtil.getTextContent(_versionElement, "to", true);

        String _downloadUrl = null;
        String _downloadChecksum = null;
        int _downloadLength = -1;
        String _downloadEncryptionType = null;
        String _downloadEncryptionKey = null;
        String _downloadEncryptionIV = null;
        Element _downloadElement = XMLUtil.getElement(patchElement, "download", false);
        if (_downloadElement != null) {
            _downloadUrl = XMLUtil.getTextContent(_downloadElement, "url", true);
            _downloadChecksum = XMLUtil.getTextContent(_downloadElement, "checksum", true);
            _downloadLength = Integer.parseInt(XMLUtil.getTextContent(_downloadElement, "length", true));

            Element _downloadEncryptionElement = XMLUtil.getElement(_downloadElement, "encryption", false);
            if (_downloadEncryptionElement != null) {
                _downloadEncryptionType = XMLUtil.getTextContent(_downloadEncryptionElement, "type", true);
                _downloadEncryptionKey = XMLUtil.getTextContent(_downloadEncryptionElement, "key", true);
                _downloadEncryptionIV = XMLUtil.getTextContent(_downloadEncryptionElement, "IV", true);
            }
        }

        if (_versionFrom == null && _versionFromSubsequent == null) {
            throw new InvalidFormatException("<from> or <from-subsequent> must exist under <version>.");
        } else if (_versionFrom != null && _versionFromSubsequent != null) {
            throw new InvalidFormatException("<version> cannot contain both <from> and <from-subsequent>.");
        }

        List<Operation> _operations = new ArrayList<Operation>();
        Element operationsElement = XMLUtil.getElement(patchElement, "operations", false);
        if (operationsElement != null) {
            NodeList _operationNodeList = operationsElement.getElementsByTagName("operation");
            for (int i = 0, iEnd = _operationNodeList.getLength(); i < iEnd; i++) {
                Element _operationNode = (Element) _operationNodeList.item(i);
                _operations.add(Operation.read(_operationNode));
            }
        }

        List<ValidationFile> _validations = new ArrayList<ValidationFile>();
        Element validationsElement = XMLUtil.getElement(patchElement, "validations", false);
        if (validationsElement != null) {
            NodeList _validationFileNodeList = validationsElement.getElementsByTagName("file");
            for (int i = 0, iEnd = _validationFileNodeList.getLength(); i < iEnd; i++) {
                Element _validationFileNode = (Element) _validationFileNodeList.item(i);
                _validations.add(ValidationFile.read(_validationFileNode));
            }
        }

        return new Patch(_id,
                _versionFrom, _versionFromSubsequent, _versionTo,
                _downloadUrl, _downloadChecksum, _downloadLength,
                _downloadEncryptionType, _downloadEncryptionKey, _downloadEncryptionIV,
                _operations, _validations);
    }

    public String output() {
        Document doc = XMLUtil.createEmptyDocument();
        if (doc == null) {
            return null;
        }

        Element patchElement = getElement(doc);
        doc.appendChild(patchElement);

        return XMLUtil.getOutput(doc);
    }

    public Element getElement(Document doc) {
        Element patchElement = doc.createElement("patch");
        patchElement.setAttribute("id", Integer.toString(id));

        Element versionElement = doc.createElement("version");
        patchElement.appendChild(versionElement);

        if (versionFrom != null) {
            Element versionFromElement = doc.createElement("from");
            versionFromElement.setTextContent(versionFrom);
            versionElement.appendChild(versionFromElement);
        } else if (versionFromSubsequent != null) {
            Element versionFromSubsequentElement = doc.createElement("from-subsequent");
            versionFromSubsequentElement.setTextContent(versionFromSubsequent);
            versionElement.appendChild(versionFromSubsequentElement);
        }
        Element versionToElement = doc.createElement("to");
        versionToElement.setTextContent(versionTo);
        versionElement.appendChild(versionToElement);

        if (downloadUrl != null) {
            Element downloadElement = doc.createElement("download");
            patchElement.appendChild(downloadElement);

            Element downloadUrlElement = doc.createElement("url");
            downloadUrlElement.setTextContent(downloadUrl);
            downloadElement.appendChild(downloadUrlElement);

            Element downloadChecksumElement = doc.createElement("checksum");
            downloadChecksumElement.setTextContent(downloadChecksum);
            downloadElement.appendChild(downloadChecksumElement);

            Element downloadLengthElement = doc.createElement("length");
            downloadLengthElement.setTextContent(Integer.toString(downloadLength));
            downloadElement.appendChild(downloadLengthElement);

            if (downloadEncryptionType != null) {
                Element downloadEncryptionElement = doc.createElement("encryption");
                downloadElement.appendChild(downloadEncryptionElement);

                Element downloadEncryptionTypeElement = doc.createElement("type");
                downloadEncryptionTypeElement.setTextContent(downloadEncryptionType);
                downloadEncryptionElement.appendChild(downloadEncryptionTypeElement);

                Element downloadEncryptionKeyElement = doc.createElement("key");
                downloadEncryptionKeyElement.setTextContent(downloadEncryptionKey);
                downloadEncryptionElement.appendChild(downloadEncryptionKeyElement);

                Element downloadEncryptionIVElement = doc.createElement("IV");
                downloadEncryptionIVElement.setTextContent(downloadEncryptionIV);
                downloadEncryptionElement.appendChild(downloadEncryptionIVElement);
            }
        }

        if (!operations.isEmpty()) {
            Element operationsElement = doc.createElement("operations");
            patchElement.appendChild(operationsElement);
            for (Operation operation : operations) {
                operationsElement.appendChild(operation.getElement(doc));
            }
        }

        if (!validations.isEmpty()) {
            Element validationsElement = doc.createElement("validations");
            patchElement.appendChild(validationsElement);
            for (ValidationFile file : validations) {
                validationsElement.appendChild(file.getElement(doc));
            }
        }

        return patchElement;
    }

    public static class Operation {

        protected String type;
        //
        protected int patchPos;
        protected int patchLength;
        //
        protected String fileType;
        //
        protected String oldFilePath;
        protected String oldFileChecksum;
        protected int oldFileLength;
        //
        protected String newFilePath;
        protected String newFileChecksum;
        protected int newFileLength;

        public Operation(String type, int patchPos, int patchLength, String fileType, String oldFilePath, String oldFileChecksum, int oldFileLength, String newFilePath, String newFileChecksum, int newFileLength) {
            this.type = type;
            this.patchPos = patchPos;
            this.patchLength = patchLength;
            this.fileType = fileType;
            this.oldFilePath = oldFilePath;
            this.oldFileChecksum = oldFileChecksum;
            this.oldFileLength = oldFileLength;
            this.newFilePath = newFilePath;
            this.newFileChecksum = newFileChecksum;
            this.newFileLength = newFileLength;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getPatchPos() {
            return patchPos;
        }

        public void setPatchPos(int patchPos) {
            this.patchPos = patchPos;
        }

        public int getPatchLength() {
            return patchLength;
        }

        public void setPatchLength(int patchLength) {
            this.patchLength = patchLength;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public String getOldFilePath() {
            return oldFilePath;
        }

        public void setOldFilePath(String oldFilePath) {
            this.oldFilePath = oldFilePath;
        }

        public String getOldFileChecksum() {
            return oldFileChecksum;
        }

        public void setOldFileChecksum(String oldFileChecksum) {
            this.oldFileChecksum = oldFileChecksum;
        }

        public int getOldFileLength() {
            return oldFileLength;
        }

        public void setOldFileLength(int oldFileLength) {
            this.oldFileLength = oldFileLength;
        }

        public String getNewFilePath() {
            return newFilePath;
        }

        public void setNewFilePath(String newFilePath) {
            this.newFilePath = newFilePath;
        }

        public String getNewFileChecksum() {
            return newFileChecksum;
        }

        public void setNewFileChecksum(String newFileChecksum) {
            this.newFileChecksum = newFileChecksum;
        }

        public int getNewFileLength() {
            return newFileLength;
        }

        public void setNewFileLength(int newFileLength) {
            this.newFileLength = newFileLength;
        }

        protected static Operation read(Element operationElement) throws InvalidFormatException {
            if (operationElement == null) {
                throw new NullPointerException();
            }

            String _type = XMLUtil.getTextContent(operationElement, "type", true);

            int pos = -1;
            int length = -1;
            if (_type.equals("patch") || _type.equals("replace") || _type.equals("new") || _type.equals("force")) {
                Element _contentElement = XMLUtil.getElement(operationElement, "content", true);
                pos = Integer.parseInt(XMLUtil.getTextContent(_contentElement, "pos", true));
                length = Integer.parseInt(XMLUtil.getTextContent(_contentElement, "length", true));
            }

            String _fileType = XMLUtil.getTextContent(operationElement, "file-type", true);

            String oldPath = null;
            String oldChecksum = null;
            int oldLength = -1;
            if (_type.equals("patch") || _type.equals("replace") || _type.equals("remove")) {
                Element _oldFileElement = XMLUtil.getElement(operationElement, "old-file", true);
                oldPath = XMLUtil.getTextContent(_oldFileElement, "path", true);
                oldChecksum = XMLUtil.getTextContent(_oldFileElement, "checksum", true);
                oldLength = Integer.parseInt(XMLUtil.getTextContent(_oldFileElement, "length", true));
            }

            String newPath = null;
            String newChecksum = null;
            int newLength = -1;
            if (_type.equals("patch") || _type.equals("replace") || _type.equals("new") || _type.equals("force")) {
                Element _newFileElement = XMLUtil.getElement(operationElement, "new-file", true);
                newPath = XMLUtil.getTextContent(_newFileElement, "path", true);
                newChecksum = XMLUtil.getTextContent(_newFileElement, "checksum", true);
                newLength = Integer.parseInt(XMLUtil.getTextContent(_newFileElement, "length", true));
            }

            return new Operation(_type, pos, length, _fileType, oldPath, oldChecksum, oldLength, newPath, newChecksum, newLength);
        }

        protected Element getElement(Document doc) {
            Element _operation = doc.createElement("operation");

            Element _type = doc.createElement("type");
            _type.appendChild(doc.createTextNode(type));
            _operation.appendChild(_type);

            //<editor-fold defaultstate="collapsed" desc="content">
            if (patchPos != -1) {
                Element _patch = doc.createElement("content");
                _operation.appendChild(_patch);

                Element _patchUrl = doc.createElement("pos");
                _patchUrl.appendChild(doc.createTextNode(Integer.toString(patchPos)));
                _patch.appendChild(_patchUrl);

                Element _patchLength = doc.createElement("length");
                _patchLength.appendChild(doc.createTextNode(Integer.toString(patchLength)));
                _patch.appendChild(_patchLength);
            }
            //</editor-fold>

            Element _fileType = doc.createElement("file-type");
            _fileType.appendChild(doc.createTextNode(fileType));
            _operation.appendChild(_fileType);

            //<editor-fold defaultstate="collapsed" desc="old">
            if (oldFilePath != null) {
                Element _old = doc.createElement("old-file");
                _operation.appendChild(_old);

                Element _oldFilePath = doc.createElement("path");
                _oldFilePath.appendChild(doc.createTextNode(oldFilePath));
                _old.appendChild(_oldFilePath);

                Element _oldFileChecksum = doc.createElement("checksum");
                _oldFileChecksum.appendChild(doc.createTextNode(oldFileChecksum));
                _old.appendChild(_oldFileChecksum);

                Element _oldFileLength = doc.createElement("length");
                _oldFileLength.appendChild(doc.createTextNode(Integer.toString(oldFileLength)));
                _old.appendChild(_oldFileLength);
            }
            //</editor-fold>

            //<editor-fold defaultstate="collapsed" desc="new">
            if (newFilePath != null) {
                Element _new = doc.createElement("new-file");
                _operation.appendChild(_new);

                Element _newFilePath = doc.createElement("path");
                _newFilePath.appendChild(doc.createTextNode(newFilePath));
                _new.appendChild(_newFilePath);

                Element _newFileChecksum = doc.createElement("checksum");
                _newFileChecksum.appendChild(doc.createTextNode(newFileChecksum));
                _new.appendChild(_newFileChecksum);

                Element _newFileLength = doc.createElement("length");
                _newFileLength.appendChild(doc.createTextNode(Integer.toString(newFileLength)));
                _new.appendChild(_newFileLength);
            }
            //</editor-fold>

            return _operation;
        }
    }

    public static class ValidationFile {

        protected String filePath;
        protected String fileChecksum;
        protected int fileLength;

        public ValidationFile(String filePath, String fileChecksum, int fileLength) {
            this.filePath = filePath;
            this.fileChecksum = fileChecksum;
            this.fileLength = fileLength;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getFileChecksum() {
            return fileChecksum;
        }

        public void setFileChecksum(String fileChecksum) {
            this.fileChecksum = fileChecksum;
        }

        public int getFileLength() {
            return fileLength;
        }

        public void setFileLength(int fileLength) {
            this.fileLength = fileLength;
        }

        protected static ValidationFile read(Element fileElement) throws InvalidFormatException {
            if (fileElement == null) {
                throw new NullPointerException();
            }

            String _path = XMLUtil.getTextContent(fileElement, "path", true);
            String _checksum = XMLUtil.getTextContent(fileElement, "checksum", true);
            int _length = Integer.parseInt(XMLUtil.getTextContent(fileElement, "length", true));

            return new ValidationFile(_path, _checksum, _length);
        }

        protected Element getElement(Document doc) {
            Element _file = doc.createElement("file");

            Element _path = doc.createElement("path");
            _path.appendChild(doc.createTextNode(filePath));
            _file.appendChild(_path);

            Element _checksum = doc.createElement("checksum");
            _checksum.appendChild(doc.createTextNode(fileChecksum));
            _file.appendChild(_checksum);

            Element _length = doc.createElement("length");
            _length.appendChild(doc.createTextNode(Integer.toString(fileLength)));
            _file.appendChild(_length);

            return _file;
        }
    }

    public static void main(String[] args) {
        try {
            Patch patch = Patch.read(CommonUtil.readFile(new File("patch.xml")));
            System.out.println(patch.output());
        } catch (InvalidFormatException ex) {
            Logger.getLogger(Patch.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
