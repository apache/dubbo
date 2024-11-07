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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi.model;

import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Discriminator extends Node<Discriminator> {

    private String propertyName;
    private Map<String, String> mapping;

    public String getPropertyName() {
        return propertyName;
    }

    public Discriminator setPropertyName(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    public Map<String, String> getMapping() {
        return mapping;
    }

    public Discriminator setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
        return this;
    }

    public Discriminator addMapping(String key, String value) {
        if (mapping == null) {
            mapping = new LinkedHashMap<>();
        }
        mapping.put(key, value);
        return this;
    }

    public Discriminator removeMapping(String key) {
        if (mapping != null) {
            mapping.remove(key);
        }
        return this;
    }

    @Override
    public Discriminator clone() {
        Discriminator clone = super.clone();
        if (mapping != null) {
            clone.setMapping(new LinkedHashMap<>(mapping));
        }
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        write(node, "propertyName", propertyName);
        write(node, "mapping", mapping);
        writeExtensions(node);
        return node;
    }
}
