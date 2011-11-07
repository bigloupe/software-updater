package updater.launcher;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import updater.launcher.BatchPatcher.UpdateResult;
import updater.patch.Patcher.Replacement;
import updater.script.Client;
import updater.script.Client.Information;
import updater.script.InvalidFormatException;
import updater.util.CommonUtil.GetClientScriptResult;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareLauncher {

    static {
        // set debug mode
        System.setProperty("SyntaxHighlighterDebugMode", "false");
    }

    protected SoftwareLauncher() {
    }

    public static void start(String clientScriptPath, String[] args) throws IOException, InvalidFormatException, LaunchFailedException {
        File clientScript = new File(clientScriptPath);
        Client client = Client.read(Util.readFile(clientScript));
        if (client != null) {
            start(clientScript, client, args);
        }
    }

    public static void start(File clientScriptFile, Client client, String[] args) throws IOException, LaunchFailedException {
        String launchType = client.getLaunchType();
        String afterLaunchOperation = client.getLaunchAfterLaunch();
        String jarPath = client.getLaunchJarPath();
        String mainClass = client.getLaunchMainClass();
        List<String> launchCommands = client.getLaunchCommands();

        String storagePath = client.getStoragePath();
        Information clientInfo = client.getInformation();

        Image softwareIcon = null;
        Image updaterIcon = null;
        //<editor-fold defaultstate="collapsed" desc="get icons">
        if (clientInfo.getSoftwareIconLocation() != null) {
            if (clientInfo.getSoftwareIconLocation().equals("jar")) {
                URL resourceURL = SoftwareLauncher.class.getResource(clientInfo.getSoftwareIconPath());
                if (resourceURL != null) {
                    softwareIcon = Toolkit.getDefaultToolkit().getImage(resourceURL);
                } else {
                    throw new IOException("Resource not found: " + clientInfo.getSoftwareIconPath());
                }
            } else {
                softwareIcon = ImageIO.read(new File(clientInfo.getSoftwareIconPath()));
            }
        }
        if (clientInfo.getLauncherIconLocation() != null) {
            if (clientInfo.getLauncherIconLocation().equals("jar")) {
                URL resourceURL = SoftwareLauncher.class.getResource(clientInfo.getLauncherIconPath());
                if (resourceURL != null) {
                    updaterIcon = Toolkit.getDefaultToolkit().getImage(resourceURL);
                } else {
                    throw new IOException("Resource not found: " + clientInfo.getLauncherIconPath());
                }
            } else {
                updaterIcon = ImageIO.read(new File(clientInfo.getLauncherIconPath()));
            }
        }
        //</editor-fold>

        UpdateResult updateResult = BatchPatcher.update(clientScriptFile, client, new File(storagePath), clientInfo.getSoftwareName(), softwareIcon, clientInfo.getLauncherTitle(), updaterIcon);

        List<Replacement> replacementList = updateResult.getReplacementList();
        if (replacementList != null && !replacementList.isEmpty()) {
            Util.writeFile(new File(client.getStoragePath() + File.separator + "SoftwareSelfUpdater.jar"), Util.readResourceFile("/SoftwareSelfUpdater.jar"));

            File replacementFile = new File(storagePath + File.separator + "replacement.txt");
            writeReplacement(replacementFile, replacementList);

            List<String> commands = new ArrayList<String>();

            String javaBinary = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String launcherPath = null;
            try {
                launcherPath = SoftwareLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
            } catch (URISyntaxException ex) {
                JOptionPane.showMessageDialog(null, "Fatal error occurred: jar path detected is invalid.");
                Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }

            commands.add(javaBinary);
            commands.add("-jar");
            commands.add(replacementFile.getAbsolutePath());

            if (launchType.equals("jar")) {
                commands.add(launcherPath);
                commands.add(replacementFile.getAbsolutePath());
                commands.add(javaBinary);
                commands.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
                commands.add("-jar");
                commands.add(launcherPath);
                commands.addAll(Arrays.asList(args));
            } else {
                for (String _command : launchCommands) {
                    commands.add(_command.replace("{java}", javaBinary));
                }
            }

            ProcessBuilder builder = new ProcessBuilder(commands);
            builder.start();
            System.exit(0);
        }

        if (updateResult.isUpdateSucceed() || updateResult.isLaunchSoftware()) {
            if (launchType.equals("jar")) {
                startSoftware(jarPath, mainClass, args);
            } else {
                ProcessBuilder builder = new ProcessBuilder(launchCommands);
                builder.start();
                if (afterLaunchOperation.equals("exit")) {
                    System.exit(0);
                }
            }
        }
    }

    protected static void writeReplacement(File file, List<Replacement> replacementList) throws IOException {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            PrintWriter writer = new PrintWriter(fout);

            for (Replacement _replacement : replacementList) {
                writer.println(_replacement.getDestination());
                writer.println(_replacement.getNewFilePath());
            }
        } finally {
            Util.closeQuietly(fout);
        }
    }

    protected static void startSoftware(String jarPath, String mainClass, String[] args) throws LaunchFailedException {
        try {
            ClassLoader loader = URLClassLoader.newInstance(new URL[]{new File(jarPath).toURI().toURL()}, SoftwareLauncher.class.getClassLoader());
            Class<?> clazz = Class.forName(mainClass, true, loader);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.getName().equals("main")) {
                    method.invoke(null, (Object) (args));
                }
            }
        } catch (Exception ex) {
            throw new LaunchFailedException(ex);
        }
    }

    public static void main(String[] args) {
        try {
            Util.setLookAndFeel();
        } catch (Exception ex) {
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }

        GetClientScriptResult result = null;
        try {
            result = Util.getClientScript(args.length > 0 ? args[0] : null);
        } catch (IOException ex) {
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Fail to load config file.");
            return;
        } catch (InvalidFormatException ex) {
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Config file format invalid.");
            return;
        }

        try {
            if (result.getClientScript() != null) {
                SoftwareLauncher.start(new File(result.getClientScriptPath()), result.getClientScript(), args);
            } else {
                JOptionPane.showMessageDialog(null, "Config file not found, is empty or is invalid.");
            }
        } catch (IOException ex) {
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Fail to read images stated in the config file: root->information->software-icon or root->information->updater-icon.");
            return;
        } catch (LaunchFailedException ex) {
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Failed to launch the software.");
            return;
        }
    }
}
