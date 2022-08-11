package org.apache.dubbo.validation.support;

import java.util.ArrayList;
import java.util.List;

/**
 * rpc method validation exception
 * @see org.apache.dubbo.validation.Validator#validate
 */
public class MethodValidatedException extends RuntimeException {

    private static final long serialVersionUID = 3588016356573293333L;
    private final List<ValidationResult> validationResults;

    public MethodValidatedException(List<ValidationResult> validationResults) {
        this.validationResults = validationResults;
    }

    public MethodValidatedException(String message) {
        super(message);
        this.validationResults = new ArrayList<>();
    }

    public MethodValidatedException(String message, List<ValidationResult> validationResults) {
        super(message);
        this.validationResults = validationResults;

    }

    public List<ValidationResult> getValidationResults() {
        return validationResults;
    }
}
