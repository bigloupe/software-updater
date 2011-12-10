package updater.util;

import updater.script.Client;

/**
 * Return result for {@link CommonUtil#getClientScript(java.lang.String)}.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class GetClientScriptResult {

  /**
   * The client script.
   */
  protected Client clientScript;
  /**
   * The file path of the {@link #clientScript}.
   */
  protected String clientScriptPath;

  /**
   * Constructor.
   * @param clientScript the client script.
   * @param clientScriptPath the file path of the {@code clientScript}.
   */
  protected GetClientScriptResult(Client clientScript, String clientScriptPath) {
    this.clientScript = clientScript;
    this.clientScriptPath = clientScriptPath;
  }

  /**
   * Get the client script.
   * @return the client script
   */
  public Client getClientScript() {
    return clientScript;
  }

  /**
   * Get the path of the client script, {@link #clientScript}.
   * @return the file path
   */
  public String getClientScriptPath() {
    return clientScriptPath;
  }
}