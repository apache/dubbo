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

import org.apache.dubbo.config.nested.OpenAPIConfig;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Constants;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public final class OpenAPI extends Node<OpenAPI> {

    private String openapi;
    private Info info;
    private List<Server> servers;
    private Map<String, PathItem> paths;
    private Components components;
    private List<SecurityRequirement> security;
    private List<Tag> tags;
    private ExternalDocs externalDocs;

    private String group;
    private int priority;

    private transient OpenAPIConfig globalConfig;
    private transient OpenAPIConfig config;
    private transient ServiceMeta meta;

    public String getOpenapi() {
        return openapi;
    }

    public OpenAPI setOpenapi(String openapi) {
        this.openapi = openapi;
        return this;
    }

    public Info getInfo() {
        return info;
    }

    public OpenAPI setInfo(Info info) {
        this.info = info;
        return this;
    }

    public List<Server> getServers() {
        return servers;
    }

    public OpenAPI setServers(List<Server> servers) {
        this.servers = servers;
        return this;
    }

    public OpenAPI addServer(Server server) {
        List<Server> thisServers = servers;
        if (thisServers == null) {
            servers = thisServers = new ArrayList<>();
        } else {
            for (int i = 0, size = thisServers.size(); i < size; i++) {
                if (thisServers.get(i).getUrl().equals(server.getUrl())) {
                    return this;
                }
            }
        }
        thisServers.add(server);
        return this;
    }

    public OpenAPI removeServer(Server server) {
        if (servers != null) {
            servers.remove(server);
        }
        return this;
    }

    public Map<String, PathItem> getPaths() {
        return paths;
    }

    public PathItem getPath(String path) {
        return paths == null ? null : paths.get(path);
    }

    public PathItem getOrAddPath(String path) {
        if (paths == null) {
            paths = new LinkedHashMap<>();
        }
        return paths.computeIfAbsent(path, k -> new PathItem());
    }

    public OpenAPI setPaths(Map<String, PathItem> paths) {
        this.paths = paths;
        return this;
    }

    public OpenAPI addPath(String path, PathItem pathItem) {
        if (paths == null) {
            paths = new LinkedHashMap<>();
        }
        paths.put(path, pathItem);
        return this;
    }

    public OpenAPI removePath(String path) {
        if (paths != null) {
            paths.remove(path);
        }
        return this;
    }

    public Components getComponents() {
        return components;
    }

    public OpenAPI setComponents(Components components) {
        this.components = components;
        return this;
    }

    public List<SecurityRequirement> getSecurity() {
        return security;
    }

    public OpenAPI setSecurity(List<SecurityRequirement> security) {
        this.security = security;
        return this;
    }

    public OpenAPI addSecurity(SecurityRequirement securityRequirement) {
        if (security == null) {
            security = new ArrayList<>();
        }
        security.add(securityRequirement);
        return this;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public OpenAPI setTags(List<Tag> tags) {
        this.tags = tags;
        return this;
    }

    public OpenAPI addTag(Tag tag) {
        List<Tag> thisTags = tags;
        if (thisTags == null) {
            tags = thisTags = new ArrayList<>();
        } else {
            for (int i = 0, size = thisTags.size(); i < size; i++) {
                if (thisTags.get(i).getName().equals(tag.getName())) {
                    return this;
                }
            }
        }
        thisTags.add(tag);
        return this;
    }

    public OpenAPI removeTag(Tag tag) {
        if (tags != null) {
            tags.remove(tag);
        }
        return this;
    }

    public ExternalDocs getExternalDocs() {
        return externalDocs;
    }

    public OpenAPI setExternalDocs(ExternalDocs externalDocs) {
        this.externalDocs = externalDocs;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public OpenAPI setGroup(String group) {
        this.group = group;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public OpenAPI setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public OpenAPIConfig getGlobalConfig() {
        return globalConfig;
    }

    public OpenAPI setGlobalConfig(OpenAPIConfig globalConfig) {
        this.globalConfig = globalConfig;
        return this;
    }

    public OpenAPIConfig getConfig() {
        return config;
    }

    public OpenAPI setConfig(OpenAPIConfig config) {
        this.config = config;
        return this;
    }

    public <T> T getConfigValue(Function<OpenAPIConfig, T> fn) {
        if (config != null) {
            T value = fn.apply(config);
            if (value != null) {
                return value;
            }
        }
        return globalConfig == null ? null : fn.apply(globalConfig);
    }

    public String getConfigSetting(String key) {
        return getConfigValue(config -> config == null ? null : config.getSetting(key));
    }

    public void walkOperations(Consumer<Operation> consumer) {
        Map<String, PathItem> paths = this.paths;
        if (paths == null) {
            return;
        }

        for (PathItem pathItem : paths.values()) {
            Map<HttpMethods, Operation> operations = pathItem.getOperations();
            if (operations != null) {
                for (Operation operation : operations.values()) {
                    consumer.accept(operation);
                }
            }
        }
    }

    public ServiceMeta getMeta() {
        return meta;
    }

    public OpenAPI setMeta(ServiceMeta meta) {
        this.meta = meta;
        return this;
    }

    @Override
    public OpenAPI clone() {
        OpenAPI clone = super.clone();
        clone.info = clone(info);
        clone.servers = clone(servers);
        clone.paths = clone(paths);
        clone.components = clone(components);
        clone.security = clone(security);
        clone.tags = clone(tags);
        clone.externalDocs = clone(externalDocs);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        node.put("openapi", openapi == null ? Constants.VERSION_30 : openapi);
        write(node, "info", info, context);
        write(node, "servers", servers, context);
        write(node, "paths", paths, context);
        write(node, "components", components, context);
        write(node, "security", security, context);
        write(node, "tags", tags, context);
        write(node, "externalDocs", externalDocs, context);
        writeExtensions(node);
        return node;
    }
}
