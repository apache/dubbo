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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ServerVariable extends Node<ServerVariable> {

    private List<String> enumeration;
    private String defaultValue;
    private String description;

    public List<String> getEnumeration() {
        return enumeration;
    }

    public ServerVariable setEnumeration(List<String> enumeration) {
        this.enumeration = enumeration;
        return this;
    }

    public ServerVariable addEnumeration(String value) {
        if (enumeration == null) {
            enumeration = new ArrayList<>();
        }
        enumeration.add(value);
        return this;
    }

    public ServerVariable removeEnumeration(String value) {
        if (enumeration != null) {
            enumeration.remove(value);
        }
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public ServerVariable setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ServerVariable setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public ServerVariable clone() {
        ServerVariable clone = super.clone();
        if (enumeration != null) {
            clone.enumeration = new ArrayList<>(enumeration);
        }
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        write(node, "enum", enumeration);
        write(node, "default", defaultValue);
        write(node, "description", description);
        writeExtensions(node);
        return node;
    }
}
