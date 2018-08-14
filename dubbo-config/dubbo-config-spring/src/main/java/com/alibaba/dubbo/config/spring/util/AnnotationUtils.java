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
package com.alibaba.dubbo.config.spring.util;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.core.env.PropertyResolver;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static java.lang.String.valueOf;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes;
import static org.springframework.core.annotation.AnnotationUtils.getDefaultValue;
import static org.springframework.util.CollectionUtils.arrayToList;
import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.StringUtils.trimAllWhitespace;

/**
 * Annotation Utilities Class
 *
 * @see org.springframework.core.annotation.AnnotationUtils
 * @since 2.5.11
 */
public class AnnotationUtils {

    /**
     * Get {@link Annotation} attributes
     *
     * @param annotation
     * @param propertyResolver
     * @param ignoreDefaultValue
     * @return non-null
     */
    public static Map<String, Object> getAttributes(Annotation annotation, PropertyResolver propertyResolver,
                                                    boolean ignoreDefaultValue, String... ignoreAttributeNames) {

        Set<String> ignoreAttributeNamesSet = new HashSet<String>(arrayToList(ignoreAttributeNames));

        Map<String, Object> attributes = getAnnotationAttributes(annotation);

        Map<String, Object> actualAttributes = new LinkedHashMap<String, Object>();

        boolean requiredResolve = propertyResolver != null;

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {

            String attributeName = entry.getKey();
            Object attributeValue = entry.getValue();

            // ignore default attribute value
            if (ignoreDefaultValue && nullSafeEquals(attributeValue, getDefaultValue(annotation, attributeName))) {
                continue;
            }

            // ignore attribute name
            if (ignoreAttributeNamesSet.contains(attributeName)) {
                continue;
            }

            if (requiredResolve && attributeValue instanceof String) { // Resolve Placeholder
                String resolvedValue = propertyResolver.resolvePlaceholders(valueOf(attributeValue));
                attributeValue = trimAllWhitespace(resolvedValue);
            }

            actualAttributes.put(attributeName, attributeValue);

        }

        return actualAttributes;

    }

    public static String resolveInterfaceName(Service service, Class<?> defaultInterfaceClass)
            throws IllegalStateException {

        String interfaceName;
        if (!"".equals(service.interfaceName())) {
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

}
