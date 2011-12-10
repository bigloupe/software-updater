package updater.patch;

import java.util.logging.Logger;

/**
 * The record used to represent the essential data in a row of replacement log.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchRecord {

  /**
   * The file index of the record.
   */
  protected int fileIndex;
  /**
   * The detail operation id.
   */
  protected int operationId;
  /**
   * Indicate whether the destination file exist when patching or not.
   */
  protected boolean destinationFileExist;
  /**
   * The backup file path of the replacement record.
   */
  protected String backupFilePath;
  /**
   * The copy-from file path of the replacement record.
   */
  protected String newFilePath;
  /**
   * The copy-to file path of the replacement record.
   */
  protected String destinationFilePath;

  /**
   * Constructor.
   * @param fileIndex the file index of the record
   * @param operationId the operation id
   * @param destinationFileExist true means destination file exist when 
   * patching, false if not
   * @param backupFilePath the backup file path
   * @param newFilePath the copy-from file path
   * @param destinationFilePath the copy-to file path
   */
  public PatchRecord(int fileIndex, int operationId,
          boolean destinationFileExist,
          String backupFilePath, String newFilePath, String destinationFilePath) {
    if (newFilePath == null) {
      throw new NullPointerException("argument 'newFilePath' cannot be null");
    }
    if (backupFilePath == null) {
      throw new NullPointerException("argument 'backupFilePath' cannot be null");
    }
    if (destinationFilePath == null) {
      throw new NullPointerException("argument 'destinationFilePath' cannot be null");
    }
    this.fileIndex = fileIndex;
    this.operationId = operationId;
    this.destinationFileExist = destinationFileExist;
    this.backupFilePath = backupFilePath;
    this.newFilePath = newFilePath;
    this.destinationFilePath = destinationFilePath;
  }

  /**
   * Get the file index of the record.
   * @return the file index, -1 means not specified
   */
  public int getFileIndex() {
    return fileIndex;
  }

  /**
   * Get detail operation id.
   * @return the operation id, -1 means not specified
   */
  public int getOperationId() {
    return operationId;
  }

  /**
   * Check if the destination file exist when patching or not.
   * @return true means exist, false if not
   */
  public boolean isDestinationFileExist() {
    return destinationFileExist;
  }

  /**
   * Set the operation id.
   * @param operationId the operation id
   */
  public void setOperationId(int operationId) {
    this.operationId = operationId;
  }

  /**
   * Get the backup file path of the replacement record.
   * @return the file path
   */
  public String getBackupFilePath() {
    return backupFilePath;
  }

  /**
   * Get the copy-from file path of the replacement record.
   * @return the file path
   */
  public String getNewFilePath() {
    return newFilePath;
  }

  /**
   * Get the copy-to file path of the replacement record.
   * @return the file path
   */
  public String getDestinationFilePath() {
    return destinationFilePath;
  }

  @Override
  public String toString() {
    return "fileIndex: " + fileIndex + ", operationId: " + operationId
            + ", destinationFileExist: " + destinationFileExist
            + ", dest: " + destinationFilePath + ", new: " + newFilePath + ", backup: " + backupFilePath;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 67 * hash + this.fileIndex;
    hash = 67 * hash + this.operationId;
    hash = 67 * hash + (this.destinationFileExist ? 1 : 0);
    hash = 67 * hash + (this.backupFilePath != null ? this.backupFilePath.hashCode() : 0);
    hash = 67 * hash + (this.newFilePath != null ? this.newFilePath.hashCode() : 0);
    hash = 67 * hash + (this.destinationFilePath != null ? this.destinationFilePath.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object compareTo) {
    if (compareTo == null || !(compareTo instanceof PatchRecord)) {
      return false;
    }
    if (compareTo == this) {
      return true;
    }
    PatchRecord _object = (PatchRecord) compareTo;

    return _object.getFileIndex() == fileIndex
            && _object.getOperationId() == getOperationId()
            && _object.isDestinationFileExist() == isDestinationFileExist()
            && _object.getBackupFilePath().equals(getBackupFilePath())
            && _object.getNewFilePath().equals(getNewFilePath())
            && _object.getDestinationFilePath().equals(getDestinationFilePath());
  }
}