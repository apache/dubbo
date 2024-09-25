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
import org.apache.dubbo.metadata.swagger.model.headers.Header;
import org.apache.dubbo.metadata.swagger.model.media.Schema;
import org.apache.dubbo.metadata.swagger.model.parameters.Parameter;
import org.apache.dubbo.metadata.swagger.model.parameters.RequestBody;
import org.apache.dubbo.metadata.swagger.model.responses.ApiResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Components
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#componentsObject"
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#componentsObject"
 */
public class Components {

    /**
     * @since 2.1.6
     */
    public static final String COMPONENTS_SCHEMAS_REF = "#/components/schemas/";

    private Map<String, Schema> schemas = null;
    private Map<String, ApiResponse> responses = null;
    private Map<String, Parameter> parameters = null;
    private Map<String, RequestBody> requestBodies = null;
    private Map<String, Header> headers = null;
    private Map<String, Object> extensions = null;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Map<String, PathItem> pathItems;

    /**
     * returns the schemas property from a Components instance.
     *
     * @return Map&lt;String, Schema&gt; schemas
     **/
    public Map<String, Schema> getSchemas() {
        return schemas;
    }

    public void setSchemas(Map<String, Schema> schemas) {
        this.schemas = schemas;
    }

    public Components schemas(Map<String, Schema> schemas) {
        this.schemas = schemas;
        return this;
    }

    public Components addSchemas(String key, Schema schemasItem) {
        if (this.schemas == null) {
            this.schemas = new LinkedHashMap<>();
        }
        this.schemas.put(key, schemasItem);
        return this;
    }

    /**
     * returns the responses property from a Components instance.
     *
     * @return Map&lt;String, ApiResponse&gt; responses
     **/
    public Map<String, ApiResponse> getResponses() {
        return responses;
    }

    public void setResponses(Map<String, ApiResponse> responses) {
        this.responses = responses;
    }

    public Components responses(Map<String, ApiResponse> responses) {
        this.responses = responses;
        return this;
    }

    public Components addResponses(String key, ApiResponse responsesItem) {
        if (this.responses == null) {
            this.responses = new LinkedHashMap<>();
        }
        this.responses.put(key, responsesItem);
        return this;
    }

    /**
     * returns the parameters property from a Components instance.
     *
     * @return Map&lt;String, Parameter&gt; parameters
     **/
    public Map<String, Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Parameter> parameters) {
        this.parameters = parameters;
    }

    public Components parameters(Map<String, Parameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    public Components addParameters(String key, Parameter parametersItem) {
        if (this.parameters == null) {
            this.parameters = new LinkedHashMap<>();
        }
        this.parameters.put(key, parametersItem);
        return this;
    }
    /**
     * returns the requestBodies property from a Components instance.
     *
     * @return Map&lt;String, RequestBody&gt; requestBodies
     **/
    public Map<String, RequestBody> getRequestBodies() {
        return requestBodies;
    }

    public void setRequestBodies(Map<String, RequestBody> requestBodies) {
        this.requestBodies = requestBodies;
    }

    public Components requestBodies(Map<String, RequestBody> requestBodies) {
        this.requestBodies = requestBodies;
        return this;
    }

    public Components addRequestBodies(String key, RequestBody requestBodiesItem) {
        if (this.requestBodies == null) {
            this.requestBodies = new LinkedHashMap<>();
        }
        this.requestBodies.put(key, requestBodiesItem);
        return this;
    }

    /**
     * returns the headers property from a Components instance.
     *
     * @return Map&lt;String, Header&gt; headers
     **/
    public Map<String, Header> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Header> headers) {
        this.headers = headers;
    }

    public Components headers(Map<String, Header> headers) {
        this.headers = headers;
        return this;
    }

    public Components addHeaders(String key, Header headersItem) {
        if (this.headers == null) {
            this.headers = new LinkedHashMap<>();
        }
        this.headers.put(key, headersItem);
        return this;
    }
    /**
     * returns the path items property from a Components instance.
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     * @return Map&lt;String, PathItem&gt; pathItems
     **/
    @OpenAPI31
    public Map<String, PathItem> getPathItems() {
        return pathItems;
    }

    @OpenAPI31
    public void setPathItems(Map<String, PathItem> pathItems) {
        this.pathItems = pathItems;
    }

    @OpenAPI31
    public Components pathItems(Map<String, PathItem> pathItems) {
        this.pathItems = pathItems;
        return this;
    }

    @OpenAPI31
    public Components addPathItem(String key, PathItem pathItem) {
        if (this.pathItems == null) {
            this.pathItems = new LinkedHashMap<>();
        }
        this.pathItems.put(key, pathItem);
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
        Components components = (Components) o;
        return Objects.equals(this.schemas, components.schemas)
                && Objects.equals(this.responses, components.responses)
                && Objects.equals(this.parameters, components.parameters)
                && Objects.equals(this.requestBodies, components.requestBodies)
                && Objects.equals(this.headers, components.headers)
                && Objects.equals(this.extensions, components.extensions)
                && Objects.equals(this.pathItems, components.pathItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemas, responses, parameters, requestBodies, headers, extensions, pathItems);
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

    public Components extensions(Map<String, Object> extensions) {
        this.extensions = extensions;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Components {\n");

        sb.append("    schemas: ").append(toIndentedString(schemas)).append("\n");
        sb.append("    responses: ").append(toIndentedString(responses)).append("\n");
        sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
        sb.append("    requestBodies: ").append(toIndentedString(requestBodies)).append("\n");
        sb.append("    headers: ").append(toIndentedString(headers)).append("\n");
        sb.append("    pathItems: ").append(toIndentedString(pathItems)).append("\n");
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
