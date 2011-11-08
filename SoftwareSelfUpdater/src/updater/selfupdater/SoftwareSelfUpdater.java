package updater.selfupdater;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import javax.swing.JOptionPane;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareSelfUpdater {

    protected static long maxExecutionTime = 15000;

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("argument length should >= 3");
            return;
        }

        long startTime = 0;
        startTime = System.currentTimeMillis();


        try {
            ByteArrayInputStream in = new ByteArrayInputStream(readResourceFile("/config.xml"));
            Properties config = new Properties();
            try {
                config.loadFromXML(in);
            } catch (InvalidPropertiesFormatException ex) {
                config = new Properties();
                // ignore
            }
            in.close();

            maxExecutionTime = Integer.parseInt(config.getProperty("max_execution_time"));
        } catch (Exception ex) {
        }


        FileOutputStream lockFileOut = null;
        FileLock lock = null;
        while (true) {
            if (System.currentTimeMillis() - startTime > maxExecutionTime) {
                JOptionPane.showMessageDialog(null, "There is another updater running.");
                return;
            }

            try {
                lockFileOut = new FileOutputStream(args[0]);
                lock = lockFileOut.getChannel().tryLock();
                if (lock != null) {
                    break;
                } else {
                    // maybe the program did not exit so quickly after launching this
                    Thread.sleep(1000);
                    throw new Exception();
                }
            } catch (Exception ex) {
                releaseQuietly(lock);
                closeQuietly(lockFileOut);
                continue;
            }
        }


        File replacementFile = new File(args[1]);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(replacementFile)));

            while (true) {
                String destinationFilePath = reader.readLine();
                String newFilePath = reader.readLine();
                if (destinationFilePath != null && newFilePath != null) {
                    File destinationFile = new File(destinationFilePath);
                    File newFile = new File(newFilePath);

                    destinationFile.delete();
                    while (!newFile.renameTo(destinationFile)) {
                        if (System.currentTimeMillis() - startTime > maxExecutionTime) {
                            JOptionPane.showMessageDialog(null, "Failed to move file from " + newFilePath + " to " + destinationFilePath);
                            return;
                        } else {
                            // maybe the program did not exit so quickly after launching this
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                            }
                        }
                    }
                } else {
                    break;
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error occurred when reading replacement file.");
            return;
        } finally {
            closeQuietly(reader);
            releaseQuietly(lock);
            closeQuietly(lockFileOut);
        }
        replacementFile.delete();


        String[] launchCommands = new String[args.length - 2];
        System.arraycopy(args, 2, launchCommands, 0, args.length - 2);
        try {
            new ProcessBuilder(Arrays.asList(launchCommands)).start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Failed to launch the software.");
        }
        System.exit(0);
    }

    public static void releaseQuietly(FileLock fileLock) {
        if (fileLock != null) {
            try {
                fileLock.release();
            } catch (IOException ex) {
            }
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Read the resource file from the jar.
     * @param path the resource path
     * @return the content of the resource file in byte array
     * @throws IOException error occurred when reading the content from the file
     */
    public static byte[] readResourceFile(String path) throws IOException {
        if (path == null) {
            throw new NullPointerException("argument 'path' cannot be null");
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        InputStream in = null;
        try {
            in = SoftwareSelfUpdater.class.getResourceAsStream(path);
            if (in == null) {
                throw new IOException("Resources not found: " + path);
            }

            int byteRead = 0;
            byte[] b = new byte[8096];

            while ((byteRead = in.read(b)) != -1) {
                bout.write(b, 0, byteRead);
            }
        } finally {
            closeQuietly(in);
        }

        return bout.toByteArray();
    }
}
