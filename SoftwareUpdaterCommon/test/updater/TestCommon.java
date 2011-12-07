package updater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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

  /**
   * Files in {@code folder2} must exist in {@code folder1}. {@code folder1}
   * can contain more files than {@code folder2} but not less than.
   */
  public static boolean compareFolderContainAtLeast(File folder1, File folder2) {
    Map<String, File> folder1Files = CommonUtil.getAllFiles(folder1, folder1.getAbsolutePath());
    Map<String, File> folder2Files = CommonUtil.getAllFiles(folder2, folder2.getAbsolutePath());

    if (folder1Files.size() < folder2Files.size()) {
      return false;
    }

    for (String _path : folder2Files.keySet()) {
      File _folder2File = folder2Files.get(_path);
      File _folder1File = folder1Files.remove(_path);
      if (_folder1File == null || _folder2File.isFile() != _folder1File.isFile()) {
        return false;
      }
      if (_folder2File.isFile()) {
        try {
          if (!CommonUtil.compareFile(_folder2File, _folder1File)) {
            return false;
          }
        } catch (IOException ex) {
          return false;
        }
      }
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

    if (!toFolder.isDirectory()) {
      if (!toFolder.mkdirs()) {
        throw new IOException("Cannot create folder: " + toFolder.getAbsolutePath());
      }
    }

    File[] files = fromFolder.listFiles();
    if (files == null) {
      throw new IOException(String.format("Failed to list the files in folder: %1$s", fromFolder.getAbsolutePath()));
    }
    String fromAbsPath = fromFolder.getAbsolutePath();
    String toAbsPath = toFolder.getAbsolutePath();
    for (File file : files) {
      File copyTo = new File(toAbsPath + File.separator + file.getAbsolutePath().replace(fromAbsPath, ""));
      if (file.isDirectory()) {
        copyFolder(file, copyTo);
      } else {
        CommonUtil.copyFile(file, copyTo);
      }
    }
  }

  public static void unzip(File zipFile, File unzipTo) throws IOException {
    if (zipFile == null) {
      throw new NullPointerException("argument 'zipFile' cannot be null");
    }
    if (unzipTo == null) {
      throw new NullPointerException("argument 'unzipTo' cannot be null");
    }
    if (!unzipTo.isDirectory() && unzipTo.exists()) {
      throw new IllegalArgumentException("folder 'unzipTo' exist and not a folder");
    }

    byte[] b = new byte[32768];

    String unzipToPath = unzipTo.getAbsolutePath();

    ZipFile _zipFile = new ZipFile(zipFile, ZipFile.OPEN_READ);
    Enumeration<? extends ZipEntry> enumeration = _zipFile.entries();
    while (enumeration.hasMoreElements()) {
      ZipEntry entry = enumeration.nextElement();

      if (entry.isDirectory()) {
        File entryDirectory = new File(unzipToPath + File.separator + entry.getName());
        if (!entryDirectory.isDirectory() && !entryDirectory.mkdirs()) {
          throw new IOException(String.format("failed to create folder. Entry path: %1$s, file path: %2$s", entry.getName(), entryDirectory.getAbsolutePath()));
        }
      } else {
        File entryFile = new File(unzipToPath + File.separator + entry.getName());

        File parentDirectory = entryFile.getParentFile();
        if (!parentDirectory.isDirectory() && !parentDirectory.mkdirs()) {
          throw new IOException(String.format("failed to create folder to hold file. Entry path: %1$s, folder path: %2$s", entry.getName(), parentDirectory.getAbsolutePath()));
        }

        InputStream in = null;
        OutputStream fout = null;
        try {
          in = _zipFile.getInputStream(entry);
          fout = new FileOutputStream(entryFile);

          long cumulateRead = 0, entrySize = entry.getSize();
          int byteRead;
          while ((byteRead = in.read(b)) != -1) {
            fout.write(b, 0, byteRead);

            cumulateRead += byteRead;
            if (cumulateRead >= entrySize) {
              break;
            }
          }

          if (cumulateRead != entrySize) {
            throw new IOException(String.format("The total number of bytes read does not match the file size. Actual file size: %1$d, bytes read: %2$d, entry path: %3$s, file path: %4$s",
                    entrySize, cumulateRead, entry.getName(), entryFile.getAbsolutePath()));
          }
        } finally {
          CommonUtil.closeQuietly(in);
          CommonUtil.closeQuietly(fout);
        }
      }
    }
  }
}
