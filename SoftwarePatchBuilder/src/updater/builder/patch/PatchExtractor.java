package updater.builder.patch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import updater.builder.util.Util;
import updater.patch.PatchReadUtil;
import updater.script.InvalidFormatException;
import updater.script.Patch;
import updater.script.Patch.Operation;
import updater.util.AESKey;

/**
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

    public static void extract(File patchFile, File saveToFolder, AESKey aesKey, File tempFileForDecryption) throws IOException, InvalidFormatException {
        File _patchFile = patchFile;
        boolean deletePatch = false;

        if (!saveToFolder.isDirectory() && !saveToFolder.exists()) {
            saveToFolder.mkdirs();
        }
        if (!saveToFolder.isDirectory()) {
            throw new IOException("Please specify a valid folder.");
        }

        if (aesKey != null) {
            PatchReadUtil.decrypt(aesKey, patchFile, tempFileForDecryption);

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

            Util.writeFile(new File(saveToFolder.getAbsolutePath() + File.separator + "patch.xml"), patchXML.output().getBytes("UTF-8"));

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
            if (in != null) {
                in.close();
            }
            if (deletePatch) {
                _patchFile.delete();
            }
        }
    }
}
