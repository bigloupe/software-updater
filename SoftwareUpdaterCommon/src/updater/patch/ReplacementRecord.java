// Copyright (c) 2012 Chan Wai Shing
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package updater.patch;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class ReplacementRecord extends PatchRecord {

  protected OperationType operationType;

  public ReplacementRecord(OperationType operationType, int operationId, String destinationFilePath, String newFilePath, String backupFilePath) {
    super(-1, operationId, false, backupFilePath, newFilePath, destinationFilePath);
    if (operationType == null) {
      throw new NullPointerException("argument 'operationType' cannot be null");
    }
    this.operationType = operationType;
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
