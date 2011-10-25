package updater.launcher;

import java.awt.Image;
import java.awt.Toolkit;
import java.util.logging.Level;
import java.util.logging.Logger;
import updater.launcher.util.Util;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import updater.launcher.patch.BatchPatcher;
import updater.launcher.patch.BatchPatcher.UpdateResult;
import updater.script.Client;
import updater.script.Client.Information;
import updater.script.InvalidFormatException;
import updater.util.CommonUtil.GetClientScriptResult;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareLauncher {

    protected SoftwareLauncher() {
    }

    public static void start(String clientScriptPath, String[] args) throws IOException, InvalidFormatException, LaunchFailedException {
        File clientScript = new File(clientScriptPath);
        Client client = Client.read(Util.readFile(clientScript));
        if (client != null) {
            start(clientScript, client, args);
        }
    }

    public static void start(File clientScriptFile, Client client, String[] args) throws IOException, InvalidFormatException, LaunchFailedException {
        String jarPath = client.getLaunchJarPath();
        String mainClass = client.getLaunchMainClass();
        String storagePath = client.getStoragePath();

        Information clientInfo = client.getInformation();

        Image softwareIcon = null;
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
        Image updaterIcon = null;
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

        UpdateResult updateResult = BatchPatcher.update(clientScriptFile, client, new File(storagePath), clientInfo.getSoftwareName(), softwareIcon, clientInfo.getLauncherTitle(), updaterIcon);
        if (updateResult.isUpdateSucceed() || updateResult.isLaunchSoftware()) {
            startSoftware(jarPath, mainClass, args);
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
            throw new LaunchFailedException();
        }
    }

    public static void main(String[] args) {
        try {
            Util.setLookAndFeel();
        } catch (Exception ex) {
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            GetClientScriptResult result = Util.getClientScript(args.length > 0 ? args[0] : null);
            if (result.getClientScript() != null) {
                SoftwareLauncher.start(new File(result.getClientScriptPath()), result.getClientScript(), args);
            } else {
                JOptionPane.showMessageDialog(null, "Config file not found, is empty or is invalid.");
            }
        } catch (IOException ex) {
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Fail to read images stated in the config file: root->information->software-icon or root->information->updater-icon.");
        } catch (InvalidFormatException ex) {
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Config file format invalid.");
        } catch (LaunchFailedException ex) {
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Failed to launch the software.");
        }
    }
}
