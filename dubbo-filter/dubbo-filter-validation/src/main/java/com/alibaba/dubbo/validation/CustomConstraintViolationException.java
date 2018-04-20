package com.alibaba.dubbo.validation;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

/**
 * Custom constraint violation exception to wrap the errors
 * @author stefli
 *
 */
public class CustomConstraintViolationException extends ConstraintViolationException {

    private static final long serialVersionUID = 1915414386865706919L;

    private String message;
    private Set<ConstraintViolation<?>> constraintViolations;

    public CustomConstraintViolationException() {
        super(null);
    }

    public CustomConstraintViolationException(String message, Set<ConstraintViolation<?>> constraintViolations) {
        super(message, constraintViolations);
        this.message = message;
        this.constraintViolations = constraintViolations;
    }

    public CustomConstraintViolationException(Set<ConstraintViolation<?>> constraintViolations) {
        this(null, constraintViolations);
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Set<ConstraintViolation<?>> getConstraintViolations() {
        return constraintViolations;
    }

    public void setConstraintViolations(Set<ConstraintViolation<?>> constraintViolations) {
        this.constraintViolations = constraintViolations;
    }

}
