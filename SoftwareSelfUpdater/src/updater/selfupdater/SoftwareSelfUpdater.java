package updater.selfupdater;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import javax.swing.JOptionPane;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareSelfUpdater {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("argument length should >= 2");
            return;
        }


        File replacementFile = new File(args[0]);
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
                    if (!newFile.renameTo(destinationFile)) {
                        JOptionPane.showMessageDialog(null, "Failed to move file from " + newFilePath + " to " + destinationFilePath);
                        return;
                    }
                } else {
                    break;
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error occurred when reading replacement file.");
            return;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
            }
        }
        replacementFile.delete();


        String[] launchCommands = new String[args.length - 1];
        System.arraycopy(args, 1, launchCommands, 0, args.length - 1);
        try {
            new ProcessBuilder(Arrays.asList(launchCommands)).start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Failed to launch the software.");
        }
        System.exit(0);
    }
}
