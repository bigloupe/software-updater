package updater.patch;

/**
 * Enum for specifying the operation type used by the patch.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public enum OperationType {

    NEW("new"), FORCE("force"), REPLACE("replace"), PATCH("patch"), REMOVE("remove");
    /**
     * The string value representation of the operation type.
     */
    protected final String value;

    OperationType(String value) {
        this.value = value;
    }

    /**
     * Get the unique string representation for this operation type..
     * @return the string value
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the {@link updater.patch.OperationType} by the types' string value.
     * @param value the string value
     * @return the {@link updater.patch.OperationType} or null if not correspondent found
     */
    public static OperationType get(String value) {
        OperationType[] operationTypes = OperationType.values();
        for (OperationType operationType : operationTypes) {
            if (operationType.getValue().equals(value)) {
                return operationType;
            }
        }
        return null;
    }
}
