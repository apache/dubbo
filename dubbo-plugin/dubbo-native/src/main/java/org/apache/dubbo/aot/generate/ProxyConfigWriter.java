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

import org.apache.dubbo.aot.api.JdkProxyDescriber;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Write a {@link ProxyConfigMetadataRepository} to the JSON output expected by the GraalVM
 * {@code native-image} compiler, typically named {@code proxy-config.json}.
 */
public class ProxyConfigWriter {

    public static final ProxyConfigWriter INSTANCE = new ProxyConfigWriter();

    public void write(BasicJsonWriter writer, ProxyConfigMetadataRepository repository) {
        writer.writeArray(
                repository.getProxyDescribers().stream().map(this::toAttributes).collect(Collectors.toList()));
    }

    private Map<String, Object> toAttributes(JdkProxyDescriber describer) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        handleCondition(attributes, describer);
        attributes.put("interfaces", describer.getProxiedInterfaces());
        return attributes;
    }

    private void handleCondition(Map<String, Object> attributes, JdkProxyDescriber describer) {
        if (describer.getReachableType() != null) {
            Map<String, Object> conditionAttributes = new LinkedHashMap<>();
            conditionAttributes.put("typeReachable", describer.getReachableType());
            attributes.put("condition", conditionAttributes);
        }
    }
}
