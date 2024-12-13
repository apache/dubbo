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
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Constants;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Context;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter.In;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Operation extends Node<Operation> {

    private Set<String> tags;
    private String summary;
    private String description;
    private ExternalDocs externalDocs;
    private String operationId;
    private List<Parameter> parameters;
    private RequestBody requestBody;
    private Map<String, ApiResponse> responses;
    private Boolean deprecated;
    private List<SecurityRequirement> security;
    private List<Server> servers;

    private String group;
    private String version;
    private HttpMethods httpMethod;
    private transient MethodMeta meta;

    public Set<String> getTags() {
        return tags;
    }

    public Operation setTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public Operation addTag(String tag) {
        if (tags == null) {
            tags = new LinkedHashSet<>();
        }
        tags.add(tag);
        return this;
    }

    public Operation removeTag(String tag) {
        if (tags != null) {
            tags.remove(tag);
        }
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public Operation setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Operation setDescription(String description) {
        this.description = description;
        return this;
    }

    public ExternalDocs getExternalDocs() {
        return externalDocs;
    }

    public Operation setExternalDocs(ExternalDocs externalDocs) {
        this.externalDocs = externalDocs;
        return this;
    }

    public String getOperationId() {
        return operationId;
    }

    public Operation setOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public Parameter getParameter(String name, In in) {
        if (parameters == null || name == null || in == null) {
            return null;
        }
        for (int i = 0, size = parameters.size(); i < size; i++) {
            Parameter parameter = parameters.get(i);
            if (name.equals(parameter.getName()) && in == parameter.getIn()) {
                return parameter;
            }
        }
        return null;
    }

    public Operation setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    public Operation addParameter(Parameter parameter) {
        if (parameters == null) {
            parameters = new ArrayList<>();
        }
        parameters.add(parameter);
        return this;
    }

    public Operation removeParameter(Parameter parameter) {
        if (parameters != null) {
            parameters.remove(parameter);
        }
        return this;
    }

    public RequestBody getRequestBody() {
        return requestBody;
    }

    public Operation setRequestBody(RequestBody requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public Map<String, ApiResponse> getResponses() {
        return responses;
    }

    public ApiResponse getResponse(String httpStatusCode) {
        return responses == null ? null : responses.get(httpStatusCode);
    }

    public ApiResponse getOrAddResponse(String httpStatusCode) {
        if (responses == null) {
            responses = new LinkedHashMap<>();
        }
        return responses.computeIfAbsent(httpStatusCode, k -> new ApiResponse());
    }

    public Operation setResponses(Map<String, ApiResponse> responses) {
        this.responses = responses;
        return this;
    }

    public Operation addResponse(String name, ApiResponse response) {
        if (responses == null) {
            responses = new LinkedHashMap<>();
        }
        responses.put(name, response);
        return this;
    }

    public Operation removeResponse(String name) {
        if (responses != null) {
            responses.remove(name);
        }
        return this;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public Operation setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    public List<SecurityRequirement> getSecurity() {
        return security;
    }

    public Operation setSecurity(List<SecurityRequirement> security) {
        this.security = security;
        return this;
    }

    public Operation addSecurity(SecurityRequirement security) {
        if (this.security == null) {
            this.security = new ArrayList<>();
        }
        this.security.add(security);
        return this;
    }

    public Operation removeSecurity(SecurityRequirement security) {
        if (this.security != null) {
            this.security.remove(security);
        }
        return this;
    }

    public List<Server> getServers() {
        return servers;
    }

    public Operation setServers(List<Server> servers) {
        this.servers = servers;
        return this;
    }

    public Operation addServer(Server server) {
        if (servers == null) {
            servers = new ArrayList<>();
        }
        servers.add(server);
        return this;
    }

    public Operation removeServer(Server server) {
        if (servers != null) {
            servers.remove(server);
        }
        return this;
    }

    public String getGroup() {
        return group;
    }

    public Operation setGroup(String group) {
        this.group = group;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Operation setVersion(String version) {
        this.version = version;
        return this;
    }

    public HttpMethods getHttpMethod() {
        return httpMethod;
    }

    public Operation setHttpMethod(HttpMethods httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public MethodMeta getMeta() {
        return meta;
    }

    public Operation setMeta(MethodMeta meta) {
        this.meta = meta;
        return this;
    }

    @Override
    public Operation clone() {
        Operation clone = super.clone();
        if (tags != null) {
            clone.tags = new LinkedHashSet<>(tags);
        }
        clone.externalDocs = clone(externalDocs);
        clone.parameters = clone(parameters);
        clone.requestBody = clone(requestBody);
        clone.responses = clone(responses);
        clone.security = clone(security);
        clone.servers = clone(servers);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        write(node, "tags", tags);
        write(node, "summary", summary);
        write(node, "description", description);
        write(node, "externalDocs", externalDocs, context);
        write(node, "operationId", operationId);
        write(node, "parameters", parameters, context);
        write(node, "requestBody", requestBody, context);
        write(node, "responses", responses, context);
        write(node, "deprecated", deprecated);
        write(node, "security", security, context);
        write(node, "servers", servers, context);
        writeExtensions(node);
        write(node, Constants.X_JAVA_CLASS, meta.getServiceMeta().getServiceInterface());
        write(node, Constants.X_JAVA_METHOD, meta.getMethod().getName());
        write(node, Constants.X_JAVA_METHOD_DESCRIPTOR, TypeUtils.getMethodDescriptor(meta));
        return node;
    }
}
