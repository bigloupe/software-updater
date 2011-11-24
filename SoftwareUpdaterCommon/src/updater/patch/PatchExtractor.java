package updater.patch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import updater.crypto.AESKey;
import updater.script.InvalidFormatException;
import updater.script.Patch;
import updater.script.Patch.Operation;
import updater.util.CommonUtil;

/**
 * Patch Extractor.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchExtractor {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }

    protected PatchExtractor() {
    }

    /**
     * Extract the patch.
     * @param patchFile the patch file
     * @param saveToFolder where to save the extracted patch file
     * @param aesKey the cipher key, null means no encryption used
     * @param tempFileForDecryption if {@code aesKey} is specified, this should be provided to store the temporary decrypted file
     * @throws IOException error occurred when extracting
     * @throws InvalidFormatException the format of the patch XML in the patch is invalid
     */
    public static void extract(File patchFile, File saveToFolder, AESKey aesKey, File tempFileForDecryption) throws IOException, InvalidFormatException {
        if (patchFile == null) {
            throw new NullPointerException("argument 'patchFile' cannot be null");
        }
        if (saveToFolder == null) {
            throw new NullPointerException("argument 'saveToFolder' cannot be null");
        }
        if (aesKey != null && tempFileForDecryption == null) {
            throw new NullPointerException("argument 'tempFileForDecryption' cannot be null while argument 'aesKey' is not null");
        }

        File _patchFile = patchFile;
        boolean deletePatch = false;

        if (!saveToFolder.isDirectory() && !saveToFolder.exists()) {
            saveToFolder.mkdirs();
        }
        if (!saveToFolder.isDirectory()) {
            throw new IOException("Please specify a valid folder 'saveToFolder'.");
        }

        if (aesKey != null) {
            PatchReadUtil.decrypt(aesKey, null, patchFile, tempFileForDecryption);

            _patchFile = tempFileForDecryption;
            _patchFile.deleteOnExit();
            deletePatch = true;
        }

        FileInputStream in = null;
        try {
            in = new FileInputStream(_patchFile);

            PatchReadUtil.readHeader(in);
            InputStream decompressedIn = PatchReadUtil.readCompressionMethod(in);
            Patch patchXML = PatchReadUtil.readXML(decompressedIn);

            CommonUtil.writeFile(new File(saveToFolder.getAbsolutePath() + File.separator + "patch.xml"), patchXML.output());

            int id = 1;
            List<Operation> operations = patchXML.getOperations();
            for (Operation operation : operations) {
                if (operation.getPatchLength() > 0) {
                    PatchReadUtil.readToFile(new File(saveToFolder.getAbsolutePath() + File.separator + id), decompressedIn, operation.getPatchLength());
                }
                id++;
            }
        } catch (TransformerException ex) {
            if (debug) {
                Logger.getLogger(PatchCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
            CommonUtil.closeQuietly(in);
            if (deletePatch) {
                _patchFile.delete();
            }
        }
    }
}
