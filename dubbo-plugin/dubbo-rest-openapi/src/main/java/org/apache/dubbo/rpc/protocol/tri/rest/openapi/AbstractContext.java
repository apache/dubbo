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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi;

import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractContext {

    private final OpenAPI openAPI;
    private final SchemaResolver schemaResolver;
    private final ExtensionFactory extensionFactory;

    private Map<String, Object> attributes;

    AbstractContext(OpenAPI openAPI, SchemaResolver schemaResolver, ExtensionFactory extensionFactory) {
        this.openAPI = openAPI;
        this.schemaResolver = schemaResolver;
        this.extensionFactory = extensionFactory;
    }

    public final String getGroup() {
        return openAPI.getGroup();
    }

    public final OpenAPI getOpenAPI() {
        return openAPI;
    }

    public final SchemaResolver getSchemaResolver() {
        return schemaResolver;
    }

    public final ExtensionFactory getExtensionFactory() {
        return extensionFactory;
    }

    @SuppressWarnings("unchecked")
    public final <T> T getAttribute(String name) {
        return attributes == null ? null : (T) attributes.get(name);
    }

    @SuppressWarnings("unchecked")
    public final <T> T removeAttribute(String name) {
        return attributes == null ? null : (T) attributes.remove(name);
    }

    public final void setAttribute(String name, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(name, value);
    }
}
