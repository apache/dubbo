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

}
