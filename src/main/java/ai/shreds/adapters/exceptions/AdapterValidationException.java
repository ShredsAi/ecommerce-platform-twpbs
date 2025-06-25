package ai.shreds.adapters.exceptions;

/**
 * Exception thrown when adapter layer validation fails
 */
public class AdapterValidationException extends RuntimeException {

    private final String fieldName;
    private final Object invalidValue;

    public AdapterValidationException(String message) {
        super(message);
        this.fieldName = null;
        this.invalidValue = null;
    }

    public AdapterValidationException(String message, String fieldName, Object invalidValue) {
        super(message);
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
    }

    public AdapterValidationException(String message, Throwable cause) {
        super(message, cause);
        this.fieldName = null;
        this.invalidValue = null;
    }

    public AdapterValidationException(String message, String fieldName, Object invalidValue, Throwable cause) {
        super(message, cause);
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getInvalidValue() {
        return invalidValue;
    }
}
