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
package org.apache.dubbo.metadata.rest;

import org.apache.dubbo.metadata.ParameterTypesComparator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The metadata class for {@link RequestMetadata HTTP(REST) request} and
 * its binding Dubbo service metadata
 *
 * @since 2.7.6
 */
public class ServiceRestMetadata implements Serializable {

    private static final long serialVersionUID = -4549723140727443569L;

    private String serviceInterface;

    private String version;

    private String group;

    private Set<RestMethodMetadata> meta;

    private Integer port;

    private boolean consumer;

    /**
     * make a distinction between mvc & resteasy
     */
    private Class codeStyle;

    private Map<PathMatcher, RestMethodMetadata> pathToServiceMap = new HashMap<>();
    private Map<String, Map<ParameterTypesComparator, RestMethodMetadata>> methodToServiceMap = new HashMap<>();

    public ServiceRestMetadata(String serviceInterface, String version, String group, boolean consumer) {
        this.serviceInterface = serviceInterface;
        this.version = version;
        this.group = group;
        this.consumer = consumer;
    }

    public ServiceRestMetadata() {
    }

    public ServiceRestMetadata(String serviceInterface, String version, String group) {
        this(serviceInterface, version, group, false);
    }

    public String getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(String serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Set<RestMethodMetadata> getMeta() {
        if (meta == null) {
            meta = new LinkedHashSet<>();
        }
        return meta;
    }

    public void setMeta(Set<RestMethodMetadata> meta) {
        this.meta = meta;
    }

    public void addRestMethodMetadata(RestMethodMetadata restMethodMetadata) {
        restMethodMetadata.setServiceRestMetadata(this);
        PathMatcher pathMather = new PathMatcher(restMethodMetadata.getRequest().getPath(),
            this.getVersion(), this.getGroup(), this.getPort());
        addPathToServiceMap(pathMather, restMethodMetadata);
        addMethodToServiceMap(restMethodMetadata);
        getMeta().add(restMethodMetadata);
    }

    public Map<PathMatcher, RestMethodMetadata> getPathToServiceMap() {
        return pathToServiceMap;
    }

    public void addPathToServiceMap(PathMatcher pathMather, RestMethodMetadata restMethodMetadata) {
        if (this.pathToServiceMap == null) {
            this.pathToServiceMap = new HashMap<>();
        }

        this.pathToServiceMap.put(pathMather, restMethodMetadata);


    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
        Map<PathMatcher, RestMethodMetadata> pathToServiceMap = getPathToServiceMap();
        for (PathMatcher pathMather : pathToServiceMap.keySet()) {
            pathMather.setPort(port);
        }
    }

    public boolean isConsumer() {
        return consumer;
    }

    public void setConsumer(boolean consumer) {
        this.consumer = consumer;
    }

    public Map<String, Map<ParameterTypesComparator, RestMethodMetadata>> getMethodToServiceMap() {
        return methodToServiceMap;
    }

    public void addMethodToServiceMap(RestMethodMetadata restMethodMetadata) {
        if (this.methodToServiceMap == null) {
            this.methodToServiceMap = new HashMap<>();
        }

        this.methodToServiceMap.computeIfAbsent(restMethodMetadata.getReflectMethod().getName(), k -> new HashMap<>())
            .put(ParameterTypesComparator.getInstance(restMethodMetadata.getReflectMethod().getParameterTypes()), restMethodMetadata);
    }

    public Class getCodeStyle() {
        return codeStyle;
    }

    public void setCodeStyle(Class codeStyle) {
        this.codeStyle = codeStyle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceRestMetadata)) {
            return false;
        }
        ServiceRestMetadata that = (ServiceRestMetadata) o;
        return Objects.equals(getServiceInterface(), that.getServiceInterface()) &&
            Objects.equals(getVersion(), that.getVersion()) &&
            Objects.equals(getGroup(), that.getGroup()) &&
            Objects.equals(getMeta(), that.getMeta()) &&
            Objects.equals(getPort(), that.getPort());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServiceInterface(), getVersion(), getGroup(), getMeta(), getPort());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ServiceRestMetadata{");
        sb.append("serviceInterface='").append(serviceInterface).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", group='").append(group).append('\'');
        sb.append(", meta=").append(meta);
        sb.append(", port=").append(port);
        sb.append('}');
        return sb.toString();
    }
}
