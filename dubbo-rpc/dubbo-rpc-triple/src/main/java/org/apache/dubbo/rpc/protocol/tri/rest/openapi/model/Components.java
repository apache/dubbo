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
import java.util.TreeMap;

public final class Components extends Node<Components> {

    private Map<String, Schema> schemas;
    private Map<String, SecurityScheme> securitySchemes;

    public Map<String, Schema> getSchemas() {
        return schemas;
    }

    public Components setSchemas(Map<String, Schema> schemas) {
        this.schemas = schemas;
        return this;
    }

    public Components addSchema(String name, Schema schema) {
        if (schemas == null) {
            schemas = new TreeMap<>();
        }
        schemas.put(name, schema);
        return this;
    }

    public Components removeSchema(String name) {
        if (schemas != null) {
            schemas.remove(name);
        }
        return this;
    }

    public Map<String, SecurityScheme> getSecuritySchemes() {
        return securitySchemes;
    }

    public Components setSecuritySchemes(Map<String, SecurityScheme> securitySchemes) {
        this.securitySchemes = securitySchemes;
        return this;
    }

    public Components addSecurityScheme(String name, SecurityScheme securityScheme) {
        if (securitySchemes == null) {
            securitySchemes = new LinkedHashMap<>();
        }
        securitySchemes.put(name, securityScheme);
        return this;
    }

    public Components removeSecurityScheme(String name) {
        if (securitySchemes != null) {
            securitySchemes.remove(name);
        }
        return this;
    }

    @Override
    public Components clone() {
        Components clone = super.clone();
        clone.schemas = clone(schemas);
        clone.securitySchemes = clone(securitySchemes);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        write(node, "schemas", schemas, context);
        write(node, "securitySchemes", securitySchemes, context);
        writeExtensions(node);
        return node;
    }
}
