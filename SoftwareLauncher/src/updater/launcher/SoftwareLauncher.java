package updater.launcher;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.transform.TransformerException;
import updater.concurrent.ConcurrentLock;
import updater.concurrent.LockType;
import updater.concurrent.LockUtil;
import updater.gui.UpdaterWindow;
import updater.patch.PatchRecord;
import updater.patch.Patcher;
import updater.script.Client;
import updater.script.Client.Information;
import updater.script.InvalidFormatException;
import updater.script.Patch;
import updater.util.CommonUtil;
import updater.util.GetClientScriptResult;

/**
 * The software launcher main class.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareLauncher {

  private static final Logger LOG = Logger.getLogger(SoftwareLauncher.class.getName());

  protected SoftwareLauncher() {
  }

  /**
   * Start update (if any) and launch/start the software.
   * @param clientScriptPath the path of the client script file
   * @param args the arguments to pass-in to launch/start the software
   * @throws IOException possible failure: failed to copy self-updater to 
   * desire destination, failed to write to replacement file or failed to 
   * execute the command
   * @throws InvalidFormatException the format of the client script is invalid
   * @throws LaunchFailedException launch failed, possible jar not found, 
   * class not found or main method not found
   */
  public static void start(String clientScriptPath, String[] args) throws IOException, InvalidFormatException, LaunchFailedException {
    File clientScript = new File(clientScriptPath);
    Client client = Client.read(Util.readFile(clientScript));
    if (client != null) {
      start(clientScript, client, args);
    }
  }

  /**
   * Start update (if any) and launch/start the software.
   * @param clientScriptFile the client script file
   * @param client the client script
   * @param args the arguments to pass-in to launch/start the software
   * @throws IOException possible failure: failed to copy self-updater to 
   * desire destination, failed to write to replacement file or failed to 
   * execute the command
   * @throws LaunchFailedException launch failed, possible jar not found, 
   * class not found or main method not found
   */
  public static void start(final File clientScriptFile, final Client client, String[] args) throws IOException, LaunchFailedException {
    boolean launchSoftware = false;
    List<PatchRecord> replacementFailList = new ArrayList<PatchRecord>();
    if (!client.getPatches().isEmpty()) {
      String storagePath = client.getStoragePath();
      Information clientInfo = client.getInformation();

      Image softwareIcon = null;
      Image updaterIcon = null;
      //<editor-fold defaultstate="collapsed" desc="get icons">
      if (clientInfo != null) {
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
      }
      if (softwareIcon == null) {
        softwareIcon = Toolkit.getDefaultToolkit().getImage(SoftwareLauncher.class.getResource("/software_icon.png"));
      }
      if (updaterIcon == null) {
        updaterIcon = Toolkit.getDefaultToolkit().getImage(SoftwareLauncher.class.getResource("/updater_icon.png"));
      }
      //</editor-fold>

      final BatchPatcher batchPatcher = new BatchPatcher();

      String softwareName = clientInfo != null && clientInfo.getSoftwareName() != null ? clientInfo.getSoftwareName() : "Software Updater";
      String launcherName = clientInfo != null && clientInfo.getLauncherTitle() != null ? clientInfo.getLauncherTitle() : "Software Updater";

      // GUI
      final Thread currentThread = Thread.currentThread();
      final UpdaterWindow updaterGUI = new UpdaterWindow(softwareName, softwareIcon, launcherName, updaterIcon);
      updaterGUI.addListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          batchPatcher.pause(true);

          Object[] options = {"Yes", "No"};
          int result = JOptionPane.showOptionDialog(null, "Are you sure to cancel update?", "Canel Update", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
          if (result == 0) {
            updaterGUI.setCancelEnabled(false);
            currentThread.interrupt();
          }

          batchPatcher.pause(false);
        }
      });
      updaterGUI.setProgress(0);
      updaterGUI.setMessage("Preparing ...");
      JFrame updaterFrame = updaterGUI.getGUI();
      updaterFrame.setVisible(true);

      // update
      ConcurrentLock lock = null;
      try {
        lock = LockUtil.acquireLock(LockType.UPDATER, new File(storagePath), 1000, 50);

        replacementFailList = batchPatcher.doPatch(new BatchPatchListener() {

          @Override
          public void patchProgress(int percentage, String message) {
            updaterGUI.setProgress(percentage);
            updaterGUI.setMessage(message);
          }

          @Override
          public void patchEnableCancel(boolean enable) {
            updaterGUI.setCancelEnabled(enable);
          }

          @Override
          public void patchInvalid(Patch patch) throws IOException {
            List<Patch> patches = client.getPatches();
            patches.remove(patch);
            client.setPatches(patches);
            try {
              CommonUtil.saveClientScript(clientScriptFile, client);
            } catch (TransformerException ex) {
              throw new IOException(ex);
            }
          }

          @Override
          public void patchFinished(Patch patch) throws IOException {
            List<Patch> patches = client.getPatches();
            patches.remove(patch);
            client.setPatches(patches);
            client.setVersion(patch.getVersionTo());
            try {
              CommonUtil.saveClientScript(clientScriptFile, client);
            } catch (TransformerException ex) {
              throw new IOException(ex);
            }
          }
        }, new File("." + File.separator), new File(storagePath), client.getVersion(), client.getPatches());

        // check if there is any replacement failed and do the replacement with the self updater
        if (replacementFailList.isEmpty()) {
          launchSoftware = true;
        } else {
          handleReplacement(client, replacementFailList, args);
        }
      } catch (Exception ex) {
        LOG.log(Level.SEVERE, null, ex);

        JOptionPane.showMessageDialog(updaterFrame, "Error occurred when updating the software.");

        if (updaterGUI.isCancelEnabled()) {
          Object[] options = {"Recover", "Exit & Restart manually"};
          int result = JOptionPane.showOptionDialog(null, "Recover back to original version or exit & restart your computer manually?", "Update Failed", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
          if (result == 0) {
            try {
              doRevert(client.getPatches(), new File(storagePath));
            } catch (IOException ex1) {
              JOptionPane.showMessageDialog(updaterFrame, "Error occurred when doing the revertion.");
              LOG.log(Level.SEVERE, null, ex1);
              return;
            }
          } else {
            return;
          }
        }
      } finally {
        updaterFrame.setVisible(false);
        updaterFrame.dispose();
        lock.release();
      }
    } else {
      launchSoftware = true;
    }

    // launch/start the software
    if (launchSoftware) {
      String launchType = client.getLaunchType();
      String afterLaunchOperation = client.getLaunchAfterLaunch();
      String jarPath = client.getLaunchJarPath();
      String mainClass = client.getLaunchMainClass();
      List<String> launchCommands = client.getLaunchCommands();

      ConcurrentLock instanceLock = LockUtil.acquireLock(LockType.INSTANCE, new File(client.getStoragePath()), 1000, 50);

      try {
        if (launchType.equals("jar")) {
          startSoftware(jarPath, mainClass, args);
        } else {
          ProcessBuilder builder = new ProcessBuilder(launchCommands);
          Process process = null;
          try {
            process = builder.start();
          } catch (Exception ex) {
            throw new LaunchFailedException(ex);
          }
          if (afterLaunchOperation != null && afterLaunchOperation.equals("exit")) {
            instanceLock.release();
            System.exit(0);
          } else {
            try {
              process.waitFor();
            } catch (InterruptedException ex) {
              LOG.log(Level.INFO, null, ex);
              Thread.currentThread().interrupt();
            }
          }
        }
      } finally {
        instanceLock.release();
      }
    }
  }

  /**
   * Revert all the patching action of {@code patches}.
   * @param patches the patches to revert, must be in sequence
   * @param tempDir temporary folder to store temporary generated files while 
   * patching
   * @throws IOException error occurred when doing revert
   */
  public static void doRevert(List<Patch> patches, File tempDir) throws IOException {
    for (int i = patches.size() - 1; i >= 0; i--) {
      Patch patch = patches.get(i);
      Patcher patcher = new Patcher(new File(tempDir + File.separator + patch.getId() + File.separator + "action.log"));
      patcher.revert();
    }
  }

  public static void handleReplacement(Client clientScript, List<PatchRecord> replacementList, String[] launchArgs) throws IOException {
    if (replacementList == null || replacementList.isEmpty()) {
      return;
    }
    if (clientScript == null) {
      throw new NullPointerException("argument 'clientScript' cannot be null");
    }
    if (launchArgs == null) {
      throw new NullPointerException("argument 'launchArgs' cannot be null");
    }

    // copy the self updater to the storage path from inside the jar
    Util.writeFile(new File(clientScript.getStoragePath() + File.separator + "SoftwareSelfUpdater.jar"), Util.readResourceFile("/SoftwareSelfUpdater.jar"));

    // prepare the replacement file for the self updater
    File replacementFile = new File(clientScript.getStoragePath() + File.separator + "replacement.txt");
    writeReplacement(replacementFile, replacementList);

    // prepare the command to execute the self updater
    List<String> commands = new ArrayList<String>();

    String javaBinary = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    // the physical/file path of this launcher jar
    String launcherPath = null;
    try {
      launcherPath = SoftwareLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
    } catch (URISyntaxException ex) {
      JOptionPane.showMessageDialog(null, "Fatal error occurred: jar path detected of this launcher is invalid.");
      LOG.log(Level.SEVERE, null, ex);
      return;
    }

    commands.add(javaBinary);
    commands.add("-jar");
    commands.add(clientScript.getStoragePath() + File.separator + "SoftwareSelfUpdater.jar");
    commands.add(clientScript.getStoragePath());
    commands.add(replacementFile.getAbsolutePath());

    if (clientScript.getLaunchType().equals("jar")) {
      commands.add(javaBinary);
      commands.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
      commands.add("-jar");
      commands.add(launcherPath);
      commands.addAll(Arrays.asList(launchArgs));
    } else {
      for (String _command : clientScript.getLaunchCommands()) {
        commands.add(_command.replace("{java}", javaBinary));
      }
    }

    ProcessBuilder builder = new ProcessBuilder(commands);
    builder.start();
    System.exit(0);
  }

  /**
   * Write the replacement list into the file. (destination, then new file 
   * path, line by line)
   * @param file the file to write into
   * @param replacementList the replacement list
   * @throws IOException error occurred when writing to the file
   */
  protected static void writeReplacement(File file, List<PatchRecord> replacementList) throws IOException {
    PrintWriter writer = null;
    try {
      writer = new PrintWriter(new FileOutputStream(file));

      for (PatchRecord _replacement : replacementList) {
        writer.println(_replacement.getDestinationFilePath());
        writer.println(_replacement.getNewFilePath());
        writer.println(_replacement.getBackupFilePath());
      }
    } finally {
      Util.closeQuietly(writer);
    }
  }

  /**
   * Load the jar and execute the main method.
   * @param jarPath the path of the jar file
   * @param mainClass the class that contain the main method
   * @param args arguments to pass-in to the main method
   * @throws LaunchFailedException launch failed, possible jar not found, 
   * class not found or main method not found
   */
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
      LOG.log(Level.SEVERE, null, ex);
    }

    GetClientScriptResult result = null;
    try {
      result = Util.getClientScript(args.length > 0 ? args[0] : null);
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
      JOptionPane.showMessageDialog(null, "Fail to load config file.");
      return;
    } catch (InvalidFormatException ex) {
      LOG.log(Level.SEVERE, null, ex);
      JOptionPane.showMessageDialog(null, "Config file format invalid.");
      return;
    }

    try {
      SoftwareLauncher.start(new File(result.getClientScriptPath()), result.getClientScript(), args);
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
      JOptionPane.showMessageDialog(null, "Fail to read images stated in the config file: root->information->software->icon or root->information->launcher->icon.");
      return;
    } catch (LaunchFailedException ex) {
      LOG.log(Level.SEVERE, null, ex);
      JOptionPane.showMessageDialog(null, "Failed to launch the software.");
      return;
    }
  }
}
