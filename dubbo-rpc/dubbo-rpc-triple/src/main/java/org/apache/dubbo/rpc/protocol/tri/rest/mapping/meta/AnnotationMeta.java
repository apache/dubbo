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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unchecked")
public final class AnnotationMeta<A extends Annotation> {

    private final Map<String, Optional<Object>> cache = CollectionUtils.newConcurrentHashMap();
    private final AnnotatedElement element;
    private final A annotation;
    private final RestToolKit toolKit;

    private Map<String, Object> attributes;

    public AnnotationMeta(AnnotatedElement element, A annotation, RestToolKit toolKit) {
        this.element = element;
        this.annotation = annotation;
        this.toolKit = toolKit;
    }

    public A getAnnotation() {
        return annotation;
    }

    public Class<? extends Annotation> getAnnotationType() {
        return annotation.annotationType();
    }

    public Map<String, Object> getAttributes() {
        Map<String, Object> attributes = this.attributes;
        if (attributes == null) {
            Map<String, Object> map = toolKit.getAttributes(element, annotation);
            if (CollectionUtils.isEmptyMap(map)) {
                attributes = Collections.emptyMap();
            } else {
                attributes = CollectionUtils.newHashMap(map.size());
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        value = toolKit.resolvePlaceholders((String) value);
                    } else if (value instanceof String[]) {
                        String[] array = (String[]) value;
                        for (int i = 0, len = array.length; i < len; i++) {
                            array[i] = toolKit.resolvePlaceholders(array[i]);
                        }
                    }
                    attributes.put(entry.getKey(), value);
                }
            }
            this.attributes = attributes;
        }
        return attributes;
    }

    public boolean hasAttribute(String attributeName) {
        return getAttributes().containsKey(attributeName);
    }

    public String getValue() {
        return getString("value");
    }

    public String[] getValueArray() {
        return getStringArray("value");
    }

    public String getString(String attributeName) {
        return getRequiredAttribute(attributeName, String.class);
    }

    public String[] getStringArray(String attributeName) {
        return getRequiredAttribute(attributeName, String[].class);
    }

    public boolean getBoolean(String attributeName) {
        return getRequiredAttribute(attributeName, Boolean.class);
    }

    public <N extends Number> N getNumber(String attributeName) {
        return (N) getRequiredAttribute(attributeName, Number.class);
    }

    public <E extends Enum<?>> E getEnum(String attributeName) {
        return (E) getRequiredAttribute(attributeName, Enum.class);
    }

    public <E extends Enum<?>> E[] getEnumArray(String attributeName) {
        return (E[]) getRequiredAttribute(attributeName, Enum[].class);
    }

    public <T> Class<? extends T> getClass(String attributeName) {
        return getRequiredAttribute(attributeName, Class.class);
    }

    public Class<?>[] getClassArray(String attributeName) {
        return getRequiredAttribute(attributeName, Class[].class);
    }

    public <A1 extends Annotation> AnnotationMeta<A1> getAnnotation(String attributeName) {
        return (AnnotationMeta<A1>) cache.computeIfAbsent(attributeName, k -> {
                    if (getAttributes().get(attributeName) == null) {
                        return Optional.empty();
                    }
                    Annotation annotation = getRequiredAttribute(attributeName, Annotation.class);
                    return Optional.of(new AnnotationMeta<>(getAnnotationType(), annotation, toolKit));
                })
                .orElseThrow(() -> attributeNotFound(attributeName));
    }

    public <A1 extends Annotation> AnnotationMeta<A1>[] getAnnotationArray(String attributeName) {
        return (AnnotationMeta<A1>[]) cache.computeIfAbsent(attributeName, k -> {
                    if (getAttributes().get(attributeName) == null) {
                        return Optional.empty();
                    }
                    Annotation[] annotation = getRequiredAttribute(attributeName, Annotation[].class);
                    int len = annotation.length;
                    AnnotationMeta<A1>[] metas = new AnnotationMeta[len];
                    for (int i = 0; i < len; i++) {
                        metas[i] = new AnnotationMeta<>(getAnnotationType(), (A1) annotation[i], toolKit);
                    }
                    return Optional.of(metas);
                })
                .orElseThrow(() -> attributeNotFound(attributeName));
    }

    public <A1 extends Annotation> A1 getAnnotation(String attributeName, Class<A1> annotationType) {
        return getRequiredAttribute(attributeName, annotationType);
    }

    public <A1 extends Annotation> A1[] getAnnotationArray(String attributeName, Class<A1> annotationType) {
        Class<?> arrayType = Array.newInstance(annotationType, 0).getClass();
        return (A1[]) getRequiredAttribute(attributeName, arrayType);
    }

    public <T> T getRequiredAttribute(String attributeName, Class<T> expectedType) {
        Object value = getAttributes().get(attributeName);
        if (value == null) {
            throw attributeNotFound(attributeName);
        }
        if (value instanceof Throwable) {
            throw new IllegalArgumentException(
                    String.format(
                            "Attribute '%s' for annotation [%s] was not resolvable due to exception [%s]",
                            attributeName, getAnnotationType().getName(), value),
                    (Throwable) value);
        }
        if (expectedType.isInstance(value)) {
            return (T) value;
        }
        if (expectedType == String.class) {
            return (T) value.toString();
        }
        if (expectedType == Boolean.class) {
            Boolean b = StringUtils.toBoolean(value.toString());
            return (T) (b == null ? Boolean.FALSE : b);
        }
        if (expectedType == Number.class) {
            String str = value.toString();
            return str.indexOf('.') > -1 ? (T) Double.valueOf(str) : (T) Long.valueOf(str);
        }
        if (expectedType.isArray()) {
            Class<?> expectedComponentType = expectedType.getComponentType();
            if (expectedComponentType.isInstance(value)) {
                Object array = Array.newInstance(expectedComponentType, 1);
                Array.set(array, 0, value);
                return (T) array;
            }
            if (expectedComponentType == String.class) {
                String[] array;
                if (value.getClass().isArray()) {
                    int len = Array.getLength(value);
                    array = new String[len];
                    for (int i = 0; i < len; i++) {
                        array[i] = Array.get(value, i).toString();
                    }
                } else {
                    array = new String[] {value.toString()};
                }
                return (T) array;
            }
        }
        throw new IllegalArgumentException(String.format(
                "Attribute '%s' is of type %s, but %s was expected in attributes for annotation [%s]",
                attributeName,
                value.getClass().getSimpleName(),
                expectedType.getSimpleName(),
                getAnnotationType().getName()));
    }

    private IllegalArgumentException attributeNotFound(String attributeName) {
        return new IllegalArgumentException(String.format(
                "Attribute '%s' not found in attributes for annotation [%s]",
                attributeName, getAnnotationType().getName()));
    }

    @Override
    public int hashCode() {
        return 31 * element.hashCode() + annotation.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != AnnotationMeta.class) {
            return false;
        }
        AnnotationMeta<?> other = (AnnotationMeta<?>) obj;
        return element.equals(other.element) && annotation.equals(other.annotation);
    }

    @Override
    public String toString() {
        return "AnnotationMeta{" + "element=" + element + ", annotation=" + annotation + '}';
    }
}
