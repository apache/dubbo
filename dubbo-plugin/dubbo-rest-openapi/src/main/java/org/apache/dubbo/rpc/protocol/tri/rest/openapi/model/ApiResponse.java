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

public final class ApiResponse extends Node<ApiResponse> {

    private String ref;
    private String description;
    private Map<String, Header> headers;
    private Map<String, MediaType> contents;

    public String getRef() {
        return ref;
    }

    public ApiResponse setRef(String ref) {
        this.ref = ref;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ApiResponse setDescription(String description) {
        this.description = description;
        return this;
    }

    public Map<String, Header> getHeaders() {
        return headers;
    }

    public Header getHeader(String name) {
        return headers == null ? null : headers.get(name);
    }

    public ApiResponse setHeaders(Map<String, Header> headers) {
        this.headers = headers;
        return this;
    }

    public ApiResponse addHeader(String name, Header header) {
        if (headers == null) {
            headers = new LinkedHashMap<>();
        }
        headers.put(name, header);
        return this;
    }

    public ApiResponse removeHeader(String name) {
        if (headers != null) {
            headers.remove(name);
        }
        return this;
    }

    public Map<String, MediaType> getContents() {
        return contents;
    }

    public MediaType getContent(String name) {
        return contents == null ? null : contents.get(name);
    }

    public MediaType getOrAddContent(String name) {
        if (contents == null) {
            contents = new LinkedHashMap<>();
        }
        return contents.computeIfAbsent(name, k -> new MediaType());
    }

    public ApiResponse setContents(Map<String, MediaType> contents) {
        this.contents = contents;
        return this;
    }

    public ApiResponse addContent(String name, MediaType content) {
        if (contents == null) {
            contents = new LinkedHashMap<>();
        }
        contents.put(name, content);
        return this;
    }

    public ApiResponse removeContent(String name) {
        if (contents != null) {
            contents.remove(name);
        }
        return this;
    }

    @Override
    public ApiResponse clone() {
        ApiResponse clone = super.clone();
        clone.contents = clone(contents);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        write(node, "description", description);
        write(node, "content", contents, context);
        writeExtensions(node);
        return node;
    }
}
