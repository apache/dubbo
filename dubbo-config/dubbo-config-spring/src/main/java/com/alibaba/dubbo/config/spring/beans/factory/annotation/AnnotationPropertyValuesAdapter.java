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
package com.alibaba.dubbo.config.spring.beans.factory.annotation;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.core.env.PropertyResolver;

import java.lang.annotation.Annotation;

import static com.alibaba.dubbo.config.spring.util.AnnotationUtils.getAttributes;

/**
 * {@link Annotation} {@link PropertyValues} Adapter
 *
 * @see Annotation
 * @see PropertyValues
 * @since 2.5.11
 */
class AnnotationPropertyValuesAdapter implements PropertyValues {

    private final Annotation annotation;

    private final PropertyResolver propertyResolver;

    private final boolean ignoreDefaultValue;

    private final PropertyValues delegate;

    public AnnotationPropertyValuesAdapter(Annotation annotation, PropertyResolver propertyResolver, boolean ignoreDefaultValue, String... ignoreAttributeNames) {
        this.annotation = annotation;
        this.propertyResolver = propertyResolver;
        this.ignoreDefaultValue = ignoreDefaultValue;
        this.delegate = adapt(annotation, ignoreDefaultValue, ignoreAttributeNames);
    }

    public AnnotationPropertyValuesAdapter(Annotation annotation, PropertyResolver propertyResolver, String... ignoreAttributeNames) {
        this(annotation, propertyResolver, true, ignoreAttributeNames);
    }

    private PropertyValues adapt(Annotation annotation, boolean ignoreDefaultValue, String... ignoreAttributeNames) {
        return new MutablePropertyValues(getAttributes(annotation, propertyResolver, ignoreDefaultValue, ignoreAttributeNames));
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public boolean isIgnoreDefaultValue() {
        return ignoreDefaultValue;
    }

    @Override
    public PropertyValue[] getPropertyValues() {
        return delegate.getPropertyValues();
    }

    @Override
    public PropertyValue getPropertyValue(String propertyName) {
        return delegate.getPropertyValue(propertyName);
    }

    @Override
    public PropertyValues changesSince(PropertyValues old) {
        return delegate.changesSince(old);
    }

    @Override
    public boolean contains(String propertyName) {
        return delegate.contains(propertyName);
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }
}
