package org.apache.dubbo.validation.support;

import java.io.Serializable;
import java.util.Objects;

/**
 * rpc method validation result
 * <p>
 * convert {@link jakarta.validation.ConstraintViolation} and {@link javax.validation.ConstraintViolation} validation result,
 * Avoid serialization exceptions.
 * </p>
 */
public class ValidationResult implements Serializable {

    private static final long serialVersionUID = -527107355540718877L;
    private Object value;
    private Object propertyPath;
    private String message;
    private String messageTemplate;
    private Object[] executableParameters;
    private Object executableReturnValue;

    public ValidationResult(Object value, Object propertyPath, String message, String messageTemplate,
                            Object[] executableParameters, Object executableReturnValue) {
        this.value = value;
        this.propertyPath = propertyPath;
        this.message = message;
        this.messageTemplate = messageTemplate;
        this.executableParameters = executableParameters;
        this.executableReturnValue = executableReturnValue;
    }


    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getPropertyPath() {
        return propertyPath;
    }

    public void setPropertyPath(Object propertyPath) {
        this.propertyPath = propertyPath;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public Object[] getExecutableParameters() {
        return executableParameters;
    }

    public void setExecutableParameters(Object[] executableParameters) {
        this.executableParameters = executableParameters;
    }

    public Object getExecutableReturnValue() {
        return executableReturnValue;
    }

    public void setExecutableReturnValue(Object executableReturnValue) {
        this.executableReturnValue = executableReturnValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ValidationResult that = (ValidationResult) o;
        return propertyPath.equals(that.propertyPath) && message.equals(that.message) && messageTemplate.equals(that.messageTemplate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyPath, message, messageTemplate);
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "propertyPath=" + propertyPath +
                ", message='" + message + '\'' +
                ", messageTemplate='" + messageTemplate + '\'' +
                '}';
    }
}
