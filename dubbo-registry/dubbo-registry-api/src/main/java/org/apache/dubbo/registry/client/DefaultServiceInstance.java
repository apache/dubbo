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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.ENDPOINTS;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;

/**
 * The default implementation of {@link ServiceInstance}.
 *
 * @since 2.7.5
 */
public class DefaultServiceInstance implements ServiceInstance {

    private static final long serialVersionUID = 1149677083747278100L;

    private String rawAddress;

    private String serviceName;

    private String host;

    private int port;

    private boolean enabled = true;

    private boolean healthy = true;

    private Map<String, String> metadata = new HashMap<>();

    private transient String address;
    private transient MetadataInfo serviceMetadata;

    /**
     * used at runtime
     */
    private transient String registryCluster;

    /**
     * extendParams can be more flexible, but one single property uses less space
     */
    private transient Map<String, String> extendParams;
    private transient List<Endpoint> endpoints;
    private transient ApplicationModel applicationModel;
    private transient Map<String, InstanceAddressURL> instanceAddressURL = new ConcurrentHashMap<>();

    public DefaultServiceInstance() {
    }

    public DefaultServiceInstance(DefaultServiceInstance other) {
        this.serviceName = other.serviceName;
        this.host = other.host;
        this.port = other.port;
        this.enabled = other.enabled;
        this.healthy = other.healthy;
        this.serviceMetadata = other.serviceMetadata;
        this.registryCluster = other.registryCluster;
        this.address = null;
        this.metadata = new HashMap<>(other.metadata);
        this.applicationModel = other.applicationModel;
        this.extendParams = other.extendParams != null ? new HashMap<>(other.extendParams) : other.extendParams;
        this.endpoints = other.endpoints != null ? new ArrayList<>(other.endpoints) : other.endpoints;
    }

    public DefaultServiceInstance(String serviceName, String host, Integer port, ApplicationModel applicationModel) {
        if (port == null || port < 1) {
            throw new IllegalArgumentException("The port value is illegal, the value is " + port);
        }
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
        setApplicationModel(applicationModel);
    }

    public DefaultServiceInstance(String serviceName, ApplicationModel applicationModel) {
        this.serviceName = serviceName;
        setApplicationModel(applicationModel);
    }

    public void setRawAddress(String rawAddress) {
        this.rawAddress = rawAddress;
    }


    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getHost() {
        return host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getAddress() {
        if (address == null) {
            address = getAddress(host, port);
        }
        return address;
    }

    private static String getAddress(String host, Integer port) {
        return port != null && port <= 0 ? host : host + ':' + port;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public SortedMap<String, String> getSortedMetadata() {
        return new TreeMap<>(getMetadata());
    }

    @Override
    public String getRegistryCluster() {
        return registryCluster;
    }

    @Override
    public void setRegistryCluster(String registryCluster) {
        this.registryCluster = registryCluster;
    }

    @Override
    public Map<String, String> getExtendParams() {
        if (extendParams == null) {
            return Collections.emptyMap();
        }
        return extendParams;
    }

    @Override
    public String getExtendParam(String key) {
        if (extendParams == null) {
            return null;
        }
        return extendParams.get(key);
    }

    @Override
    public String putExtendParam(String key, String value) {
        if (extendParams == null) {
            extendParams = new HashMap<>();
        }
        return extendParams.put(key, value);
    }

    @Override
    public String putExtendParamIfAbsent(String key, String value) {
        if (extendParams == null) {
            extendParams = new HashMap<>();
        }
        return extendParams.putIfAbsent(key, value);
    }

    @Override
    public String removeExtendParam(String key) {
        if (extendParams == null) {
            return null;
        }
        return extendParams.remove(key);
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public List<Endpoint> getEndpoints() {
        if (endpoints == null) {
            endpoints = new LinkedList<>(JsonUtils.toJavaList(metadata.get(ENDPOINTS), Endpoint.class));
        }
        return endpoints;
    }

    public DefaultServiceInstance copyFrom(Endpoint endpoint) {
        DefaultServiceInstance copyOfInstance = new DefaultServiceInstance(this);
        copyOfInstance.setPort(endpoint.getPort());
        return copyOfInstance;
    }

    public DefaultServiceInstance copyFrom(int port) {
        DefaultServiceInstance copyOfInstance = new DefaultServiceInstance(this);
        copyOfInstance.setPort(port);
        return copyOfInstance;
    }

    @Override
    public Map<String, String> getAllParams() {
        if (extendParams == null) {
            return metadata;
        } else {
            Map<String, String> allParams = new HashMap<>((int) ((metadata.size() + extendParams.size()) / 0.75f + 1));
            allParams.putAll(metadata);
            allParams.putAll(extendParams);
            return allParams;
        }
    }

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    @Override
    @Transient
    public ApplicationModel getApplicationModel() {
        return applicationModel;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Override
    public MetadataInfo getServiceMetadata() {
        return serviceMetadata;
    }

    @Override
    public void setServiceMetadata(MetadataInfo serviceMetadata) {
        this.serviceMetadata = serviceMetadata;
        this.instanceAddressURL.clear();
    }

    @Override
    public InstanceAddressURL toURL(String protocol) {
        return instanceAddressURL.computeIfAbsent(protocol,
            key -> new InstanceAddressURL(this, serviceMetadata, protocol));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultServiceInstance)) {
            return false;
        }
        DefaultServiceInstance that = (DefaultServiceInstance) o;
        boolean equals = Objects.equals(getServiceName(), that.getServiceName()) &&
            Objects.equals(getHost(), that.getHost()) &&
            Objects.equals(getPort(), that.getPort());
        for (Map.Entry<String, String> entry : this.getMetadata().entrySet()) {
            if (entry.getKey().equals(EXPORTED_SERVICES_REVISION_PROPERTY_NAME)) {
                continue;
            }
            if (entry.getValue() == null) {
                equals = equals && (entry.getValue() == that.getMetadata().get(entry.getKey()));
            } else {
                equals = equals && entry.getValue().equals(that.getMetadata().get(entry.getKey()));
            }
        }

        return equals;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getServiceName(), getHost(), getPort());
        for (Map.Entry<String, String> entry : this.getMetadata().entrySet()) {
            if (entry.getKey().equals(EXPORTED_SERVICES_REVISION_PROPERTY_NAME)) {
                continue;
            }
            result = 31 * result + (entry.getValue() == null ? 0 : entry.getValue().hashCode());
        }
        return result;
    }

    @Override
    public String toString() {
        return rawAddress == null ? toFullString() : rawAddress;
    }

    public String toFullString() {
        return "DefaultServiceInstance{" +
            "serviceName='" + serviceName + '\'' +
            ", host='" + host + '\'' +
            ", port=" + port +
            ", enabled=" + enabled +
            ", healthy=" + healthy +
            ", metadata=" + metadata +
            '}';
    }

    public static class Endpoint {
        int port;
        String protocol;

        public Endpoint() {
        }

        public Endpoint(int port, String protocol) {
            this.port = port;
            this.protocol = protocol;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }
    }
}
