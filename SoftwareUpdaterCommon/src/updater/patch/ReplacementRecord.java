package updater.patch;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class ReplacementRecord extends PatchRecord {

  protected OperationType operationType;

  public ReplacementRecord(OperationType operationType, int operationId, String backupFilePath, String newFilePath, String destinationFilePath) {
    super(-1, operationId, false, backupFilePath, newFilePath, destinationFilePath);
    if (operationType == null) {
      throw new NullPointerException("argument 'operationType' cannot be null");
    }
  }

  /**
   * Get operation type.
   * @return the operation type, null means not specified
   */
  public OperationType getOperationType() {
    return operationType;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 41 * hash + (this.operationType != null ? this.operationType.hashCode() : 0);
    hash = 41 * hash + super.hashCode();
    return hash;
  }

  @Override
  public boolean equals(Object compareTo) {
    if (compareTo == null || !(compareTo instanceof ReplacementRecord)) {
      return false;
    }
    if (compareTo == this) {
      return true;
    }
    ReplacementRecord _object = (ReplacementRecord) compareTo;
    return super.equals(_object) && (_object.getOperationType() == null && getOperationType() == null || (_object.getOperationType() != null && getOperationType() != null && _object.getOperationType().equals(getOperationType())));
  }
}
