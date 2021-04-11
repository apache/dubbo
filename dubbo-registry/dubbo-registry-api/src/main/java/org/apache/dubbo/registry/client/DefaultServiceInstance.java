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

import org.apache.dubbo.metadata.MetadataInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;

/**
 * The default implementation of {@link ServiceInstance}.
 *
 * @since 2.7.5
 */
public class DefaultServiceInstance implements ServiceInstance {

    private static final long serialVersionUID = 1149677083747278100L;

    private String id;

    private String serviceName;

    private String host;

    private Integer port;

    private boolean enabled;

    private boolean healthy;

    private Map<String, String> metadata = new HashMap<>();

    private transient String address;
    private transient MetadataInfo serviceMetadata;
    // used at runtime
    private transient Map<String, String> extendParams = new HashMap<>();

    public DefaultServiceInstance() {
    }

    public DefaultServiceInstance(String id, String serviceName, String host, Integer port) {
        if (port != null && port.intValue() < 1) {
            throw new IllegalArgumentException("The port must be greater than zero!");
        }
        this.id = id;
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
        this.enabled = true;
        this.healthy = true;
    }

    public DefaultServiceInstance(String serviceName, String host, Integer port) {
        this(host + ":" + port, serviceName, host, port);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getHost() {
        return host;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public String getAddress() {
        if (address == null) {
            address = getAddress(host, port);
        }
        return address;
    }

    private static String getAddress(String host, int port) {
        return port <= 0 ? host : host + ':' + port;
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
    public Map<String, String> getExtendParams() {
        return extendParams;
    }

    @Override
    public Map<String, String> getAllParams() {
        Map<String, String> allParams = new HashMap<>((int) ((metadata.size() + extendParams.size()) / 0.75f + 1));
        allParams.putAll(metadata);
        allParams.putAll(extendParams);
        return allParams;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public MetadataInfo getServiceMetadata() {
        return serviceMetadata;
    }

    public void setServiceMetadata(MetadataInfo serviceMetadata) {
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public InstanceAddressURL toURL() {
        return new InstanceAddressURL(this, serviceMetadata);
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
            if (entry.getKey().equals(REVISION_KEY)) {
                continue;
            }
            equals = equals && entry.getValue().equals(that.getMetadata().get(entry.getKey()));
        }

        return equals;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getServiceName(), getHost(), getPort());
        for (Map.Entry<String, String> entry : this.getMetadata().entrySet()) {
            if (entry.getKey().equals(REVISION_KEY)) {
                continue;
            }
            result = 31 * result + (entry.getValue() == null ? 0 : entry.getValue().hashCode());
        }
        return result;
    }

    @Override
    public String toString() {
        return "DefaultServiceInstance{" +
                "id='" + id + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", enabled=" + enabled +
                ", healthy=" + healthy +
                ", metadata=" + metadata +
                '}';
    }
}
