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
package org.apache.dubbo.metadata.swagger.models.links;

import org.apache.dubbo.metadata.swagger.models.annotations.OpenAPI31;
import org.apache.dubbo.metadata.swagger.models.headers.Header;
import org.apache.dubbo.metadata.swagger.models.servers.Server;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Link
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#linkObject"
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#linkObject"
 */
public class Link {
    private String operationRef = null;
    private String operationId = null;
    private Map<String, String> parameters = null;
    private Object requestBody = null;
    /**
     * @deprecated as it's not part of OpenAPI specification
     */
    @Deprecated
    private Map<String, Header> headers = null;

    private String description = null;
    private String $ref = null;
    private Map<String, Object> extensions = null;
    private Server server;

    /**
     * returns the server property from a Link instance.
     *
     * @return Server server
     **/
    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Link server(Server server) {
        this.setServer(server);
        return this;
    }

    /**
     * returns the operationRef property from a Link instance.
     *
     * @return String operationRef
     **/
    public String getOperationRef() {
        return operationRef;
    }

    public void setOperationRef(String operationRef) {
        this.operationRef = operationRef;
    }

    public Link operationRef(String operationRef) {
        this.operationRef = operationRef;
        return this;
    }

    /**
     * returns the requestBody property from a Link instance.
     *
     * @return Object requestBody
     **/
    public Object getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(Object requestBody) {
        this.requestBody = requestBody;
    }

    public Link requestBody(Object requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public Link operationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    /**
     * returns the parameters property from a Link instance.
     *
     * @return LinkParameters parameters
     **/
    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Deprecated
    public Link parameters(String name, String parameter) {
        return this.addParameter(name, parameter);
    }

    public Link addParameter(String name, String parameter) {
        if (this.parameters == null) {
            this.parameters = new LinkedHashMap<>();
        }
        this.parameters.put(name, parameter);
        return this;
    }

    /**
     * @deprecated as it's not part of OpenAPI specification
     */
    @Deprecated
    public Map<String, Header> getHeaders() {
        return headers;
    }

    /**
     * @deprecated as it's not part of OpenAPI specification
     */
    @Deprecated
    public void setHeaders(Map<String, Header> headers) {
        this.headers = headers;
    }

    /**
     * @deprecated as it's not part of OpenAPI specification
     */
    @Deprecated
    public Link headers(Map<String, Header> headers) {
        this.headers = headers;
        return this;
    }

    /**
     * @deprecated as it's not part of OpenAPI specification
     */
    @Deprecated
    public Link addHeaderObject(String name, Header header) {
        if (this.headers == null) {
            headers = new LinkedHashMap<>();
        }
        headers.put(name, header);
        return this;
    }

    /**
     * returns the description property from a Link instance.
     *
     * @return String description
     **/
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Link description(String description) {
        this.description = description;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Link)) {
            return false;
        }

        Link link = (Link) o;

        if (operationRef != null ? !operationRef.equals(link.operationRef) : link.operationRef != null) {
            return false;
        }
        if (operationId != null ? !operationId.equals(link.operationId) : link.operationId != null) {
            return false;
        }
        if (parameters != null ? !parameters.equals(link.parameters) : link.parameters != null) {
            return false;
        }
        if (requestBody != null ? !requestBody.equals(link.requestBody) : link.requestBody != null) {
            return false;
        }
        if (headers != null ? !headers.equals(link.headers) : link.headers != null) {
            return false;
        }
        if (description != null ? !description.equals(link.description) : link.description != null) {
            return false;
        }
        if ($ref != null ? !$ref.equals(link.$ref) : link.$ref != null) {
            return false;
        }
        if (extensions != null ? !extensions.equals(link.extensions) : link.extensions != null) {
            return false;
        }
        return server != null ? server.equals(link.server) : link.server == null;
    }

    @Override
    public int hashCode() {
        int result = operationRef != null ? operationRef.hashCode() : 0;
        result = 31 * result + (operationId != null ? operationId.hashCode() : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        result = 31 * result + (requestBody != null ? requestBody.hashCode() : 0);
        result = 31 * result + (headers != null ? headers.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + ($ref != null ? $ref.hashCode() : 0);
        result = 31 * result + (extensions != null ? extensions.hashCode() : 0);
        result = 31 * result + (server != null ? server.hashCode() : 0);
        return result;
    }

    public String get$ref() {
        return $ref;
    }

    public void set$ref(String $ref) {
        if ($ref != null && ($ref.indexOf('.') == -1 && $ref.indexOf('/') == -1)) {
            $ref = "#/components/links/" + $ref;
        }
        this.$ref = $ref;
    }

    public Link $ref(String $ref) {
        set$ref($ref);
        return this;
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

    public Link extensions(Map<String, Object> extensions) {
        this.extensions = extensions;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Link {\n");

        sb.append("    operationRef: ").append(toIndentedString(operationRef)).append("\n");
        sb.append("    operationId: ").append(toIndentedString(operationId)).append("\n");
        sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
        sb.append("    requestBody: ").append(toIndentedString(requestBody)).append("\n");
        sb.append("    headers: ").append(toIndentedString(headers)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    $ref: ").append(toIndentedString($ref)).append("\n");
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
