/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.validation.support.jsr303;

import java.io.Serializable;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.ValidationException;
import javax.validation.metadata.ConstraintDescriptor;

/**
 * DubboConstraintViolation
 */
public class DubboConstraintViolation<T> implements ConstraintViolation<T>, Serializable {

    private static final long serialVersionUID = 885686998063132138L;
    private String interpolatedMessage;
    private Object value;
    private Path propertyPath;
    private String messageTemplate;
    private Object[] executableParameters;
    private Object executableReturnValue;
    private int hashCode;

    public DubboConstraintViolation() {
    }

    public DubboConstraintViolation(ConstraintViolation<T> violation) {
        this(violation.getMessageTemplate(), violation.getMessage(), violation.getInvalidValue(), violation.getPropertyPath(),
                violation.getExecutableParameters(), violation.getExecutableReturnValue());
    }

    public DubboConstraintViolation(String messageTemplate,
                                    String interpolatedMessage,
                                    Object value,
                                    Path propertyPath,
                                    Object[] executableParameters,
                                    Object executableReturnValue) {
        this.messageTemplate = messageTemplate;
        this.interpolatedMessage = interpolatedMessage;
        this.value = value;
        this.propertyPath = propertyPath;
        this.executableParameters = executableParameters;
        this.executableReturnValue = executableReturnValue;
        // pre-calculate hash code, the class is immutable and hashCode is needed often
        this.hashCode = createHashCode();
    }

    @Override
    public final String getMessage() {
        return interpolatedMessage;
    }

    @Override
    public final String getMessageTemplate() {
        return messageTemplate;
    }

    @Override
    public final T getRootBean() {
        return null;
    }

    @Override
    public final Class<T> getRootBeanClass() {
        return null;
    }

    @Override
    public final Object getLeafBean() {
        return null;
    }

    @Override
    public final Object getInvalidValue() {
        return value;
    }

    @Override
    public final Path getPropertyPath() {
        return propertyPath;
    }

    @Override
    public final ConstraintDescriptor<?> getConstraintDescriptor() {
        return null;
    }

    @Override
    public <C> C unwrap(Class<C> type) {
        if ( type.isAssignableFrom( ConstraintViolation.class ) ) {
            return type.cast( this );
        }
        throw new ValidationException("Type " + type.toString() + " not supported for unwrapping.");
    }

    @Override
    public Object[] getExecutableParameters() {
        return executableParameters;
    }

    @Override
    public Object getExecutableReturnValue() {
        return executableReturnValue;
    }

    @Override
    // IMPORTANT - some behaviour of Validator depends on the correct implementation of this equals method! (HF)

    // Do not take expressionVariables into account here. If everything else matches, the two CV should be considered
    // equals (and because of the scary comment above). After all, expressionVariables is just a hint about how we got
    // to the actual CV. (NF)
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        DubboConstraintViolation<?> that = (DubboConstraintViolation<?>) o;

        if ( interpolatedMessage != null ? !interpolatedMessage.equals( that.interpolatedMessage ) : that.interpolatedMessage != null ) {
            return false;
        }
        if ( propertyPath != null ? !propertyPath.equals( that.propertyPath ) : that.propertyPath != null ) {
            return false;
        }
        if ( messageTemplate != null ? !messageTemplate.equals( that.messageTemplate ) : that.messageTemplate != null ) {
            return false;
        }
        if ( value != null ? !value.equals( that.value ) : that.value != null ) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append( "DubboConstraintViolation" );
        sb.append( "{interpolatedMessage='" ).append( interpolatedMessage ).append( '\'' );
        sb.append( ", propertyPath=" ).append( propertyPath );
        sb.append( ", messageTemplate='" ).append( messageTemplate ).append( '\'' );
        sb.append( ", value='" ).append( value ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }

    // Same as for equals, do not take expressionVariables into account here.
    private int createHashCode() {
        int result = interpolatedMessage != null ? interpolatedMessage.hashCode() : 0;
        result = 31 * result + ( propertyPath != null ? propertyPath.hashCode() : 0 );
        result = 31 * result + ( value != null ? value.hashCode() : 0 );
        result = 31 * result + ( messageTemplate != null ? messageTemplate.hashCode() : 0 );
        return result;
    }
}

