package updater;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import updater.util.CommonUtil;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class TestCommon {

    public static final String modulusString = "0080ac742891f8ba0d59dcc96b464e2245e53a9b29f8219aa0b683ad10007247ced6d74b7bef2a6b0555ec22735827b2b9dfe94664d492a723ad78d6d97d1c9b19ade1225edc060eaced684436ce221659c7e8320bc2bf5ddcdbe6751b0f476066437ccc50ea0e5afafb6a59581df509145d34aa4d0541f500f09868686f5681a509bf58feda73b35326f816b60205550783d628e5e61b24e37198349e416f09ef7579f6f25b5725d54df44017e256b1c7060f0c5ba5f3dd162e26fc5fbfcf4294ee261124737b1cdc3024dc2be62c8ebd89c8766bfaf3606a9e7aefa4fd41758498441fe69a967005c66df3ac0551d7b04910c6a9fa272aa6d081defbc2db174f";
    public static final String publicExponentString = "010001";
    public static final String privateExponentString = "45fa8429d4494b161bbb21a7bfd29a7d1ccfa4b74c852a0d2175b7572e86f85a9b28f79a6d55ca625a7a53ba1b456bc3feec65264d1d7cdcc069299f9a95461ccf1dd38d7767abef8c25da835bd3da07f5da67ed517ab5d779987a33bf397849e58627b011bac0ec227392278413515ecbd9ea8c7cc1843780a1c296998698769825cd7ac298f5a468af873e2e30eb94cf867086742d0b8d1fd9ab7efc7ce3f07a855fe280e8714c963c8436a20fbaf81f874a6714da4699a75cb5c7e2fa0546038f8a8134661a25ce30ff37d73bd94dee33e7bdc6425729e2fd71bdb938a2f5cd7caf56eca8f7ccb8ea320b20610ffeae7f5c8380da62dca4d7964ded34b731";
    //
    public static final String pathToTestPackage = "test/";
    public static final String urlRoot = "http://localhost/SoftwareUpdaterTest/";
    //
    protected static PrintStream errorStream;

    protected TestCommon() {
    }

    public synchronized static void suppressErrorOutput() {
        if (errorStream == null) {
            errorStream = System.err;
            System.setErr(new PrintStream(new OutputStream() {

                @Override
                public void write(int b) throws IOException {
                }
            }));
        }
    }

    public synchronized static void restoreErrorOutput() {
        if (errorStream != null) {
            System.setErr(errorStream);
            errorStream = null;
        }
    }

    public static boolean compareFolder(File folder1, File folder2) {
        Map<String, File> folder1Files = CommonUtil.getAllFiles(folder1, folder1.getAbsolutePath());
        Map<String, File> folder2Files = CommonUtil.getAllFiles(folder2, folder2.getAbsolutePath());

        if (folder1Files.size() != folder2Files.size()) {
            return false;
        }

        Iterator<String> iterator = folder1Files.keySet().iterator();
        while (iterator.hasNext()) {
            String _path = iterator.next();
            File _folder1File = folder1Files.get(_path);
            File _folder2File = folder2Files.remove(_path);
            if (_folder2File == null || _folder1File.isFile() != _folder2File.isFile()) {
                return false;
            }
            if (_folder1File.isFile()) {
                try {
                    if (!CommonUtil.compareFile(_folder1File, _folder2File)) {
                        return false;
                    }
                } catch (IOException ex) {
                    return false;
                }
            }
            iterator.remove();
        }

        if (!folder1Files.isEmpty() || !folder2Files.isEmpty()) {
            return false;
        }

        return true;
    }

    public static void copyFolder(File fromFolder, File toFolder) throws IOException {
        if (fromFolder == null || toFolder == null) {
            return;
        }
        if (!fromFolder.isDirectory()) {
            throw new IllegalArgumentException("Argument 'fromFolder' is not a directory");
        }
        if (toFolder.exists() && !toFolder.isDirectory()) {
            throw new IllegalArgumentException("Argument 'toFolder' exist but not a directory");
        }

        toFolder.mkdirs();

        File[] files = fromFolder.listFiles();
        if (files == null) {
            throw new IOException("Error occurred when listing the files in folder: " + fromFolder.getAbsolutePath());
        }
        for (File file : files) {
            if (file.isDirectory()) {
                copyFolder(file, new File(toFolder.getAbsolutePath() + File.separator + file.getName()));
            } else {
                CommonUtil.copyFile(file, new File(toFolder.getAbsolutePath() + File.separator + file.getName()));
            }
        }
    }
}
