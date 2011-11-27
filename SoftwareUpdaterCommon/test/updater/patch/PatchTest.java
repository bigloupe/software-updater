package updater.patch;

import updater.crypto.AESKey;
import java.io.File;
import updater.TestCommon;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import updater.crypto.KeyGenerator;
import updater.util.CommonUtil;
import static org.junit.Assert.*;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchTest {

    protected final String packagePath = TestCommon.pathToTestPackage + this.getClass().getCanonicalName().replace('.', '/') + "/";

    public PatchTest() {
    }

    protected static String getClassName() {
        return new Object() {
        }.getClass().getEnclosingClass().getName();
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("***** " + getClassName() + " *****");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        System.out.println("******************************\r\n");
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void test() throws Exception {
        System.out.println("+++++ test +++++");

        // follow the folder name and change the path to do real software patching test
        File oldFolder = new File(packagePath + "PatchTest_old");
        File newFolder = new File(packagePath + "PatchTest_new");
        File newOverOldFolder = new File(packagePath + "PatchTest_new_over_old"); // this is the folder that contains all files after applying full patch of 'new' on 'old'

        File tempDir = new File("PatchTest_tmp_dir_na4Ja");
        tempDir.mkdirs();
        CommonUtil.truncateFolder(tempDir);

        File aesKeyFile = new File(tempDir.getAbsolutePath() + File.separator + "aes.xml");
        KeyGenerator.generateAES(256, aesKeyFile);
        AESKey aesKey = AESKey.read(CommonUtil.readFile(aesKeyFile));


        File patch = new File(tempDir.getAbsolutePath() + File.separator + "patch.patch");
        File tempDirForCreatePatch = new File(tempDir.getAbsolutePath() + File.separator + "create_patch");
        File tempFileForPatchEncryption = new File(patch.getAbsolutePath() + ".encrypted");
        File tempDirForPatch = new File(tempDir.getAbsolutePath() + File.separator + "patch");
        File tempFileForPatchDecryption = new File(patch.getAbsolutePath() + ".decrypted");
        File logFile = new File(tempDirForCreatePatch.getAbsolutePath() + File.separator + "action.log");
        File tempDirForApplyPatch = new File(tempDir.getAbsolutePath() + File.separator + "apply_patch");
        tempDirForCreatePatch.mkdirs();
        tempDirForPatch.mkdirs();
        tempDirForApplyPatch.mkdirs();

        // create patch of new from old (upgrade patch)
        PatchCreator.createPatch(oldFolder, newFolder, tempDirForCreatePatch, patch, -1, "1.0.0", "1.0.1", aesKey, tempFileForPatchEncryption);
        // copy 'old' folder to new directory
        TestCommon.copyFolder(oldFolder, tempDirForPatch);
        // apply the patch on 'old' folder
        Patcher patcher = new Patcher(new PatcherListener() {

            @Override
            public void patchProgress(int percentage, String message) {
            }

            @Override
            public void patchEnableCancel(boolean enable) {
            }
        }, logFile, tempDirForPatch, tempDirForApplyPatch);
        patcher.doPatch(patch, 1, aesKey, tempFileForPatchDecryption);
        // compare the new 'old' folder and the 'new' folder
        assertTrue(TestCommon.compareFolder(tempDirForPatch, newFolder));


        File fullPatch = new File(tempDir.getAbsolutePath() + File.separator + "full_patch.patch");
        File tempDirForCreateFullPatch = new File(tempDir.getAbsolutePath() + File.separator + "create_full_patch");
        File tempFileForFullPatchEncryption = new File(fullPatch.getAbsolutePath() + ".encrypted");
        File tempDirForFullPatch = new File(tempDir.getAbsolutePath() + File.separator + "full_patch");
        File tempFileForFullPatchDecryption = new File(fullPatch.getAbsolutePath() + ".decrypted");
        File logFileForFullPatch = new File(tempDirForCreateFullPatch.getAbsolutePath() + File.separator + "action_full_patch.log");
        File tempDirForApplyFullPatch = new File(tempDir.getAbsolutePath() + File.separator + "apply_full_patch");
        tempDirForCreateFullPatch.mkdirs();
        tempDirForFullPatch.mkdirs();
        tempDirForApplyFullPatch.mkdirs();

        // create patch of new from old (full patch)
        PatchCreator.createFullPatch(newFolder, fullPatch, -1, "1.0.0", null, "1.0.1", aesKey, tempFileForFullPatchEncryption);
        // copy 'old' folder to new directory
        assertTrue(CommonUtil.truncateFolder(tempDirForFullPatch));
        TestCommon.copyFolder(oldFolder, tempDirForFullPatch);
        // apply the patch on 'old' folder
        patcher = new Patcher(new PatcherListener() {

            @Override
            public void patchProgress(int percentage, String message) {
            }

            @Override
            public void patchEnableCancel(boolean enable) {
            }
        }, logFileForFullPatch, tempDirForFullPatch, tempDirForApplyFullPatch);
        patcher.doPatch(fullPatch, 1, aesKey, tempFileForFullPatchDecryption);
        // compare the new 'old' folder and the 'new_over_old' folder
        assertTrue(TestCommon.compareFolder(tempDirForFullPatch, newOverOldFolder));


        File repackedPatch = new File(tempDir.getAbsolutePath() + File.separator + "patch.repacked.patch");
        File tempFileForExtractPatchEncryption = new File(repackedPatch.getAbsolutePath() + ".encrypted");
        File tempFileForExtractPatchDecryption = new File(repackedPatch.getAbsolutePath() + ".decrypted");
        File tempDirForExtractPatch = new File(tempDir.getAbsolutePath() + File.separator + "extract");
        File repackedFullPatch = new File(tempDir.getAbsolutePath() + File.separator + "full_patch.repacked.patch");
        File tempFileForExtractFullPatchEncryption = new File(repackedFullPatch.getAbsolutePath() + ".encrypted");
        File tempFileForExtractFullPatchDecryption = new File(repackedFullPatch.getAbsolutePath() + ".decrypted");
        File tempDirForExtractFullPatch = new File(tempDir.getAbsolutePath() + File.separator + "extract_full");

        // extract the patch
        PatchExtractor.extract(patch, tempDirForExtractPatch, aesKey, tempFileForExtractPatchDecryption);
        // pack the patch
        PatchPacker.pack(tempDirForExtractPatch, repackedPatch, aesKey, tempFileForExtractPatchEncryption);
        // compare the newly packed patch with the original patch
        assertTrue(CommonUtil.compareFile(repackedPatch, patch));

        // extract the full patch
        PatchExtractor.extract(fullPatch, tempDirForExtractFullPatch, aesKey, tempFileForExtractFullPatchDecryption);
        // pack the full patch
        PatchPacker.pack(tempDirForExtractFullPatch, repackedFullPatch, aesKey, tempFileForExtractFullPatchEncryption);
        // compare the newly packed full patch with the original full patch
        assertTrue(CommonUtil.compareFile(repackedFullPatch, fullPatch));


        CommonUtil.truncateFolder(tempDir);
        tempDir.delete();
    }
}
