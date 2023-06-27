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
package org.apache.dubbo.aot.generate;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Write {@link ReflectConfigMetadataRepository} to the JSON output expected by the GraalVM
 * {@code native-image} compiler, typically named {@code reflect-config.json}
 * or {@code jni-config.json}.
 */
public class ReflectionConfigWriter {

    public static final ReflectionConfigWriter INSTANCE = new ReflectionConfigWriter();

    public void write(BasicJsonWriter writer, ReflectConfigMetadataRepository repository) {
        writer.writeArray(repository.getTypes().stream().map(this::toAttributes).collect(Collectors.toList()));
    }

    private Map<String, Object> toAttributes(TypeDescriber describer) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("name", describer.getName());
        handleCondition(attributes, describer);
        handleCategories(attributes, describer.getMemberCategories());
        handleFields(attributes, describer.getFields());
        handleExecutables(attributes, describer.getConstructors());
        handleExecutables(attributes, describer.getMethods());
        return attributes;
    }

    private void handleCondition(Map<String, Object> attributes, TypeDescriber describer) {
        if (describer.getReachableType() != null) {
            Map<String, Object> conditionAttributes = new LinkedHashMap<>();
            conditionAttributes.put("typeReachable", describer.getReachableType());
            attributes.put("condition", conditionAttributes);
        }
    }

    private void handleFields(Map<String, Object> attributes, Set<FieldDescriber> fieldDescribers) {
        addIfNotEmpty(attributes, "fields", fieldDescribers.stream().map(this::toAttributes).collect(Collectors.toList()));
    }

    private Map<String, Object> toAttributes(FieldDescriber describer) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("name", describer.getName());
        return attributes;
    }

    private void handleExecutables(Map<String, Object> attributes, Set<ExecutableDescriber> executableDescribers) {
        addIfNotEmpty(attributes, "methods", executableDescribers.stream()
            .filter(h -> h.getMode().equals(ExecutableMode.INVOKE))
            .map(this::toAttributes).collect(Collectors.toList()));
        addIfNotEmpty(attributes, "queriedMethods", executableDescribers.stream()
            .filter(h -> h.getMode().equals(ExecutableMode.INTROSPECT))
            .map(this::toAttributes).collect(Collectors.toList()));
    }

    private Map<String, Object> toAttributes(ExecutableDescriber describer) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("name", describer.getName());
        attributes.put("parameterTypes", describer.getParameterTypes());
        return attributes;
    }

    private void handleCategories(Map<String, Object> attributes, Set<MemberCategory> categories) {
        categories.forEach(category -> {
                switch (category) {
                    case PUBLIC_FIELDS:
                        attributes.put("allPublicFields", true);
                        break;
                    case DECLARED_FIELDS:
                        attributes.put("allDeclaredFields", true);
                        break;
                    case INTROSPECT_PUBLIC_CONSTRUCTORS:
                        attributes.put("queryAllPublicConstructors", true);
                        break;
                    case INTROSPECT_DECLARED_CONSTRUCTORS:
                        attributes.put("queryAllDeclaredConstructors", true);
                        break;
                    case INVOKE_PUBLIC_CONSTRUCTORS:
                        attributes.put("allPublicConstructors", true);
                        break;
                    case INVOKE_DECLARED_CONSTRUCTORS:
                        attributes.put("allDeclaredConstructors", true);
                        break;
                    case INTROSPECT_PUBLIC_METHODS:
                        attributes.put("queryAllPublicMethods", true);
                        break;
                    case INTROSPECT_DECLARED_METHODS:
                        attributes.put("queryAllDeclaredMethods", true);
                        break;
                    case INVOKE_PUBLIC_METHODS:
                        attributes.put("allPublicMethods", true);
                        break;
                    case INVOKE_DECLARED_METHODS:
                        attributes.put("allDeclaredMethods", true);
                        break;
                    case PUBLIC_CLASSES:
                        attributes.put("allPublicClasses", true);
                        break;
                    case DECLARED_CLASSES:
                        attributes.put("allDeclaredClasses", true);
                        break;
                    default:
                        break;
                }
            }
        );
    }

    private void addIfNotEmpty(Map<String, Object> attributes, String name, Object value) {
        if ((value instanceof Collection<?> && ((Collection<?>) value).size() != 0)) {
            attributes.put(name, value);
        }
    }
}
