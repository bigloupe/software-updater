package updater.patch;

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
   * The operation type.
   */
  protected OperationType operationType;
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
   * @param backupPath the backup file path
   * @param newFilePath the copy-from file path
   * @param destinationFilePath the copy-to file path
   */
  public PatchRecord(int fileIndex, String backupPath, String newFilePath, String destinationFilePath) {
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
    this.operationType = null;
    this.backupFilePath = backupPath;
    this.newFilePath = newFilePath;
    this.destinationFilePath = destinationFilePath;
  }

  /**
   * Constructor.
   * @param operationType the operation type
   * @param destinationFilePath the path to move the new file to
   * @param newFilePath the path where the new file locate
   * @param backupFilePath the path where the backup file locate
   */
  protected PatchRecord(OperationType operationType, String destinationFilePath, String newFilePath, String backupFilePath) {
    if (operationType == null) {
      throw new NullPointerException("argument 'operationType' cannot be null");
    }
    if (destinationFilePath == null) {
      throw new NullPointerException("argument 'destinationFilePath' cannot be null");
    }
    if (newFilePath == null) {
      throw new NullPointerException("argument 'newFilePath' cannot be null");
    }
    if (backupFilePath == null) {
      throw new NullPointerException("argument 'backupFilePath' cannot be null");
    }
    this.fileIndex = -1;
    this.operationType = operationType;
    this.destinationFilePath = destinationFilePath;
    this.newFilePath = newFilePath;
    this.backupFilePath = backupFilePath;
  }

  /**
   * Get operation type.
   * @return the operation type
   */
  public OperationType getOperationType() {
    return operationType;
  }

  /**
   * Get the file index of the record.
   * @return the file index
   */
  public int getFileIndex() {
    return fileIndex;
  }

  /**
   * Get the backup file path of the replacement record.
   * @return the file path
   */
  public String getBackupFilePath() {
    return newFilePath;
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
    return fileIndex + ": " + newFilePath;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 11 * hash + this.fileIndex;
    hash = 11 * hash + (this.operationType != null ? this.operationType.hashCode() : 0);
    hash = 11 * hash + (this.backupFilePath != null ? this.backupFilePath.hashCode() : 0);
    hash = 11 * hash + (this.newFilePath != null ? this.newFilePath.hashCode() : 0);
    hash = 11 * hash + (this.destinationFilePath != null ? this.destinationFilePath.hashCode() : 0);
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
            && _object.getOperationType().equals(getOperationType())
            && _object.getBackupFilePath().equals(getBackupFilePath())
            && _object.getNewFilePath().equals(getNewFilePath())
            && _object.getDestinationFilePath().equals(getDestinationFilePath());
  }
}