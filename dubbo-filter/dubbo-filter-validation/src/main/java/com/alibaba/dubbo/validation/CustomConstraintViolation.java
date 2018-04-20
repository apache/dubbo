package com.alibaba.dubbo.validation;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import java.io.Serializable;

/**
 * Custom constraint violation to store the validation errors
 * In order to instead of the implementation of hibernate validator
 * @author stefli
 * @param <T>
 */
public class CustomConstraintViolation<T> implements ConstraintViolation<T>, Serializable {

    private static final long serialVersionUID = -3042079204640314031L;

    private String message;
    private Path path;

    public CustomConstraintViolation() {

    }

    public CustomConstraintViolation(String message, Path path) {
        this.message = message;
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getMessageTemplate() {
        return null;
    }

    @Override
    public T getRootBean() {
        return null;
    }

    @Override
    public Class<T> getRootBeanClass() {
        return null;
    }

    @Override
    public Object getLeafBean() {
        return null;
    }

    @Override
    public Object[] getExecutableParameters() {
        return null;
    }

    @Override
    public Object getExecutableReturnValue() {
        return null;
    }

    @Override
    public Path getPropertyPath() {
        return this.path;
    }

    @Override
    public Object getInvalidValue() {
        return null;
    }

    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return null;
    }

    @Override
    public <U> U unwrap(Class<U> type) {
        return null;
    }

}