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
package org.apache.dubbo.metadata.swagger.model;

import org.apache.dubbo.metadata.swagger.model.annotations.OpenAPI31;
import org.apache.dubbo.metadata.swagger.model.parameters.Parameter;
import org.apache.dubbo.metadata.swagger.model.parameters.RequestBody;
import org.apache.dubbo.metadata.swagger.model.responses.ApiResponses;
import org.apache.dubbo.metadata.swagger.model.servers.Server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Operation
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#operationObject"
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#operationObject"
 */
public class Operation {
    private List<String> tags = null;
    private String summary = null;
    private String description = null;
    private String operationId = null;
    private List<Parameter> parameters = null;
    private RequestBody requestBody = null;
    private ApiResponses responses = null;
    private Boolean deprecated = null;
    private List<Server> servers = null;
    private Map<String, Object> extensions = null;

    /**
     * returns the tags property from a Operation instance.
     *
     * @return List&lt;String&gt; tags
     **/
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Operation tags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public Operation addTagsItem(String tagsItem) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(tagsItem);
        return this;
    }

    /**
     * returns the summary property from a Operation instance.
     *
     * @return String summary
     **/
    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Operation summary(String summary) {
        this.summary = summary;
        return this;
    }

    /**
     * returns the description property from a Operation instance.
     *
     * @return String description
     **/
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Operation description(String description) {
        this.description = description;
        return this;
    }

    /**
     * returns the operationId property from a Operation instance.
     *
     * @return String operationId
     **/
    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public Operation operationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    /**
     * returns the parameters property from a Operation instance.
     *
     * @return List&lt;Parameter&gt; parameters
     **/
    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public Operation parameters(List<Parameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    public Operation addParametersItem(Parameter parametersItem) {
        if (this.parameters == null) {
            this.parameters = new ArrayList<>();
        }
        this.parameters.add(parametersItem);
        return this;
    }

    /**
     * returns the requestBody property from a Operation instance.
     *
     * @return RequestBody requestBody
     **/
    public RequestBody getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(RequestBody requestBody) {
        this.requestBody = requestBody;
    }

    public Operation requestBody(RequestBody requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    /**
     * returns the responses property from a Operation instance.
     *
     * @return ApiResponses responses
     **/
    public ApiResponses getResponses() {
        return responses;
    }

    public void setResponses(ApiResponses responses) {
        this.responses = responses;
    }

    public Operation responses(ApiResponses responses) {
        this.responses = responses;
        return this;
    }

    /**
     * returns the deprecated property from a Operation instance.
     *
     * @return Boolean deprecated
     **/
    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public Operation deprecated(Boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    /**
     * returns the servers property from a Operation instance.
     *
     * @return List&lt;Server&gt; servers
     **/
    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    public Operation servers(List<Server> servers) {
        this.servers = servers;
        return this;
    }

    public Operation addServersItem(Server serversItem) {
        if (this.servers == null) {
            this.servers = new ArrayList<>();
        }
        this.servers.add(serversItem);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Operation operation = (Operation) o;
        return Objects.equals(this.tags, operation.tags)
                && Objects.equals(this.summary, operation.summary)
                && Objects.equals(this.description, operation.description)
                && Objects.equals(this.operationId, operation.operationId)
                && Objects.equals(this.parameters, operation.parameters)
                && Objects.equals(this.requestBody, operation.requestBody)
                && Objects.equals(this.responses, operation.responses)
                && Objects.equals(this.deprecated, operation.deprecated)
                && Objects.equals(this.servers, operation.servers)
                && Objects.equals(this.extensions, operation.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                tags,
                summary,
                description,
                operationId,
                parameters,
                requestBody,
                responses,
                deprecated,
                servers,
                extensions);
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void addExtension(String name, Object value) {
        if (name == null || name.isEmpty() || !name.startsWith("x-")) {
            return;
        }
        if (this.extensions == null) {
            this.extensions = new LinkedHashMap<>();
        }
        this.extensions.put(name, value);
    }

    @OpenAPI31
    public void addExtension31(String name, Object value) {
        if (name != null && (name.startsWith("x-oas-") || name.startsWith("x-oai-"))) {
            return;
        }
        addExtension(name, value);
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    public Operation extensions(Map<String, Object> extensions) {
        this.extensions = extensions;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Operation {\n");

        sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
        sb.append("    summary: ").append(toIndentedString(summary)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    operationId: ").append(toIndentedString(operationId)).append("\n");
        sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
        sb.append("    requestBody: ").append(toIndentedString(requestBody)).append("\n");
        sb.append("    responses: ").append(toIndentedString(responses)).append("\n");
        sb.append("    deprecated: ").append(toIndentedString(deprecated)).append("\n");
        sb.append("    servers: ").append(toIndentedString(servers)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
