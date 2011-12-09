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
   * The detail operation id.
   */
  protected int operationId;
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
   * @param operationId 
   * @param backupFilePath the backup file path
   * @param newFilePath the copy-from file path
   * @param destinationFilePath the copy-to file path
   */
  public PatchRecord(int fileIndex, int operationId, String backupFilePath, String newFilePath, String destinationFilePath) {
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
    this.operationType = null;
    this.backupFilePath = backupFilePath;
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
  public PatchRecord(OperationType operationType, String destinationFilePath, String newFilePath, String backupFilePath) {
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
    this.operationId = -1;
    this.operationType = operationType;
    this.destinationFilePath = destinationFilePath;
    this.newFilePath = newFilePath;
    this.backupFilePath = backupFilePath;
  }

  /**
   * Get the file index of the record.
   * @return the file index, -1 means not specified
   */
  public int getFileIndex() {
    return fileIndex;
  }

  /**
   * Get operation type.
   * @return the operation type, null means not specified
   */
  public OperationType getOperationType() {
    return operationType;
  }

  /**
   * Get detail operation id.
   * @return the operation id, -1 means not specified
   */
  public int getOperationId() {
    return operationId;
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
    return "fileIndex: " + fileIndex + ", operationType: " + (getOperationType() != null ? getOperationType().getValue() : "")
            + ", dest: " + destinationFilePath + ", new: " + newFilePath + ", backup: " + backupFilePath;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 17 * hash + this.fileIndex;
    hash = 17 * hash + (this.operationType != null ? this.operationType.hashCode() : 0);
    hash = 17 * hash + this.operationId;
    hash = 17 * hash + (this.backupFilePath != null ? this.backupFilePath.hashCode() : 0);
    hash = 17 * hash + (this.newFilePath != null ? this.newFilePath.hashCode() : 0);
    hash = 17 * hash + (this.destinationFilePath != null ? this.destinationFilePath.hashCode() : 0);
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
            && _object.getOperationId() == _object.getOperationId()
            && (_object.getOperationType() == null && getOperationType() == null || (_object.getOperationType() != null && getOperationType() != null && _object.getOperationType().equals(getOperationType())))
            && _object.getBackupFilePath().equals(getBackupFilePath())
            && _object.getNewFilePath().equals(getNewFilePath())
            && _object.getDestinationFilePath().equals(getDestinationFilePath());
  }
}