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
import java.util.Properties;
import javax.swing.JOptionPane;

/**
 * This is a self updater for software launcher.
 * When the software launcher need to update itself but failed due to file locking on itself, this is used.
 * The launcher will launch this and exit (to release the file lock on the itself), then this self updater will replace the launcher with the updated one.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareSelfUpdater {

    /**
     * The maximum execution time allowed (in ms).
     * When the updater first failed, it will not give up immediately, because the program may not exit so quickly after launching this.
     * In this case, the program will keep trying until this maximum execution time is reached.
     * This is configurable by placing '/config.xml' inside the jar, for more information, see the code below.
     */
    protected static long maxExecutionTime = 15000;

    protected SoftwareSelfUpdater() {
    }

    /**
     * The format of the replacement file:
     * One row for destination file path, one row for new file path.
     * Example:
     * <p>
     * C:\software\dest.txt<br />
     * C:\tmp\dest.tmp.txt<br />
     * C:\software\dest.jar<br />
     * C:\tmp\dest.tmp.jar<br />
     * (a new line character here)
     * </p>
     * @param args 0: lock file path, 1: replacement file path, start from 2: command and arguments to launch the software.
     */
    public static void main(String[] args) {
        // check if the length of args meet the minimum requirement
        if (args.length < 3) {
            StringBuilder sb = new StringBuilder();
            sb.append("argument length should >= 3, current length: ");
            sb.append(args.length);
            sb.append(", args: ");
            for (String arg : args) {
                sb.append(arg);
                sb.append(' ');
            }
            JOptionPane.showMessageDialog(null, sb.toString()); // this error message normally would not be shown to user
            return;
        }

        // the time for determine whether reach maxExecutionTime or not
        long startTime = 0;
        startTime = System.currentTimeMillis();


        // read the maximum execution time from /config.xml inside the jar if there is any
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(readResourceFile("/config.xml"));
            Properties config = new Properties();
            config.loadFromXML(in);
            maxExecutionTime = Integer.parseInt(config.getProperty("max_execution_time"));
        } catch (Exception ex) {
            // ignore
        }


        // acquire lock on the lock file to make sure there is no other updater/downloader/self-updater running
        FileOutputStream lockFileOut = null;
        FileLock lock = null;
        while (true) {
            // check if maxExecutionTime reached
            if (System.currentTimeMillis() - startTime > maxExecutionTime) {
                JOptionPane.showMessageDialog(null, "There is another updater running.");
                return;
            }

            try {
                lockFileOut = new FileOutputStream(args[0], true);
                lock = lockFileOut.getChannel().tryLock();
                if (lock != null) {
                    // acquire lock succeed
                    break;
                } else {
                    // acquire lock failed, retry
                    throw new Exception();
                }
            } catch (Exception ex) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex1) {
                    // should not catch any
                }
                releaseQuietly(lock);
                closeQuietly(lockFileOut);
                continue;
            }
        }


        // read the replacement file and delete & move file
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
                            // retry the move
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                            }
                        }
                        // since we didn't check if the deletion on destinationFile succeed or not, so we delete it here again (in case the previous deletion was failed due to the program not terminated yet)
                        destinationFile.delete();
                    }
                } else {
                    // if not two lines was read, finish the process
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


        // launch the software
        String[] launchCommands = new String[args.length - 2];
        System.arraycopy(args, 2, launchCommands, 0, args.length - 2);
        try {
            new ProcessBuilder(Arrays.asList(launchCommands)).start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Failed to launch the software, you can try to launch it again after a while.");
            return;
        }
        System.exit(0);
    }

    /**
     * Release the file lock quietly without throwing any exception
     * @param fileLock the file lock to release
     */
    public static void releaseQuietly(FileLock fileLock) {
        if (fileLock != null) {
            try {
                fileLock.release();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Close the closeable quietly without throwing any exception
     * @param closeable the object with closable
     */
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
