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

public final class RequestBody extends Node<RequestBody> {

    private String description;
    private Map<String, MediaType> contents;
    private boolean required;

    public String getDescription() {
        return description;
    }

    public RequestBody setDescription(String description) {
        this.description = description;
        return this;
    }

    public Map<String, MediaType> getContents() {
        return contents;
    }

    public MediaType getContent(String content) {
        return contents == null ? null : contents.get(content);
    }

    public MediaType getOrAddContent(String content) {
        if (contents == null) {
            contents = new LinkedHashMap<>();
        }
        return contents.computeIfAbsent(content, k -> new MediaType());
    }

    public RequestBody setContents(Map<String, MediaType> contents) {
        this.contents = contents;
        return this;
    }

    public RequestBody addContent(String name, MediaType content) {
        if (contents == null) {
            contents = new LinkedHashMap<>();
        }
        contents.put(name, content);
        return this;
    }

    public RequestBody removeContent(String name) {
        if (contents != null) {
            contents.remove(name);
        }
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public RequestBody setRequired(boolean required) {
        this.required = required;
        return this;
    }

    @Override
    public RequestBody clone() {
        RequestBody clone = super.clone();
        clone.contents = clone(contents);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        write(node, "description", description);
        write(node, "required", required);
        write(node, "content", contents, context);
        return node;
    }
}
