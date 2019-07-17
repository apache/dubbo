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

import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.springframework.core.annotation.AnnotatedElementUtils.getMergedAnnotation;
import static org.springframework.core.annotation.AnnotationAttributes.fromMap;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes;
import static org.springframework.core.annotation.AnnotationUtils.getDefaultValue;
import static org.springframework.util.ClassUtils.getAllInterfacesForClass;
import static org.springframework.util.ClassUtils.resolveClassName;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.ObjectUtils.containsElement;
import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.trimWhitespace;

/**
 * Annotation Utilities Class
 *
 * @see org.springframework.core.annotation.AnnotationUtils
 * @since 2.5.11
 */
public class AnnotationUtils {


    @Deprecated
    public static String resolveInterfaceName(Service service, Class<?> defaultInterfaceClass)
            throws IllegalStateException {

        String interfaceName;
        if (hasText(service.interfaceName())) {
            interfaceName = service.interfaceName();
        } else if (!void.class.equals(service.interfaceClass())) {
            interfaceName = service.interfaceClass().getName();
        } else if (defaultInterfaceClass.isInterface()) {
            interfaceName = defaultInterfaceClass.getName();
        } else {
            throw new IllegalStateException(
                    "The @Service undefined interfaceClass or interfaceName, and the type "
                            + defaultInterfaceClass.getName() + " is not a interface.");
        }

        return interfaceName;

    }

    /**
     * Resolve the interface name from {@link AnnotationAttributes}
     *
     * @param attributes            {@link AnnotationAttributes} instance, may be {@link Service @Service} or {@link Reference @Reference}
     * @param defaultInterfaceClass the default {@link Class class} of interface
     * @return the interface name if found
     * @throws IllegalStateException if interface name was not found
     */
    public static String resolveInterfaceName(AnnotationAttributes attributes, Class<?> defaultInterfaceClass) {
        return resolveServiceInterfaceClass(attributes, defaultInterfaceClass).getName();
    }

    /**
     * Get the attribute value
     *
     * @param attributes {@link AnnotationAttributes the annotation attributes}
     * @param name       the name of attribute
     * @param <T>        the type of attribute value
     * @return the attribute value if found
     * @since 2.7.3
     */
    public static <T> T getAttribute(AnnotationAttributes attributes, String name) {
        return (T) attributes.get(name);
    }

    /**
     * Resolve the {@link Class class} of Dubbo Service interface from the specified
     * {@link AnnotationAttributes annotation attributes} and annotated {@link Class class}.
     *
     * @param attributes            {@link AnnotationAttributes annotation attributes}
     * @param defaultInterfaceClass the annotated {@link Class class}.
     * @return the {@link Class class} of Dubbo Service interface
     * @throws IllegalArgumentException if can't resolved
     */
    public static Class<?> resolveServiceInterfaceClass(AnnotationAttributes attributes, Class<?> defaultInterfaceClass)
            throws IllegalArgumentException {

        ClassLoader classLoader = defaultInterfaceClass != null ? defaultInterfaceClass.getClassLoader() : Thread.currentThread().getContextClassLoader();

        Class<?> interfaceClass = getAttribute(attributes, "interfaceClass");

        if (void.class.equals(interfaceClass)) { // default or set void.class for purpose.

            interfaceClass = null;

            String interfaceClassName = getAttribute(attributes, "interfaceName");

            if (hasText(interfaceClassName)) {
                if (ClassUtils.isPresent(interfaceClassName, classLoader)) {
                    interfaceClass = resolveClassName(interfaceClassName, classLoader);
                }
            }

        }

        if (interfaceClass == null && defaultInterfaceClass != null) {
            // Find all interfaces from the annotated class
            // To resolve an issue : https://github.com/apache/dubbo/issues/3251
            Class<?>[] allInterfaces = getAllInterfacesForClass(defaultInterfaceClass);

            if (allInterfaces.length > 0) {
                interfaceClass = allInterfaces[0];
            }

        }

        Assert.notNull(interfaceClass,
                "@Service interfaceClass() or interfaceName() or interface class must be present!");

        Assert.isTrue(interfaceClass.isInterface(),
                "The annotated type must be an interface!");

        return interfaceClass;
    }

    @Deprecated
    public static String resolveInterfaceName(Reference reference, Class<?> defaultInterfaceClass)
            throws IllegalStateException {

        String interfaceName;
        if (!"".equals(reference.interfaceName())) {
            interfaceName = reference.interfaceName();
        } else if (!void.class.equals(reference.interfaceClass())) {
            interfaceName = reference.interfaceClass().getName();
        } else if (defaultInterfaceClass.isInterface()) {
            interfaceName = defaultInterfaceClass.getName();
        } else {
            throw new IllegalStateException(
                    "The @Reference undefined interfaceClass or interfaceName, and the type "
                            + defaultInterfaceClass.getName() + " is not a interface.");
        }

        return interfaceName;

    }


    // Cloned from https://github.com/alibaba/spring-context-support/blob/1.0.2/src/main/java/com/alibaba/spring/util/AnnotationUtils.java

    /**
     * Is specified {@link Annotation} present on {@link Method}'s declaring class or parameters or itself.
     *
     * @param method          {@link Method}
     * @param annotationClass {@link Annotation} type
     * @param <A>             {@link Annotation} type
     * @return If present , return <code>true</code> , or <code>false</code>
     * @since 2.6.6
     */
    public static <A extends Annotation> boolean isPresent(Method method, Class<A> annotationClass) {

        Map<ElementType, List<A>> annotationsMap = findAnnotations(method, annotationClass);

        return !annotationsMap.isEmpty();

    }

    /**
     * Find specified {@link Annotation} type maps from {@link Method}
     *
     * @param method          {@link Method}
     * @param annotationClass {@link Annotation} type
     * @param <A>             {@link Annotation} type
     * @return {@link Annotation} type maps , the {@link ElementType} as key ,
     * the list of {@link Annotation} as value.
     * If {@link Annotation} was annotated on {@link Method}'s parameters{@link ElementType#PARAMETER} ,
     * the associated {@link Annotation} list may contain multiple elements.
     * @since 2.6.6
     */
    public static <A extends Annotation> Map<ElementType, List<A>> findAnnotations(Method method,
                                                                                   Class<A> annotationClass) {

        Retention retention = annotationClass.getAnnotation(Retention.class);

        RetentionPolicy retentionPolicy = retention.value();

        if (!RetentionPolicy.RUNTIME.equals(retentionPolicy)) {
            return Collections.emptyMap();
        }

        Map<ElementType, List<A>> annotationsMap = new LinkedHashMap<ElementType, List<A>>();

        Target target = annotationClass.getAnnotation(Target.class);

        ElementType[] elementTypes = target.value();


        for (ElementType elementType : elementTypes) {

            List<A> annotationsList = new LinkedList<A>();

            switch (elementType) {

                case PARAMETER:

                    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

                    for (Annotation[] annotations : parameterAnnotations) {

                        for (Annotation annotation : annotations) {

                            if (annotationClass.equals(annotation.annotationType())) {

                                annotationsList.add((A) annotation);

                            }

                        }

                    }

                    break;

                case METHOD:

                    A annotation = findAnnotation(method, annotationClass);

                    if (annotation != null) {

                        annotationsList.add(annotation);

                    }

                    break;

                case TYPE:

                    Class<?> beanType = method.getDeclaringClass();

                    A annotation2 = findAnnotation(beanType, annotationClass);

                    if (annotation2 != null) {

                        annotationsList.add(annotation2);

                    }

                    break;

            }

            if (!annotationsList.isEmpty()) {

                annotationsMap.put(elementType, annotationsList);

            }


        }

        return unmodifiableMap(annotationsMap);

    }

    /**
     * Get the {@link Annotation} attributes
     *
     * @param annotation           specified {@link Annotation}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     * @since 2.6.6
     * @deprecated
     */
    @Deprecated
    public static Map<String, Object> getAttributes(Annotation annotation, boolean ignoreDefaultValue,
                                                    String... ignoreAttributeNames) {
        return getAttributes(annotation, null, ignoreDefaultValue, ignoreAttributeNames);
    }

    /**
     * Get the {@link Annotation} attributes
     *
     * @param annotation           specified {@link Annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     * @since 2.6.6
     */
    public static Map<String, Object> getAttributes(Annotation annotation, PropertyResolver propertyResolver,
                                                    boolean ignoreDefaultValue, String... ignoreAttributeNames) {

        if (annotation == null) {
            return emptyMap();
        }

        Map<String, Object> attributes = getAnnotationAttributes(annotation);

        Map<String, Object> actualAttributes = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {

            String attributeName = entry.getKey();
            Object attributeValue = entry.getValue();

            // ignore default attribute value
            if (ignoreDefaultValue && nullSafeEquals(attributeValue, getDefaultValue(annotation, attributeName))) {
                continue;
            }

            /**
             * @since 2.7.1
             * ignore annotation member
             */
            if (attributeValue.getClass().isAnnotation()) {
                continue;
            }
            if (attributeValue.getClass().isArray() && attributeValue.getClass().getComponentType().isAnnotation()) {
                continue;
            }
            actualAttributes.put(attributeName, attributeValue);
        }


        return resolvePlaceholders(actualAttributes, propertyResolver, ignoreAttributeNames);
    }

    /**
     * Resolve the placeholders from the specified annotation attributes
     *
     * @param sourceAnnotationAttributes the source of annotation attributes
     * @param propertyResolver           {@link PropertyResolver}
     * @param ignoreAttributeNames       the attribute names to be ignored
     * @return a new resolved annotation attributes , non-null and read-only
     * @since 2.7.3
     */
    public static Map<String, Object> resolvePlaceholders(Map<String, Object> sourceAnnotationAttributes,
                                                          PropertyResolver propertyResolver,
                                                          String... ignoreAttributeNames) {

        if (isEmpty(sourceAnnotationAttributes)) {
            return emptyMap();
        }

        Map<String, Object> resolvedAnnotationAttributes = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : sourceAnnotationAttributes.entrySet()) {

            String attributeName = entry.getKey();

            // ignore attribute name to skip
            if (containsElement(ignoreAttributeNames, attributeName)) {
                continue;
            }

            Object attributeValue = entry.getValue();

            if (attributeValue instanceof String) {
                attributeValue = resolvePlaceholders(valueOf(attributeValue), propertyResolver);
            } else if (attributeValue instanceof String[]) {
                String[] values = (String[]) attributeValue;
                for (int i = 0; i < values.length; i++) {
                    values[i] = resolvePlaceholders(values[i], propertyResolver);
                }
                attributeValue = values;
            }

            resolvedAnnotationAttributes.put(attributeName, attributeValue);
        }

        return unmodifiableMap(resolvedAnnotationAttributes);
    }

    /**
     * Get {@link AnnotationAttributes the annotation attributes} after merging and resolving the placeholders
     *
     * @param annotatedElement     {@link AnnotatedElement the annotated element}
     * @param annotationType       the {@link Class tyoe} pf {@link Annotation annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return If the specified annotation type is not found, return <code>null</code>
     * @since 2.7.3
     */
    public static AnnotationAttributes getMergedAttributes(AnnotatedElement annotatedElement,
                                                           Class<? extends Annotation> annotationType,
                                                           PropertyResolver propertyResolver,
                                                           boolean ignoreDefaultValue,
                                                           String... ignoreAttributeNames) {
        Annotation annotation = getMergedAnnotation(annotatedElement, annotationType);
        return annotation == null ? null : fromMap(getAttributes(annotation, propertyResolver, ignoreDefaultValue, ignoreAttributeNames));

    }

    private static String resolvePlaceholders(String attributeValue, PropertyResolver propertyResolver) {
        String resolvedValue = attributeValue;
        if (propertyResolver != null) {
            resolvedValue = propertyResolver.resolvePlaceholders(resolvedValue);
            resolvedValue = trimWhitespace(resolvedValue);
        }
        return resolvedValue;
    }

}
