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
import org.apache.dubbo.metadata.swagger.model.info.Info;
import org.apache.dubbo.metadata.swagger.model.media.Schema;
import org.apache.dubbo.metadata.swagger.model.servers.Server;
import org.apache.dubbo.metadata.swagger.model.tags.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OpenAPI
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md"
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md"
 */
public class OpenAPI {
    private String openapi = "3.0.1";
    private Info info = null;
    private List<Server> servers = null;
    private List<Tag> tags = null;
    private Paths paths = null;
    private Components components = null;
    private Map<String, Object> extensions = null;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private String jsonSchemaDialect;

    public OpenAPI() {}

    public OpenAPI(SpecVersion specVersion) {
        this.specVersion = specVersion;
    }

    private SpecVersion specVersion = SpecVersion.V30;

    public SpecVersion getSpecVersion() {
        return this.specVersion;
    }

    public void setSpecVersion(SpecVersion specVersion) {
        this.specVersion = specVersion;
    }

    public OpenAPI specVersion(SpecVersion specVersion) {
        this.setSpecVersion(specVersion);
        return this;
    }

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Map<String, PathItem> webhooks = null;

    /**
     * returns the openapi property from a OpenAPI instance.
     *
     * @return String openapi
     **/
    public String getOpenapi() {
        return openapi;
    }

    public void setOpenapi(String openapi) {
        this.openapi = openapi;
    }

    public OpenAPI openapi(String openapi) {
        this.openapi = openapi;
        return this;
    }

    /**
     * returns the info property from a OpenAPI instance.
     *
     * @return Info info
     **/
    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public OpenAPI info(Info info) {
        this.info = info;
        return this;
    }

    /**
     * Servers defined in the API
     *
     * @return List&lt;Server&gt; servers
     **/
    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    public OpenAPI servers(List<Server> servers) {
        this.servers = servers;
        return this;
    }

    public OpenAPI addServersItem(Server serversItem) {
        if (this.servers == null) {
            this.servers = new ArrayList<>();
        }
        this.servers.add(serversItem);
        return this;
    }

    /**
     * returns the tags property from a OpenAPI instance.
     *
     * @return List&lt;Tag&gt; tags
     **/
    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public OpenAPI tags(List<Tag> tags) {
        this.tags = tags;
        return this;
    }

    public OpenAPI addTagsItem(Tag tagsItem) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(tagsItem);
        return this;
    }

    /**
     * returns the paths property from a OpenAPI instance.
     *
     * @return Paths paths
     **/
    public Paths getPaths() {
        return paths;
    }

    public void setPaths(Paths paths) {
        this.paths = paths;
    }

    public OpenAPI paths(Paths paths) {
        this.paths = paths;
        return this;
    }

    /**
     * returns the components property from a OpenAPI instance.
     *
     * @return Components components
     **/
    public Components getComponents() {
        return components;
    }

    public void setComponents(Components components) {
        this.components = components;
    }

    public OpenAPI components(Components components) {
        this.components = components;
        return this;
    }

    /*
     * helpers
     */

    public OpenAPI path(String name, PathItem path) {
        if (this.paths == null) {
            this.paths = new Paths();
        }

        this.paths.addPathItem(name, path);
        return this;
    }

    public OpenAPI schema(String name, Schema schema) {
        if (components == null) {
            this.components = new Components();
        }
        components.addSchemas(name, schema);
        return this;
    }

    /**
     * returns the webhooks property from a OpenAPI instance.
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     * @return Map&lt;String, PathItem&gt; webhooks
     **/
    @OpenAPI31
    public Map<String, PathItem> getWebhooks() {
        return webhooks;
    }

    @OpenAPI31
    public void setWebhooks(Map<String, PathItem> webhooks) {
        this.webhooks = webhooks;
    }

    @OpenAPI31
    public OpenAPI webhooks(Map<String, PathItem> webhooks) {
        this.webhooks = webhooks;
        return this;
    }

    @OpenAPI31
    public OpenAPI addWebhooks(String key, PathItem pathItem) {
        if (this.webhooks == null) {
            this.webhooks = new LinkedHashMap<>();
        }
        this.webhooks.put(key, pathItem);
        return this;
    }

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public String getJsonSchemaDialect() {
        return jsonSchemaDialect;
    }

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setJsonSchemaDialect(String jsonSchemaDialect) {
        this.jsonSchemaDialect = jsonSchemaDialect;
    }

    @OpenAPI31
    public OpenAPI jsonSchemaDialect(String jsonSchemaDialect) {
        this.jsonSchemaDialect = jsonSchemaDialect;
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
        OpenAPI openAPI = (OpenAPI) o;
        return Objects.equals(this.openapi, openAPI.openapi)
                && Objects.equals(this.info, openAPI.info)
                && Objects.equals(this.servers, openAPI.servers)
                && Objects.equals(this.tags, openAPI.tags)
                && Objects.equals(this.paths, openAPI.paths)
                && Objects.equals(this.components, openAPI.components)
                && Objects.equals(this.webhooks, openAPI.webhooks)
                && Objects.equals(this.extensions, openAPI.extensions)
                && Objects.equals(this.jsonSchemaDialect, openAPI.jsonSchemaDialect);
    }

    @Override
    public int hashCode() {
        return Objects.hash(openapi, info, servers, tags, paths, components, webhooks, extensions, jsonSchemaDialect);
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

    public OpenAPI extensions(Map<String, Object> extensions) {
        this.extensions = extensions;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class OpenAPI {\n");

        sb.append("    openapi: ").append(toIndentedString(openapi)).append("\n");
        sb.append("    info: ").append(toIndentedString(info)).append("\n");
        sb.append("    servers: ").append(toIndentedString(servers)).append("\n");
        sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
        sb.append("    paths: ").append(toIndentedString(paths)).append("\n");
        sb.append("    components: ").append(toIndentedString(components)).append("\n");
        if (specVersion == SpecVersion.V31)
            sb.append("    webhooks: ").append(toIndentedString(webhooks)).append("\n");
        if (specVersion == SpecVersion.V31)
            sb.append("    jsonSchemaDialect: ")
                    .append(toIndentedString(jsonSchemaDialect))
                    .append("\n");
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
