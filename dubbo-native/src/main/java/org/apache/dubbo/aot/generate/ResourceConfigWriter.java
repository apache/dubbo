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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Write a {@link ResourceConfigMetadataRepository} to the JSON output expected by the GraalVM
 * {@code native-image} compiler, typically named {@code resource-config.json}.
 */
public class ResourceConfigWriter {

    public static final ResourceConfigWriter INSTANCE = new ResourceConfigWriter();

    public void write(BasicJsonWriter writer, ResourceConfigMetadataRepository repository) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        addIfNotEmpty(attributes, "resources", toAttributes(repository.getIncludes(), repository.getExcludes()));
        handleResourceBundles(attributes, repository.getResourceBundles());
        writer.writeObject(attributes);
    }

    private Map<String, Object> toAttributes(List<ResourcePatternDescriber> includes, List<ResourcePatternDescriber> excludes) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        addIfNotEmpty(attributes, "includes", includes.stream().distinct().map(this::toAttributes).collect(Collectors.toList()));
        addIfNotEmpty(attributes, "excludes", excludes.stream().distinct().map(this::toAttributes).collect(Collectors.toList()));
        return attributes;
    }

    private void handleResourceBundles(Map<String, Object> attributes, Set<ResourceBundleDescriber> resourceBundleDescribers) {
        addIfNotEmpty(attributes, "bundles", resourceBundleDescribers.stream().map(this::toAttributes).collect(Collectors.toList()));
    }

    private Map<String, Object> toAttributes(ResourceBundleDescriber describer) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        handleCondition(attributes, describer);
        attributes.put("name", describer.getName());
        return attributes;
    }

    private Map<String, Object> toAttributes(ResourcePatternDescriber describer) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        handleCondition(attributes, describer);
        attributes.put("pattern", describer.toRegex().toString());
        return attributes;
    }

    private void addIfNotEmpty(Map<String, Object> attributes, String name, Object value) {
        if (value instanceof Collection<?>) {
            if (!((Collection<?>) value).isEmpty()) {
                attributes.put(name, value);
            }
        } else if (value instanceof Map<?, ?>) {
            if (!((Map<?, ?>) value).isEmpty()) {
                attributes.put(name, value);
            }
        } else if (value != null) {
            attributes.put(name, value);
        }
    }

    private void handleCondition(Map<String, Object> attributes, ConditionalDescriber conditionalDescriber) {
        if (conditionalDescriber.getReachableType() != null) {
            Map<String, Object> conditionAttributes = new LinkedHashMap<>();
            conditionAttributes.put("typeReachable", conditionalDescriber.getReachableType());
            attributes.put("condition", conditionAttributes);
        }
    }
}
