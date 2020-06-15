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
import org.apache.dubbo.common.compiler.support.ClassUtils;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.apache.dubbo.common.constants.CommonConstants.DOT_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_CHAR_SEPERATOR;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

public class MetadataInfo implements Serializable {
    private String app;
    private String revision;
    private Map<String, ServiceInfo> services;

    public MetadataInfo(String app) {
        this.app = app;
        this.services = new HashMap<>();
    }

    public MetadataInfo(String app, String revision, Map<String, ServiceInfo> services) {
        this.app = app;
        this.revision = revision;
        this.services = services == null ? new HashMap<>() : services;
    }

    public void addService(ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return;
        }
        this.services.put(serviceInfo.getMatchKey(), serviceInfo);
    }

    public void removeService(ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return;
        }
        this.services.remove(serviceInfo.getMatchKey());
    }

    public void removeService(String key) {
        if (key == null) {
            return;
        }
        this.services.remove(key);
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getRevision() {
        if (revision != null) {
            return revision;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(app);
        for (Map.Entry<String, ServiceInfo> entry : services.entrySet()) {
            sb.append(entry.getValue().toDescString());
        }
        this.revision = RevisionResolver.calRevision(sb.toString());
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
        private static ExtensionLoader<MetadataParamsFilter> loader = ExtensionLoader.getExtensionLoader(MetadataParamsFilter.class);
        private String name;
        private String group;
        private String version;
        private String protocol;
        private String registry;
        private Map<String, String> params;

        private transient Map<String, Map<String, String>> methodParams;
        private transient String serviceKey;
        private transient String matchKey;

        public ServiceInfo() {
        }

        public ServiceInfo(URL url) {
            // FIXME, how to match registry.
            this(
                    url.getServiceInterface(),
                    url.getParameter(GROUP_KEY),
                    url.getParameter(VERSION_KEY),
                    url.getProtocol(),
                    "",
                    null
            );

            Map<String, String> params = new HashMap<>();
            List<MetadataParamsFilter> filters = loader.getActivateExtension(url, "params-filter");
            for (MetadataParamsFilter filter : filters) {
                String[] paramsIncluded = filter.include();
                if (ArrayUtils.isNotEmpty(paramsIncluded)) {
                    for (String p : paramsIncluded) {
                        String value = url.getParameter(p);
                        if (StringUtils.isNotEmpty(value) && params.get(p) == null) {
                            params.put(p, value);
                        }
                        String[] methods = url.getParameter(METHODS_KEY, (String[]) null);
                        if (methods != null) {
                            for (String method : methods) {
                                String mValue = url.getMethodParameterStrict(method, p);
                                if (StringUtils.isNotEmpty(mValue)) {
                                    params.put(method + DOT_SEPARATOR + p, mValue);
                                }
                            }
                        }
                    }
                }
            }
            this.params = params;
        }

        public ServiceInfo(String name, String group, String version, String protocol, String registry, Map<String, String> params) {
            this.name = name;
            this.group = group;
            this.version = version;
            this.protocol = protocol;
            this.registry = registry;
            this.params = params == null ? new HashMap<>() : params;

            this.serviceKey = URL.buildKey(name, group, version);
            this.matchKey = buildMatchKey();
        }

        public String getMatchKey() {
            if (matchKey != null) {
                return matchKey;
            }
            buildMatchKey();
            return matchKey;
        }

        private String buildMatchKey() {
            matchKey = getServiceKey();
            if (StringUtils.isNotEmpty(protocol)) {
                matchKey = getServiceKey() + GROUP_CHAR_SEPERATOR + protocol;
            }
            if (StringUtils.isNotEmpty(registry)) {
                matchKey = getServiceKey() + GROUP_CHAR_SEPERATOR + registry;
            }
            return matchKey;
        }

        public String getServiceKey() {
            if (serviceKey != null) {
                return serviceKey;
            }
            this.serviceKey = URL.buildKey(name, group, version);
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

        public String toDescString() {
            return this.getMatchKey() + getMethodSignaturesString() + getParams();
        }

        private String getMethodSignaturesString() {
            SortedSet<String> methodStrings = new TreeSet();

            Method[] methods = ClassUtils.forName(name).getMethods();
            for (Method method : methods) {
                methodStrings.add(method.toString());
            }
            return methodStrings.toString();
        }
    }
}
