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

public class InstanceAddressURL extends URL {
    private ServiceInstance instance;
    private MetadataInfo metadataInfo;

    public InstanceAddressURL(
            ServiceInstance instance,
            MetadataInfo metadataInfo
    ) {
        this.instance = instance;
        this.metadataInfo = metadataInfo;
        this.setHost(instance.getHost());
        this.setPort(instance.getPort());
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
    public String getParameter(String key) {
        if (VERSION_KEY.equals(key)) {
            return getVersion();
        } else if (GROUP_KEY.equals(key)) {
            return getGroup();
        } else if (INTERFACE_KEY.equals(key)) {
            return getServiceInterface();
        }

        String value = getConsumerParameters().get(key);
        if (StringUtils.isEmpty(value)) {
            value = getInstanceMetadata().get(key);
        }
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
        String value = getMethodParameter(method, key);
        if (StringUtils.isNotEmpty(value)) {
            return value;
        }
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(getServiceKey());
        return serviceInfo.getMethodParameter(method, key, null);
    }

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

        params.putAll(getConsumerParameters());
        return params;
    }

    private Map<String, String> getInstanceMetadata() {
        return this.instance.getMetadata();
    }

    private Map<String, String> getConsumerParameters() {
        return RpcContext.getContext().getConsumerUrl().getParameters();
    }

    private String getConsumerParameter(String key) {
        return RpcContext.getContext().getConsumerUrl().getParameter(key);
    }

    private String getConsumerMethodParameter(String method, String key) {
        return RpcContext.getContext().getConsumerUrl().getMethodParameter(method, key);
    }

    @Override
    public URL addParameter(String key, String value) {
        throw new UnsupportedOperationException("");
    }

    @Override
    public boolean equals(Object obj) {
        // instance metadata equals
        // service metadata equals
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
