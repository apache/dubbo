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

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

/**
 * FIXME, replace RpcContext operations with explicitly defined APIs
 */
public class InstanceAddressURL extends URL {
    private ServiceInstance instance;
    private MetadataInfo metadataInfo;

    public InstanceAddressURL(
            ServiceInstance instance,
            MetadataInfo metadataInfo
    ) {
//        super()
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
        }

        String value = getInstanceMetadata().get(key);
        if (StringUtils.isEmpty(value) && metadataInfo != null) {
            value = metadataInfo.getParameter(key, RpcContext.getContext().getProtocolServiceKey());
        }
        return value;
    }

    @Override
    public String getParameter(String key, String defaultValue) {
        if (VERSION_KEY.equals(key)) {
            return getVersion();
        } else if (GROUP_KEY.equals(key)) {
            return getGroup();
        } else if (INTERFACE_KEY.equals(key)) {
            return getServiceInterface();
        }

        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public String getMethodParameter(String method, String key) {
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(getProtocolServiceKey());
        String value = serviceInfo.getMethodParameter(method, key, null);
        if (StringUtils.isNotEmpty(value)) {
            return value;
        }
        return getParameter(key);
    }

    @Override
    public boolean hasMethodParameter(String method, String key) {
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(getProtocolServiceKey());

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
    public boolean hasMethodParameter(String method) {
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(getProtocolServiceKey());
        return serviceInfo.hasMethodParameter(method);
    }

    /**
     * Avoid calling this method in RPC call.
     *
     * @return
     */
    @Override
    public Map<String, String> getParameters() {
        Map<String, String> instanceParams = getInstanceMetadata();
        Map<String, String> metadataParams = (metadataInfo == null ? new HashMap<>() : metadataInfo.getParameters(RpcContext.getContext().getProtocolServiceKey()));
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

    private Map<String, String> getInstanceMetadata() {
        return this.instance.getMetadata();
    }

    @Override
    public URL addParameter(String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }

        String protocolServiceKey = RpcContext.getContext().getProtocolServiceKey();
        getMetadataInfo().getServiceInfo(protocolServiceKey).addParameter(key, value);
        return this;
    }

    @Override
    public URL addParameterIfAbsent(String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }

        String protocolServiceKey = RpcContext.getContext().getProtocolServiceKey();
        getMetadataInfo().getServiceInfo(protocolServiceKey).addParameterIfAbsent(key, value);
        return this;
    }

    public URL addConsumerParams(Map<String, String> params) {
        String protocolServiceKey = RpcContext.getContext().getProtocolServiceKey();
        getMetadataInfo().getServiceInfo(protocolServiceKey).addConsumerParams(params);
        return this;
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

    @Override
    public String toString() {
        return super.toString();
    }
}
