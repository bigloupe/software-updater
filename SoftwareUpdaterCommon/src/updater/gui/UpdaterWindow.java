package updater.gui;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

/**
 * The update GUI window for downloader and launcher.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class UpdaterWindow {

  /**
   * List of listeners.
   */
  protected final List<ActionListener> listeners;
  /**
   * Updater window/frame.
   */
  protected JFrame frame;
  /**
   * Components
   */
  protected JTextField messageField;
  protected JLabel progressLabel;
  protected JProgressBar progressBar;
  protected JButton cancelButton;

  /**
   * Constructor.
   * @param windowTitle the title of the frame
   * @param windowIcon the icon of the frame
   * @param title the title of the content/progress panel
   * @param icon the icon on the left-hand-side of the {@code title}
   */
  public UpdaterWindow(String windowTitle, Image windowIcon, String title, Image icon) {
    listeners = Collections.synchronizedList(new ArrayList<ActionListener>());

    // message
    messageField = new JTextField();
    messageField.setBorder(null);
    messageField.setEditable(false);
    messageField.setOpaque(false);
//        messageField.setFont(messageField.getFont().deriveFont(12F));

    // progress, 0% to 100%
    progressLabel = new JLabel();

    // progress bar, 0% to 100%
    progressBar = new JProgressBar();
    progressBar.setMinimum(0);
    progressBar.setMaximum(100);
    progressBar.setPreferredSize(new Dimension(20, 20));
    progressBar.setMinimumSize(new Dimension(20, 20));
    progressBar.setMaximumSize(new Dimension(65535, 20));

    // cancel button
    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        synchronized (listeners) {
          for (ActionListener listener : listeners) {
            listener.actionPerformed(new ActionEvent(UpdaterWindow.this, 1, "cancel"));
          }
        }
      }
    });


    // message box
    Box messageBox = Box.createHorizontalBox();
    messageBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
    messageBox.add(messageField);
    messageBox.add(Box.createHorizontalGlue());
    messageBox.add(Box.createRigidArea(new Dimension(5, 5)));
    messageBox.add(progressLabel);

    // progress bar box
    Box progressBox = Box.createHorizontalBox();
    progressBox.add(progressBar);

    // button panel box
    Box buttonBox = Box.createHorizontalBox();
    buttonBox.setBorder(BorderFactory.createEmptyBorder(9, 0, 0, 0));
    buttonBox.add(Box.createHorizontalGlue());
    buttonBox.add(cancelButton);


    // main content panel
    JTitledPanel panel = new JTitledPanel();
    panel.setTitle(title, icon != null ? new ImageIcon(icon) : null);
    panel.getContentPanel().add(messageBox);
    panel.getContentPanel().add(progressBox);
    panel.getContentPanel().add(buttonBox);
    panel.setFooterPanelVisibility(false);


    // frame
    frame = new JFrame();
    frame.setTitle(windowTitle);
    frame.setIconImage(windowIcon);
    frame.addWindowListener(new WindowAdapter() {

      @Override
      public void windowClosing(WindowEvent e) {
        synchronized (listeners) {
          for (ActionListener listener : listeners) {
            listener.actionPerformed(new ActionEvent(UpdaterWindow.this, 1, "cancel"));
          }
        }
      }
    });
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    frame.setResizable(false);
    frame.setContentPane(panel);
    frame.pack();
    frame.setLocationByPlatform(true);
    frame.setSize(new Dimension(450, frame.getPreferredSize().height));
  }

  /**
   * Add listener for the cancel button click event.
   * @param listener the listener
   */
  public void addListener(ActionListener listener) {
    if (listener == null) {
      return;
    }
    listeners.add(listener);
  }

  /**
   * Remove the listener that listen for the cancel button client event.
   * @param listener the listener to remove
   */
  public void removeListener(ActionListener listener) {
    if (listener == null) {
      return;
    }
    listeners.remove(listener);
  }

  /**
   * Get the GUI of the updater window.
   * @return the frame
   */
  public JFrame getGUI() {
    return frame;
  }

  /**
   * Set the message for the current progress. The message should be describing 
   * the current taking action.
   * @param message the message
   */
  public void setMessage(String message) {
    messageField.setText(message);
  }

  /**
   * Set the progress of current work, range from 0 to 100.
   * @param progress the progress, range from 0 to 100
   */
  public void setProgress(int progress) {
    if (progress < 0 || progress > 100) {
      return;
    }
    progressLabel.setText(progress + "%");
    progressBar.setValue(progress);
  }

  /**
   * Get whether the cancel action/button is enabled or not.
   * @return true if the cancel button is enabled, false if not
   */
  public boolean isCancelEnabled() {
    return cancelButton.isEnabled();
  }

  /**
   * Set enable the cancel or not. (update cancel button)
   * @param enable true to enable, false to disable
   */
  public void setCancelEnabled(boolean enable) {
    cancelButton.setEnabled(enable);
  }
}
