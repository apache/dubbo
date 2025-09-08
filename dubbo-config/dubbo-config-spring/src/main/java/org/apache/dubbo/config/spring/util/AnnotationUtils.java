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
package org.apache.dubbo.config.spring.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.ClassUtils;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static org.springframework.core.annotation.AnnotationAttributes.fromMap;
import static org.springframework.core.annotation.AnnotationUtils.getDefaultValue;
import static org.springframework.util.ClassUtils.resolveClassName;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ReflectionUtils.findMethod;
import static org.springframework.util.ReflectionUtils.invokeMethod;
import static org.springframework.util.StringUtils.trimWhitespace;

@SuppressWarnings("unchecked")
public abstract class AnnotationUtils {

    /**
     * The class name of AnnotatedElementUtils that is introduced since Spring Framework 4
     */
    public static final String ANNOTATED_ELEMENT_UTILS_CLASS_NAME =
            "org.springframework.core.annotation.AnnotatedElementUtils";

    private static final Map<Integer, Boolean> annotatedElementUtilsPresentCache = new ConcurrentHashMap<>();

    /**
     * Get the {@link Annotation} attributes
     *
     * @param annotation           specified {@link Annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     */
    public static Map<String, Object> getAttributes(
            Annotation annotation,
            PropertyResolver propertyResolver,
            boolean ignoreDefaultValue,
            String... ignoreAttributeNames) {
        return getAttributes(annotation, propertyResolver, false, false, ignoreDefaultValue, ignoreAttributeNames);
    }

    /**
     * Get the {@link Annotation} attributes
     *
     * @param annotationAttributes the attributes of specified {@link Annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     */
    public static Map<String, Object> getAttributes(
            Map<String, Object> annotationAttributes,
            PropertyResolver propertyResolver,
            String... ignoreAttributeNames) {

        Set<String> ignoreAttributeNamesSet = new HashSet<>(Arrays.asList(ignoreAttributeNames));

        Map<String, Object> actualAttributes = new LinkedHashMap<>();

        for (Map.Entry<String, Object> annotationAttribute : annotationAttributes.entrySet()) {

            String attributeName = annotationAttribute.getKey();
            Object attributeValue = annotationAttribute.getValue();

            // ignore attribute name
            if (ignoreAttributeNamesSet.contains(attributeName)) {
                continue;
            }

            if (attributeValue instanceof String) {
                attributeValue = resolvePlaceholders(valueOf(attributeValue), propertyResolver);
            } else if (attributeValue instanceof String[]) {
                String[] values = (String[]) attributeValue;
                for (int i = 0; i < values.length; i++) {
                    values[i] = resolvePlaceholders(values[i], propertyResolver);
                }
                attributeValue = values;
            }
            actualAttributes.put(attributeName, attributeValue);
        }
        return actualAttributes;
    }

    /**
     * @param annotation             specified {@link Annotation}
     * @param propertyResolver       {@link PropertyResolver} instance, e.g {@link Environment}
     * @param classValuesAsString    whether to turn Class references into Strings (for
     *                               compatibility with {@link org.springframework.core.type.AnnotationMetadata} or to
     *                               preserve them as Class references
     * @param nestedAnnotationsAsMap whether to turn nested Annotation instances into
     *                               {@link AnnotationAttributes} maps (for compatibility with
     *                               {@link org.springframework.core.type.AnnotationMetadata} or to preserve them as
     *                               Annotation instances
     * @param ignoreDefaultValue     whether ignore default value or not
     * @param ignoreAttributeNames   the attribute names of annotation should be ignored
     * @return
     */
    public static Map<String, Object> getAttributes(
            Annotation annotation,
            PropertyResolver propertyResolver,
            boolean classValuesAsString,
            boolean nestedAnnotationsAsMap,
            boolean ignoreDefaultValue,
            String... ignoreAttributeNames) {

        Map<String, Object> annotationAttributes =
                org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes(
                        annotation, classValuesAsString, nestedAnnotationsAsMap);

        String[] actualIgnoreAttributeNames = ignoreAttributeNames;

        if (ignoreDefaultValue && !isEmpty(annotationAttributes)) {

            List<String> attributeNamesToIgnore = new LinkedList<>(asList(ignoreAttributeNames));

            for (Map.Entry<String, Object> annotationAttribute : annotationAttributes.entrySet()) {
                String attributeName = annotationAttribute.getKey();
                Object attributeValue = annotationAttribute.getValue();
                if (nullSafeEquals(attributeValue, getDefaultValue(annotation, attributeName))) {
                    attributeNamesToIgnore.add(attributeName);
                }
            }
            // extends the ignored list
            actualIgnoreAttributeNames = attributeNamesToIgnore.toArray(new String[attributeNamesToIgnore.size()]);
        }

        return getAttributes(annotationAttributes, propertyResolver, actualIgnoreAttributeNames);
    }

    private static String resolvePlaceholders(String attributeValue, PropertyResolver propertyResolver) {
        String resolvedValue = attributeValue;
        if (propertyResolver != null) {
            resolvedValue = propertyResolver.resolvePlaceholders(resolvedValue);
            resolvedValue = trimWhitespace(resolvedValue);
        }
        return resolvedValue;
    }

    /**
     * Get the attribute value
     *
     * @param attributes    {@link Map the annotation attributes} or {@link AnnotationAttributes}
     * @param attributeName the name of attribute
     * @param <T>           the type of attribute value
     * @return the attribute value if found
     */
    public static <T> T getAttribute(Map<String, Object> attributes, String attributeName) {
        return getAttribute(attributes, attributeName, false);
    }

    /**
     * Get the attribute value the will
     *
     * @param attributes    {@link Map the annotation attributes} or {@link AnnotationAttributes}
     * @param attributeName the name of attribute
     * @param required      the required attribute or not
     * @param <T>           the type of attribute value
     * @return the attribute value if found
     * @throws IllegalStateException if attribute value can't be found
     */
    public static <T> T getAttribute(Map<String, Object> attributes, String attributeName, boolean required) {
        T value = getAttribute(attributes, attributeName, null);
        if (required && value == null) {
            throw new IllegalStateException("The attribute['" + attributeName + "] is required!");
        }
        return value;
    }

    /**
     * Get the attribute value with default value
     *
     * @param attributes    {@link Map the annotation attributes} or {@link AnnotationAttributes}
     * @param attributeName the name of attribute
     * @param defaultValue  the default value of attribute
     * @param <T>           the type of attribute value
     * @return the attribute value if found
     */
    public static <T> T getAttribute(Map<String, Object> attributes, String attributeName, T defaultValue) {
        T value = (T) attributes.get(attributeName);
        return value == null ? defaultValue : value;
    }

    /**
     * Get the required attribute value
     *
     * @param attributes    {@link Map the annotation attributes} or {@link AnnotationAttributes}
     * @param attributeName the name of attribute
     * @param <T>           the type of attribute value
     * @return the attribute value if found
     * @throws IllegalStateException if attribute value can't be found
     */
    public static <T> T getRequiredAttribute(Map<String, Object> attributes, String attributeName) {
        return getAttribute(attributes, attributeName, true);
    }

    /**
     * Get the {@link AnnotationAttributes}
     *
     * @param annotation           specified {@link Annotation}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     * @see #getAnnotationAttributes(Annotation, PropertyResolver, boolean, String...)
     */
    public static AnnotationAttributes getAnnotationAttributes(
            Annotation annotation, boolean ignoreDefaultValue, String... ignoreAttributeNames) {
        return getAnnotationAttributes(annotation, null, ignoreDefaultValue, ignoreAttributeNames);
    }

    /**
     * Get the {@link AnnotationAttributes}
     *
     * @param annotation             specified {@link Annotation}
     * @param propertyResolver       {@link PropertyResolver} instance, e.g {@link Environment}
     * @param classValuesAsString    whether to turn Class references into Strings (for
     *                               compatibility with {@link org.springframework.core.type.AnnotationMetadata} or to
     *                               preserve them as Class references
     * @param nestedAnnotationsAsMap whether to turn nested Annotation instances into
     *                               {@link AnnotationAttributes} maps (for compatibility with
     *                               {@link org.springframework.core.type.AnnotationMetadata} or to preserve them as
     *                               Annotation instances
     * @param ignoreAttributeNames   the attribute names of annotation should be ignored
     * @param ignoreDefaultValue     whether ignore default value or not
     * @return non-null
     * @see #getAttributes(Annotation, PropertyResolver, boolean, String...)
     * @see #getAnnotationAttributes(AnnotatedElement, Class, PropertyResolver, boolean, String...)
     */
    public static AnnotationAttributes getAnnotationAttributes(
            Annotation annotation,
            PropertyResolver propertyResolver,
            boolean classValuesAsString,
            boolean nestedAnnotationsAsMap,
            boolean ignoreDefaultValue,
            String... ignoreAttributeNames) {
        return fromMap(getAttributes(
                annotation,
                propertyResolver,
                classValuesAsString,
                nestedAnnotationsAsMap,
                ignoreDefaultValue,
                ignoreAttributeNames));
    }

    /**
     * Get the {@link AnnotationAttributes}
     *
     * @param annotation           specified {@link Annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     * @see #getAttributes(Annotation, PropertyResolver, boolean, String...)
     * @see #getAnnotationAttributes(AnnotatedElement, Class, PropertyResolver, boolean, String...)
     */
    public static AnnotationAttributes getAnnotationAttributes(
            Annotation annotation,
            PropertyResolver propertyResolver,
            boolean ignoreDefaultValue,
            String... ignoreAttributeNames) {
        return getAnnotationAttributes(
                annotation, propertyResolver, false, false, ignoreDefaultValue, ignoreAttributeNames);
    }

    /**
     * Get the {@link AnnotationAttributes}
     *
     * @param annotatedElement     {@link AnnotatedElement the annotated element}
     * @param annotationType       the {@link Class tyoe} pf {@link Annotation annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return if <code>annotatedElement</code> can't be found in <code>annotatedElement</code>, return <code>null</code>
     */
    public static AnnotationAttributes getAnnotationAttributes(
            AnnotatedElement annotatedElement,
            Class<? extends Annotation> annotationType,
            PropertyResolver propertyResolver,
            boolean classValuesAsString,
            boolean nestedAnnotationsAsMap,
            boolean ignoreDefaultValue,
            String... ignoreAttributeNames) {
        Annotation annotation = annotatedElement.getAnnotation(annotationType);
        return annotation == null
                ? null
                : getAnnotationAttributes(
                        annotation,
                        propertyResolver,
                        classValuesAsString,
                        nestedAnnotationsAsMap,
                        ignoreDefaultValue,
                        ignoreAttributeNames);
    }

    /**
     * Get the {@link AnnotationAttributes}, if the argument <code>tryMergedAnnotation</code> is <code>true</code>,
     * the {@link AnnotationAttributes} will be got from
     * {@link #tryGetMergedAnnotationAttributes(AnnotatedElement, Class, PropertyResolver, boolean, String...) merged annotation} first,
     * if failed, and then to get from
     * {@link #getAnnotationAttributes(AnnotatedElement, Class, PropertyResolver, boolean, boolean, String...) normal one}
     *
     * @param annotatedElement     {@link AnnotatedElement the annotated element}
     * @param annotationType       the {@link Class tyoe} pf {@link Annotation annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param tryMergedAnnotation  whether try merged annotation or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return if <code>annotatedElement</code> can't be found in <code>annotatedElement</code>, return <code>null</code>
     */
    public static AnnotationAttributes getAnnotationAttributes(
            AnnotatedElement annotatedElement,
            Class<? extends Annotation> annotationType,
            PropertyResolver propertyResolver,
            boolean ignoreDefaultValue,
            boolean tryMergedAnnotation,
            String... ignoreAttributeNames) {
        return getAnnotationAttributes(
                annotatedElement,
                annotationType,
                propertyResolver,
                false,
                false,
                ignoreDefaultValue,
                tryMergedAnnotation,
                ignoreAttributeNames);
    }

    /**
     * Get the {@link AnnotationAttributes}, if the argument <code>tryMergedAnnotation</code> is <code>true</code>,
     * the {@link AnnotationAttributes} will be got from
     * {@link #tryGetMergedAnnotationAttributes(AnnotatedElement, Class, PropertyResolver, boolean, String...) merged annotation} first,
     * if failed, and then to get from
     * {@link #getAnnotationAttributes(AnnotatedElement, Class, PropertyResolver, boolean, boolean, String...) normal one}
     *
     * @param annotatedElement       {@link AnnotatedElement the annotated element}
     * @param annotationType         the {@link Class tyoe} pf {@link Annotation annotation}
     * @param propertyResolver       {@link PropertyResolver} instance, e.g {@link Environment}
     * @param classValuesAsString    whether to turn Class references into Strings (for
     *                               compatibility with {@link org.springframework.core.type.AnnotationMetadata} or to
     *                               preserve them as Class references
     * @param nestedAnnotationsAsMap whether to turn nested Annotation instances into
     *                               {@link AnnotationAttributes} maps (for compatibility with
     *                               {@link org.springframework.core.type.AnnotationMetadata} or to preserve them as
     *                               Annotation instances
     * @param ignoreDefaultValue     whether ignore default value or not
     * @param tryMergedAnnotation    whether try merged annotation or not
     * @param ignoreAttributeNames   the attribute names of annotation should be ignored
     * @return if <code>annotatedElement</code> can't be found in <code>annotatedElement</code>, return <code>null</code>
     */
    public static AnnotationAttributes getAnnotationAttributes(
            AnnotatedElement annotatedElement,
            Class<? extends Annotation> annotationType,
            PropertyResolver propertyResolver,
            boolean classValuesAsString,
            boolean nestedAnnotationsAsMap,
            boolean ignoreDefaultValue,
            boolean tryMergedAnnotation,
            String... ignoreAttributeNames) {

        AnnotationAttributes attributes = null;

        if (tryMergedAnnotation) {
            attributes = tryGetMergedAnnotationAttributes(
                    annotatedElement,
                    annotationType,
                    propertyResolver,
                    classValuesAsString,
                    nestedAnnotationsAsMap,
                    ignoreDefaultValue,
                    ignoreAttributeNames);
        }

        if (attributes == null) {
            attributes = getAnnotationAttributes(
                    annotatedElement,
                    annotationType,
                    propertyResolver,
                    classValuesAsString,
                    nestedAnnotationsAsMap,
                    ignoreDefaultValue,
                    ignoreAttributeNames);
        }

        return attributes;
    }

    /**
     * Try to get the merged {@link Annotation annotation}
     *
     * @param annotatedElement       {@link AnnotatedElement the annotated element}
     * @param annotationType         the {@link Class tyoe} pf {@link Annotation annotation}
     * @param classValuesAsString    whether to turn Class references into Strings (for
     *                               compatibility with {@link org.springframework.core.type.AnnotationMetadata} or to
     *                               preserve them as Class references
     * @param nestedAnnotationsAsMap whether to turn nested Annotation instances into
     *                               {@link AnnotationAttributes} maps (for compatibility with
     *                               {@link org.springframework.core.type.AnnotationMetadata} or to preserve them as
     *                               Annotation instances
     * @return If current version of Spring Framework is below 4.2, return <code>null</code>
     */
    public static Annotation tryGetMergedAnnotation(
            AnnotatedElement annotatedElement,
            Class<? extends Annotation> annotationType,
            boolean classValuesAsString,
            boolean nestedAnnotationsAsMap) {

        Annotation mergedAnnotation = null;

        ClassLoader classLoader = annotationType.getClassLoader();

        if (annotatedElementUtilsPresentCache.computeIfAbsent(
                System.identityHashCode(classLoader),
                (_k) -> ClassUtils.isPresent(ANNOTATED_ELEMENT_UTILS_CLASS_NAME, classLoader))) {
            Class<?> annotatedElementUtilsClass = resolveClassName(ANNOTATED_ELEMENT_UTILS_CLASS_NAME, classLoader);
            // getMergedAnnotation method appears in the Spring Framework 4.2
            Method getMergedAnnotationMethod =
                    findMethod(annotatedElementUtilsClass, "getMergedAnnotation", AnnotatedElement.class, Class.class);
            if (getMergedAnnotationMethod != null) {
                mergedAnnotation =
                        (Annotation) invokeMethod(getMergedAnnotationMethod, null, annotatedElement, annotationType);
            }
        }

        return mergedAnnotation;
    }

    /**
     * Try to get {@link AnnotationAttributes the annotation attributes} after merging and resolving the placeholders
     *
     * @param annotatedElement       {@link AnnotatedElement the annotated element}
     * @param annotationType         the {@link Class tyoe} pf {@link Annotation annotation}
     * @param propertyResolver       {@link PropertyResolver} instance, e.g {@link Environment}
     * @param classValuesAsString    whether to turn Class references into Strings (for
     *                               compatibility with {@link org.springframework.core.type.AnnotationMetadata} or to
     *                               preserve them as Class references
     * @param nestedAnnotationsAsMap whether to turn nested Annotation instances into
     *                               {@link AnnotationAttributes} maps (for compatibility with
     *                               {@link org.springframework.core.type.AnnotationMetadata} or to preserve them as
     *                               Annotation instances
     * @param ignoreDefaultValue     whether ignore default value or not
     * @param ignoreAttributeNames   the attribute names of annotation should be ignored
     * @return If the specified annotation type is not found, return <code>null</code>
     */
    public static AnnotationAttributes tryGetMergedAnnotationAttributes(
            AnnotatedElement annotatedElement,
            Class<? extends Annotation> annotationType,
            PropertyResolver propertyResolver,
            boolean classValuesAsString,
            boolean nestedAnnotationsAsMap,
            boolean ignoreDefaultValue,
            String... ignoreAttributeNames) {
        Annotation annotation =
                tryGetMergedAnnotation(annotatedElement, annotationType, classValuesAsString, nestedAnnotationsAsMap);
        return annotation == null
                ? null
                : getAnnotationAttributes(
                        annotation,
                        propertyResolver,
                        classValuesAsString,
                        nestedAnnotationsAsMap,
                        ignoreDefaultValue,
                        ignoreAttributeNames);
    }
}
