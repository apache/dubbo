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

import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Context;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Helper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PathItem extends Node<PathItem> {

    private String ref;
    private String summary;
    private String description;
    private Map<HttpMethods, Operation> operations;
    private List<Server> servers;
    private List<Parameter> parameters;

    public String getRef() {
        return ref;
    }

    public PathItem setRef(String ref) {
        this.ref = ref;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public PathItem setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PathItem setDescription(String description) {
        this.description = description;
        return this;
    }

    public Map<HttpMethods, Operation> getOperations() {
        return operations;
    }

    public Operation getOperation(HttpMethods method) {
        return operations == null ? null : operations.get(method);
    }

    public PathItem setOperations(Map<HttpMethods, Operation> operations) {
        this.operations = operations;
        return this;
    }

    public PathItem addOperation(HttpMethods method, Operation operation) {
        if (operations == null) {
            operations = new LinkedHashMap<>();
        }
        operations.put(method, operation);
        return this;
    }

    public PathItem removeOperation(HttpMethods method) {
        if (operations != null) {
            operations.remove(method);
        }
        return this;
    }

    public List<Server> getServers() {
        return servers;
    }

    public PathItem setServers(List<Server> servers) {
        this.servers = servers;
        return this;
    }

    public PathItem addServer(Server server) {
        if (servers == null) {
            servers = new ArrayList<>();
        }
        servers.add(server);
        return this;
    }

    public PathItem removeServer(Server server) {
        if (servers != null) {
            servers.remove(server);
        }
        return this;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public PathItem setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    public PathItem addParameter(Parameter parameter) {
        List<Parameter> thisParameters = parameters;
        if (thisParameters == null) {
            parameters = thisParameters = new ArrayList<>();
        } else {
            for (int i = 0, size = thisParameters.size(); i < size; i++) {
                Parameter tParameter = thisParameters.get(i);
                if (tParameter.getName().equals(parameter.getName())) {
                    return this;
                }
            }
        }
        thisParameters.add(parameter);
        return this;
    }

    public PathItem removeParameter(Parameter parameter) {
        if (parameters != null) {
            parameters.remove(parameter);
        }
        return this;
    }

    @Override
    public PathItem clone() {
        PathItem clone = super.clone();
        clone.operations = clone(operations);
        clone.servers = clone(servers);
        clone.parameters = clone(parameters);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        if (ref != null) {
            write(node, "$ref", Helper.pathToRef(ref));
        } else if (operations != null) {
            write(node, "summary", summary);
            write(node, "description", description);
            for (Map.Entry<HttpMethods, Operation> entry : operations.entrySet()) {
                write(node, entry.getKey().name().toLowerCase(), entry.getValue(), context);
            }
            write(node, "servers", servers, context);
            write(node, "parameters", parameters, context);
        }
        writeExtensions(node);
        return node;
    }
}
