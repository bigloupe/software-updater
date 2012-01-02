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

  private static final Logger LOG = Logger.getLogger(PatchExtractor.class.getName());

  protected PatchExtractor() {
  }

  /**
   * Extract the patch.
   * @param patchFile the patch file
   * @param saveToFolder where to save the extracted patch file
   * @param aesKey the cipher key, null means no encryption used
   * @param tempFileForDecryption if {@code aesKey} is specified, this should 
   * be provided to store the temporary decrypted file
   * @throws IOException error occurred when extracting
   * @throws InvalidFormatException the format of the patch XML in the patch 
   * is invalid
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
      LOG.log(Level.SEVERE, null, ex);
    } finally {
      CommonUtil.closeQuietly(in);
      if (deletePatch) {
        _patchFile.delete();
      }
    }
  }
}
