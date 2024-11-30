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

import java.util.Map;

public final class XML extends Node<XML> {

    private String name;
    private String namespace;
    private String prefix;
    private Boolean attribute;
    private Boolean wrapped;

    public String getName() {
        return name;
    }

    public XML setName(String name) {
        this.name = name;
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public XML setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getPrefix() {
        return prefix;
    }

    public XML setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public Boolean getAttribute() {
        return attribute;
    }

    public XML setAttribute(Boolean attribute) {
        this.attribute = attribute;
        return this;
    }

    public Boolean getWrapped() {
        return wrapped;
    }

    public XML setWrapped(Boolean wrapped) {
        this.wrapped = wrapped;
        return this;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        node.put("name", name);
        writeExtensions(node);
        return node;
    }
}
