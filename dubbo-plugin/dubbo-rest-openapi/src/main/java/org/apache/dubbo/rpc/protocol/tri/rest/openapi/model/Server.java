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

public final class Server extends Node<Server> {

    private String url;
    private String description;
    private Map<String, ServerVariable> variables;

    public String getUrl() {
        return url;
    }

    public Server setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Server setDescription(String description) {
        this.description = description;
        return this;
    }

    public Map<String, ServerVariable> getVariables() {
        return variables;
    }

    public Server setVariables(Map<String, ServerVariable> variables) {
        this.variables = variables;
        return this;
    }

    public Server addVariable(String name, ServerVariable variable) {
        if (variables == null) {
            variables = new LinkedHashMap<>();
        }
        variables.put(name, variable);
        return this;
    }

    public Server removeVariable(String name) {
        if (variables != null) {
            variables.remove(name);
        }
        return this;
    }

    @Override
    public Server clone() {
        Server clone = super.clone();
        clone.variables = clone(variables);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        write(node, "url", url);
        write(node, "description", description);
        write(node, "variables", variables, context);
        writeExtensions(node);
        return node;
    }
}
