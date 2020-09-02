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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.rpc.RpcContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

public class InstanceAddressURL extends URL {
    private ServiceInstance instance;
    private MetadataInfo metadataInfo;

    // cached numbers
    private volatile transient Map<String, Number> numbers;
    private volatile transient Map<String, Map<String, Number>> methodNumbers;

    public InstanceAddressURL() {
    }

    public InstanceAddressURL(
            ServiceInstance instance,
            MetadataInfo metadataInfo
    ) {
        this.instance = instance;
        this.metadataInfo = metadataInfo;
        this.host = instance.getHost();
        this.port = instance.getPort();
    }

    public ServiceInstance getInstance() {
        return instance;
    }

    public MetadataInfo getMetadataInfo() {
        return metadataInfo;
    }

    @Override
    public String getServiceInterface() {
        return RpcContext.getContext().getInterfaceName();
    }

    public String getGroup() {
        return RpcContext.getContext().getGroup();
    }

    public String getVersion() {
        return RpcContext.getContext().getVersion();
    }

    @Override
    public String getProtocol() {
        return RpcContext.getContext().getProtocol();
    }

    @Override
    public String getProtocolServiceKey() {
        return RpcContext.getContext().getProtocolServiceKey();
    }

    @Override
    public String getServiceKey() {
        return RpcContext.getContext().getServiceKey();
    }

    @Override
    public String getAddress() {
        return instance.getAddress();
    }

    @Override
    public String getPath() {
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(getProtocolServiceKey());
        return serviceInfo.getPath();
    }

    @Override
    public String getParameter(String key) {
        if (VERSION_KEY.equals(key)) {
            return getVersion();
        } else if (GROUP_KEY.equals(key)) {
            return getGroup();
        } else if (INTERFACE_KEY.equals(key)) {
            return getServiceInterface();
        } else if (REMOTE_APPLICATION_KEY.equals(key)) {
            return instance.getServiceName();
        }

        String protocolServiceKey = getProtocolServiceKey();
        if (protocolServiceKey == null) {
            return getInstanceParameter(key);
        }
        return getServiceParameter(protocolServiceKey, key);
    }

    @Override
    public String getServiceParameter(String service, String key) {
        String value = getInstanceParameter(key);
        if (StringUtils.isEmpty(value) && metadataInfo != null) {
            value = metadataInfo.getParameter(key, service);
        }
        return value;
    }

    /**
     * method parameter only exists in ServiceInfo
     *
     * @param method
     * @param key
     * @return
     */
    @Override
    public String getServiceMethodParameter(String protocolServiceKey, String method, String key) {
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(protocolServiceKey);
        String value = serviceInfo.getMethodParameter(method, key, null);
        if (StringUtils.isNotEmpty(value)) {
            return value;
        }
        return getParameter(key);
    }

    @Override
    public String getMethodParameter(String method, String key) {
        String protocolServiceKey = getProtocolServiceKey();
        if (protocolServiceKey == null) {
            return null;
        }
        return getServiceMethodParameter(protocolServiceKey, method, key);
    }

    /**
     * method parameter only exists in ServiceInfo
     *
     * @param method
     * @param key
     * @return
     */
    @Override
    public boolean hasServiceMethodParameter(String protocolServiceKey, String method, String key) {
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(protocolServiceKey);

        if (method == null) {
            String suffix = "." + key;
            for (String fullKey : getParameters().keySet()) {
                if (fullKey.endsWith(suffix)) {
                    return true;
                }
            }
            return false;
        }
        if (key == null) {
            String prefix = method + ".";
            for (String fullKey : getParameters().keySet()) {
                if (fullKey.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

        return serviceInfo.hasMethodParameter(method, key);
    }

    @Override
    public boolean hasMethodParameter(String method, String key) {
        String protocolServiceKey = getProtocolServiceKey();
        if (protocolServiceKey == null) {
            return false;
        }
        return hasServiceMethodParameter(protocolServiceKey, method, key);
    }

    /**
     * method parameter only exists in ServiceInfo
     *
     * @param method
     * @return
     */
    @Override
    public boolean hasServiceMethodParameter(String protocolServiceKey, String method) {
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(protocolServiceKey);
        return serviceInfo.hasMethodParameter(method);
    }

    @Override
    public boolean hasMethodParameter(String method) {
        String protocolServiceKey = getProtocolServiceKey();
        if (protocolServiceKey == null) {
            return false;
        }
        return hasServiceMethodParameter(protocolServiceKey, method);
    }

    /**
     * Avoid calling this method in RPC call.
     *
     * @return
     */
    @Override
    public Map<String, String> getServiceParameters(String protocolServiceKey) {
        Map<String, String> instanceParams = getInstanceMetadata();
        Map<String, String> metadataParams = (metadataInfo == null ? new HashMap<>() : metadataInfo.getParameters(protocolServiceKey));
        int i = instanceParams == null ? 0 : instanceParams.size();
        int j = metadataParams == null ? 0 : metadataParams.size();
        Map<String, String> params = new HashMap<>((int) ((i + j) / 0.75) + 1);
        if (instanceParams != null) {
            params.putAll(instanceParams);
        }
        if (metadataParams != null) {
            params.putAll(metadataParams);
        }
        return params;
    }

    @Override
    public Map<String, String> getParameters() {
        String protocolServiceKey = getProtocolServiceKey();
        if (protocolServiceKey == null) {
            return getInstance().getAllParams();
        }
        return getServiceParameters(protocolServiceKey);
    }

    @Override
    public URL addParameter(String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }

        getInstance().getExtendParams().put(key, value);
        return this;
    }

    @Override
    public URL addParameterIfAbsent(String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }

        getInstance().getExtendParams().putIfAbsent(key, value);
        return this;
    }

    public URL addServiceParameter(String protocolServiceKey, String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }

        getMetadataInfo().getServiceInfo(protocolServiceKey).addParameter(key, value);
        return this;
    }

    public URL addServiceParameterIfAbsent(String protocolServiceKey, String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }

        getMetadataInfo().getServiceInfo(protocolServiceKey).addParameterIfAbsent(key, value);
        return this;
    }

    public URL addConsumerParams(String protocolServiceKey, Map<String, String> params) {
        getMetadataInfo().getServiceInfo(protocolServiceKey).addConsumerParams(params);
        return this;
    }

    @Override
    protected Map<String, Number> getServiceNumbers(String protocolServiceKey) {
        return getServiceInfo(protocolServiceKey).getNumbers();
    }

    @Override
    protected Map<String, Number> getNumbers() {
        String protocolServiceKey = getProtocolServiceKey();
        if (protocolServiceKey == null) {
            if (numbers == null) { // concurrent initialization is tolerant
                numbers = new ConcurrentHashMap<>();
            }
            return numbers;
        }
        return getServiceNumbers(protocolServiceKey);
    }

    @Override
    protected Map<String, Map<String, Number>> getServiceMethodNumbers(String protocolServiceKey) {
        return getServiceInfo(protocolServiceKey).getMethodNumbers();
    }

    @Override
    protected Map<String, Map<String, Number>> getMethodNumbers() {
        String protocolServiceKey = getProtocolServiceKey();
        if (protocolServiceKey == null) {
            if (methodNumbers == null) { // concurrent initialization is tolerant
                methodNumbers = new ConcurrentHashMap<>();
            }
            return methodNumbers;
        }
        return getServiceMethodNumbers(protocolServiceKey);
    }

    private MetadataInfo.ServiceInfo getServiceInfo(String protocolServiceKey) {
        return metadataInfo.getServiceInfo(protocolServiceKey);
    }

    private String getInstanceParameter(String key) {
        String value = this.instance.getMetadata().get(key);
        if (StringUtils.isNotEmpty(value)) {
            return value;
        }
        return this.instance.getExtendParams().get(key);
    }

    private Map<String, String> getInstanceMetadata() {
        return this.instance.getMetadata();
    }

    @Override
    public boolean equals(Object obj) {
        // instance metadata equals
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof InstanceAddressURL)) {
            return false;
        }

        InstanceAddressURL that = (InstanceAddressURL) obj;

        return this.getInstance().equals(that.getInstance());
    }

    @Override
    public int hashCode() {
        return getInstance().hashCode();
    }

    public String getServiceString(String service) {
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(service);
        if (serviceInfo == null) {
            return instance.toString();
        }
        return instance.toString() + serviceInfo.toString();
    }

    @Override
    public String toString() {
        if (instance == null) {
            return "{}";
        }
        if (metadataInfo == null) {
            return instance.toString();
        }
        return instance.toString() + metadataInfo.toString();
    }
}
