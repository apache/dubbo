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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MetadataInfo implements Serializable {
    private String app;
    private String revision;
    private Map<String, ServiceInfo> services;

    public MetadataInfo() {
    }

    public MetadataInfo(String app, String revision, Map<String, ServiceInfo> services) {
        this.app = app;
        this.revision = revision;
        this.services = services == null ? new HashMap<>() : services;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public Map<String, ServiceInfo> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceInfo> services) {
        this.services = services;
    }

    public ServiceInfo getServiceInfo(String serviceKey) {
        return services.get(serviceKey);
    }

    public String getParameter(String key, String serviceKey) {
        ServiceInfo serviceInfo = services.get(serviceKey);
        if (serviceInfo == null) {
            return null;
        }
        return serviceInfo.getParameter(key);
    }

    public Map<String, String> getParameters(String serviceKey) {
        ServiceInfo serviceInfo = services.get(serviceKey);
        if (serviceInfo == null) {
            return Collections.emptyMap();
        }
        return serviceInfo.getParams();
    }

    public static class ServiceInfo implements Serializable {
        private String name;
        private String group;
        private String version;
        private String protocol;
        private String registry;
        private Map<String, String> params;

        private transient Map<String, Map<String, String>> methodParams;
        private transient String serviceKey;

        public ServiceInfo() {
        }

        public ServiceInfo(String name, String group, String version, String protocol, String registry, Map<String, String> params) {
            this.name = name;
            this.group = group;
            this.version = version;
            this.protocol = protocol;
            this.registry = registry;
            this.params = params == null ? new HashMap<>() : params;

            this.serviceKey = URL.buildKey(name, group, version);
        }

        public String getServiceKey() {
            return serviceKey;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getRegistry() {
            return registry;
        }

        public void setRegistry(String registry) {
            this.registry = registry;
        }

        public Map<String, String> getParams() {
            if (params == null) {
                return Collections.emptyMap();
            }
            return params;
        }

        public void setParams(Map<String, String> params) {
            this.params = params;
        }

        public String getParameter(String key) {
            return params.get(key);
        }

        public String getMethodParameter(String method, String key, String defaultValue) {
            if (methodParams == null) {
                methodParams = URL.toMethodParameters(params);
            }

            Map<String, String> keyMap = methodParams.get(method);
            String value = null;
            if (keyMap != null) {
                value = keyMap.get(key);
            }
            return value == null ? defaultValue : value;
        }
    }
}
