package org.apache.dubbo.config.exception;

/**
 * Config validation exception
 */
public class ConfigValidationException extends RuntimeException {

    public ConfigValidationException(String message) {
        super(message);
    }

    public ConfigValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigValidationException(Throwable cause) {
        super(cause);
    }

    public ConfigValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
