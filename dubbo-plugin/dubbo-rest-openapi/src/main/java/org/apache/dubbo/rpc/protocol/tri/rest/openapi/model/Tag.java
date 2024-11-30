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

public final class Tag extends Node<Tag> {

    private String name;
    private String description;
    private ExternalDocs externalDocs;

    public String getName() {
        return name;
    }

    public Tag setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Tag setDescription(String description) {
        this.description = description;
        return this;
    }

    public ExternalDocs getExternalDocs() {
        return externalDocs;
    }

    public Tag setExternalDocs(ExternalDocs externalDocs) {
        this.externalDocs = externalDocs;
        return this;
    }

    @Override
    public Tag clone() {
        Tag clone = super.clone();
        clone.externalDocs = clone(externalDocs);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        node.put("name", name);
        node.put("description", description);
        write(node, "externalDocs", externalDocs, context);
        writeExtensions(node);
        return node;
    }
}
