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
import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ServiceModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;
import static org.apache.dubbo.common.utils.StringUtils.isEquals;

public class InstanceAddressURL extends URL {
    private final ServiceInstance instance;
    private final MetadataInfo metadataInfo;

    // cached numbers
    private volatile transient Map<String, Number> numbers;
    private volatile transient Map<String, Map<String, Number>> methodNumbers;
    private volatile transient Set<String> providerFirstParams;
    // one instance address url serves only one protocol.
    private final transient String protocol;

    protected InstanceAddressURL() {
        this(null, null, null);
    }

    public InstanceAddressURL(
        ServiceInstance instance,
        MetadataInfo metadataInfo
    ) {
        this.instance = instance;
        this.metadataInfo = metadataInfo;
        this.protocol = DUBBO;
    }

    public InstanceAddressURL(
        ServiceInstance instance,
        MetadataInfo metadataInfo,
        String protocol
    ) {
        this.instance = instance;
        this.metadataInfo = metadataInfo;
        this.protocol = protocol;
    }

    public ServiceInstance getInstance() {
        return instance;
    }

    public MetadataInfo getMetadataInfo() {
        return metadataInfo;
    }

    @Override
    public String getServiceInterface() {
        return RpcContext.getServiceContext().getInterfaceName();
    }

    @Override
    public String getGroup() {
        return RpcContext.getServiceContext().getGroup();
    }

    @Override
    public String getVersion() {
        return RpcContext.getServiceContext().getVersion();
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getProtocolServiceKey() {
        // if protocol is not specified on consumer side, return serviceKey.
        URL consumerURL = RpcContext.getServiceContext().getConsumerUrl();
        String consumerProtocol = consumerURL == null ? null : consumerURL.getProtocol();
        if (isEquals(consumerProtocol, CONSUMER)) {
            return RpcContext.getServiceContext().getServiceKey();
        }
        // if protocol is specified on consumer side, return accurate protocolServiceKey
        else {
            return RpcContext.getServiceContext().getProtocolServiceKey();
        }
    }

    @Override
    public String getServiceKey() {
        return RpcContext.getServiceContext().getServiceKey();
    }

    @Override
    public String getAddress() {
        return instance.getAddress();
    }

    @Override
    public String getHost() {
        return instance.getHost();
    }

    @Override
    public int getPort() {
        return instance.getPort();
    }

    @Override
    public String getIp() {
        return instance.getHost();
    }

    @Override
    public String getRemoteApplication() {
        return instance.getServiceName();
    }

    @Override
    public String getSide() {
        return CONSUMER_SIDE;
    }

    @Override
    public String getPath() {
        MetadataInfo.ServiceInfo serviceInfo = null;
        String protocolServiceKey = getProtocolServiceKey();
        if (StringUtils.isNotEmpty(protocolServiceKey)) {
            serviceInfo = getServiceInfo(protocolServiceKey);
        }
        if (serviceInfo == null) {
            return getServiceInterface();
        }
        return serviceInfo.getPath();
    }

    @Override
    public String getParameter(final String key) {
        if (VERSION_KEY.equals(key)) {
            return getVersion();
        } else if (GROUP_KEY.equals(key)) {
            return getGroup();
        } else if (INTERFACE_KEY.equals(key)) {
            return getServiceInterface();
        } else if (REMOTE_APPLICATION_KEY.equals(key)) {
            return instance.getServiceName();
        } else if (SIDE_KEY.equals(key)) {
            return getSide();
        }
    
        String executeResult = (String) executeWithConsumerUrl(new ExecuteWithConsumerUrlFunction(ParameterMethodOnConsumerUrlEnum.GetParameter,
                null, null, key, consumerParamFirst(true, providerFirstParams, key), true));
        if (executeResult != null) {
            return executeResult;
        }

        String protocolServiceKey = getProtocolServiceKey();
        if (isEmpty(protocolServiceKey)) {
            return getInstanceParameter(key);
        }
        return getServiceParameter(protocolServiceKey, key);
    }

    @Override
    public String getServiceParameter(final String service, final String key) {
        String executeResult = (String) executeWithConsumerUrl(new ExecuteWithConsumerUrlFunction(ParameterMethodOnConsumerUrlEnum.GetServiceParameter,
                service, null, key, consumerParamFirst(true, providerFirstParams, key), true));
        if (executeResult != null) {
            return executeResult;
        }

        if (metadataInfo != null) {
            String value = metadataInfo.getParameter(key, service);
            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
        }

        return getInstanceParameter(key);
    }

    /**
     * method parameter only exists in ServiceInfo
     *
     * @param method
     * @param key
     * @return
     */
    @Override
    public String getServiceMethodParameter(final String protocolServiceKey, final String method, final String key) {
        String executeResult = (String) executeWithConsumerUrl(new ExecuteWithConsumerUrlFunction(ParameterMethodOnConsumerUrlEnum.GetServiceMethodParameter,
                protocolServiceKey, method, key, consumerParamFirst(true, providerFirstParams, key), true));
        if (executeResult != null) {
            return executeResult;
        }

        MetadataInfo.ServiceInfo serviceInfo = getServiceInfo(protocolServiceKey);
        if (null == serviceInfo) {
            return getParameter(key);
        }

        String value = serviceInfo.getMethodParameter(method, key, null);
        if (StringUtils.isNotEmpty(value)) {
            return value;
        }
        return getParameter(key);
    }

    @Override
    public String getMethodParameter(final String method, final String key) {
        String executeResult = (String) executeWithConsumerUrl(new ExecuteWithConsumerUrlFunction(ParameterMethodOnConsumerUrlEnum.GetMethodParameter,
                null, method, key, consumerParamFirst(true, providerFirstParams, key), true));
        if (executeResult != null) {
            return executeResult;
        }

        String protocolServiceKey = getProtocolServiceKey();
        if (isEmpty(protocolServiceKey)) {
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
    public boolean hasServiceMethodParameter(final String protocolServiceKey, final String method, final String key) {
        Boolean executeResult = (Boolean) executeWithConsumerUrl(new ExecuteWithConsumerUrlFunction(ParameterMethodOnConsumerUrlEnum.HasServiceMethodParameter,
                protocolServiceKey, method, key, consumerParamFirst(true, providerFirstParams, key), false));
        if (executeResult) {
            return true;
        }
        
        MetadataInfo.ServiceInfo serviceInfo = getServiceInfo(protocolServiceKey);

        if (isEmpty(method)) {
            String suffix = "." + key;
            for (String fullKey : getParameters().keySet()) {
                if (fullKey.endsWith(suffix)) {
                    return true;
                }
            }
            return false;
        }
        if (isEmpty(key)) {
            String prefix = method + ".";
            for (String fullKey : getParameters().keySet()) {
                if (fullKey.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

        if (null == serviceInfo) {
            return false;
        }

        return serviceInfo.hasMethodParameter(method, key);
    }

    @Override
    public boolean hasMethodParameter(final String method, final String key) {
        Boolean executeResult = (Boolean) executeWithConsumerUrl(new ExecuteWithConsumerUrlFunction(ParameterMethodOnConsumerUrlEnum.HasMethodParameter,
                null, method, key, consumerParamFirst(true, providerFirstParams, key), false));
        if (executeResult) {
            return true;
        }
        
        String protocolServiceKey = getProtocolServiceKey();
        if (isEmpty(protocolServiceKey)) {
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
    public boolean hasServiceMethodParameter(final String protocolServiceKey, final String method) {
        Boolean executeResult = (Boolean) executeWithConsumerUrl(new ExecuteWithConsumerUrlFunction(ParameterMethodOnConsumerUrlEnum.HasServiceMethodParameter,
                protocolServiceKey, method, null, consumerParamFirst(true, providerFirstParams, null), false));
        if (executeResult) {
            return true;
        }

        MetadataInfo.ServiceInfo serviceInfo = getServiceInfo(protocolServiceKey);
        if (null == serviceInfo) {
            return false;
        }
        return serviceInfo.hasMethodParameter(method);
    }

    @Override
    public boolean hasMethodParameter(final String method) {
        Boolean executeResult = (Boolean) executeWithConsumerUrl(new ExecuteWithConsumerUrlFunction(ParameterMethodOnConsumerUrlEnum.HasMethodParameter,
                null, method, null, consumerParamFirst(true, providerFirstParams, null), false));
        if (executeResult) {
            return true;
        }
        
        String protocolServiceKey = getProtocolServiceKey();
        if (isEmpty(protocolServiceKey)) {
            return false;
        }
        return hasServiceMethodParameter(protocolServiceKey, method);
    }
    
    public Object executeWithConsumerUrl(final Function<URL, Object> function) {
        return function.apply(RpcContext.getServiceContext().getConsumerUrl());
    }
    
    public class ExecuteWithConsumerUrlFunction implements Function<URL, Object> {
        private final ParameterMethodOnConsumerUrlEnum parameterMethodOnConsumerUrlEnum;
        private final String service;
        private final String method;
        private final String key;
        private final Boolean consumerParamFirst;
        private final Boolean getOnConsumerUrl;
    
        public ExecuteWithConsumerUrlFunction(final ParameterMethodOnConsumerUrlEnum parameterMethodOnConsumerUrlEnum, final String service, 
                                              final String method, final String key, final Boolean consumerParamFirst, final Boolean getOnConsumerUrl) {
            this.parameterMethodOnConsumerUrlEnum = parameterMethodOnConsumerUrlEnum;
            this.service = service;
            this.method = method;
            this.key = key;
            this.consumerParamFirst = consumerParamFirst;
            this.getOnConsumerUrl = getOnConsumerUrl;
        }
        
        @Override
        public Object apply(final URL url) {
            if (url == null) {
                return resultOnGetOnConsumerUrl(getOnConsumerUrl);
            }
            if (!consumerParamFirst) {
                return resultOnGetOnConsumerUrl(getOnConsumerUrl);
            }
            if (parameterMethodOnConsumerUrlEnum == ParameterMethodOnConsumerUrlEnum.GetParameter) {
                return cleanEmpty(url.getParameter(key));
            }
            if (parameterMethodOnConsumerUrlEnum == ParameterMethodOnConsumerUrlEnum.GetServiceParameter) {
                return cleanEmpty(url.getServiceParameter(service, key));
            }
            if (parameterMethodOnConsumerUrlEnum == ParameterMethodOnConsumerUrlEnum.GetServiceMethodParameter) {
                return cleanEmpty(url.getServiceMethodParameter(service, method, key));
            }
            if (parameterMethodOnConsumerUrlEnum == ParameterMethodOnConsumerUrlEnum.HasServiceMethodParameter) {
                return url.hasServiceMethodParameter(service, method, key);
            }
            if (parameterMethodOnConsumerUrlEnum == ParameterMethodOnConsumerUrlEnum.GetMethodParameter) {
                return cleanEmpty(url.getMethodParameter(method, key));
            }
            if (parameterMethodOnConsumerUrlEnum == ParameterMethodOnConsumerUrlEnum.HasMethodParameter) {
                return url.hasMethodParameter(method, key);
            }
            return resultOnGetOnConsumerUrl(getOnConsumerUrl);
        }
        
        private String cleanEmpty(final String value) {
            return StringUtils.isEmpty(value) ? null : value;
        }
        
        private Object resultOnGetOnConsumerUrl(final Boolean getOnConsumerUrl) {
            return getOnConsumerUrl ? null : Boolean.FALSE;
        }
    }
    
    private Boolean consumerParamFirst(final boolean checkConsumerParamFirst, final Set<String> providerFirstParams, final String key) {
        return checkConsumerParamFirst && !providerFirstParam(providerFirstParams, key);
    }
    
    private Boolean providerFirstParam(final Set<String> providerFirstParams, final String key) {
        return CollectionUtils.isNotEmpty(providerFirstParams) && providerFirstParams.contains(key);
    }
    
    public enum ParameterMethodOnConsumerUrlEnum {
        GetParameter,
        GetServiceParameter,
        GetServiceMethodParameter,
        HasServiceMethodParameter,
        GetMethodParameter,
        HasMethodParameter
    }

    /**
     * Avoid calling this method in RPC call.
     *
     * @return
     */
    @Override
    public Map<String, String> getServiceParameters(String protocolServiceKey) {
        Map<String, String> instanceParams = getInstance().getAllParams();
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

        URL consumerUrl = RpcContext.getServiceContext().getConsumerUrl();
        if (consumerUrl != null) {
            Map<String, String> consumerParams = new HashMap<>(consumerUrl.getParameters());
            if (CollectionUtils.isNotEmpty(providerFirstParams)) {
                providerFirstParams.forEach(consumerParams::remove);
            }
            params.putAll(consumerParams);
        }
        return params;
    }

    @Override
    public Map<String, String> getParameters() {
        String protocolServiceKey = getProtocolServiceKey();
        if (isEmpty(protocolServiceKey)) {
            return getInstance().getAllParams();
        }
        return getServiceParameters(protocolServiceKey);
    }

    @Override
    public URL addParameter(String key, String value) {
        if (isEmpty(key) || isEmpty(value)) {
            return this;
        }

        getInstance().putExtendParam(key, value);
        return this;
    }

    @Override
    public URL addParameterIfAbsent(String key, String value) {
        if (isEmpty(key) || isEmpty(value)) {
            return this;
        }

        getInstance().putExtendParamIfAbsent(key, value);
        return this;
    }

    public URL addServiceParameter(String protocolServiceKey, String key, String value) {
        if (isEmpty(key) || isEmpty(value)) {
            return this;
        }

        MetadataInfo.ServiceInfo serviceInfo = getServiceInfo(protocolServiceKey);
        if (null != serviceInfo) {
            serviceInfo.addParameter(key, value);
        }

        return this;
    }

    public URL addServiceParameterIfAbsent(String protocolServiceKey, String key, String value) {
        if (isEmpty(key) || isEmpty(value)) {
            return this;
        }

        MetadataInfo.ServiceInfo serviceInfo = getServiceInfo(protocolServiceKey);
        if (null != serviceInfo) {
            serviceInfo.addParameterIfAbsent(key, value);
        }

        return this;
    }

    public URL addConsumerParams(String protocolServiceKey, Map<String, String> params) {
        MetadataInfo.ServiceInfo serviceInfo = getServiceInfo(protocolServiceKey);
        if (null != serviceInfo) {
            serviceInfo.addConsumerParams(params);
        }

        return this;
    }

    /**
     * Gets method level value of the specified key.
     *
     * @param key
     * @return
     */
    @Override
    public String getAnyMethodParameter(String key) {
        String suffix = "." + key;
        String protocolServiceKey = getProtocolServiceKey();
        if (StringUtils.isNotEmpty(protocolServiceKey)) {
            MetadataInfo.ServiceInfo serviceInfo = getServiceInfo(protocolServiceKey);
            if (null == serviceInfo) {
                return null;
            }

            for (String fullKey : serviceInfo.getAllParams().keySet()) {
                if (fullKey.endsWith(suffix)) {
                    return getParameter(fullKey);
                }
            }
        }
        return null;
    }

    @Override
    public URLParam getUrlParam() {
        throw new UnsupportedOperationException("URLParam is replaced with MetadataInfo in instance url");
    }

    @Override
    public URLAddress getUrlAddress() {
        throw new UnsupportedOperationException("URLAddress is replaced with ServiceInstance in instance url");
    }

    @Override
    protected Map<String, Number> getServiceNumbers(String protocolServiceKey) {
        MetadataInfo.ServiceInfo serviceInfo = getServiceInfo(protocolServiceKey);

        return null == serviceInfo ? new ConcurrentHashMap<>() : serviceInfo.getNumbers();
    }

    @Override
    protected Map<String, Number> getNumbers() {
        String protocolServiceKey = getProtocolServiceKey();
        if (isEmpty(protocolServiceKey)) {
            if (numbers == null) { // concurrent initialization is tolerant
                numbers = new ConcurrentHashMap<>();
            }
            return numbers;
        }
        return getServiceNumbers(protocolServiceKey);
    }

    @Override
    protected Map<String, Map<String, Number>> getServiceMethodNumbers(String protocolServiceKey) {
        MetadataInfo.ServiceInfo serviceInfo = getServiceInfo(protocolServiceKey);
        return null == serviceInfo ? new ConcurrentHashMap<>() : serviceInfo.getMethodNumbers();
    }

    @Override
    protected Map<String, Map<String, Number>> getMethodNumbers() {
        String protocolServiceKey = getProtocolServiceKey();
        if (isEmpty(protocolServiceKey)) {
            if (methodNumbers == null) { // concurrent initialization is tolerant
                methodNumbers = new ConcurrentHashMap<>();
            }
            return methodNumbers;
        }
        return getServiceMethodNumbers(protocolServiceKey);
    }

    private MetadataInfo.ServiceInfo getServiceInfo(String protocolServiceKey) {
        return metadataInfo.getValidServiceInfo(protocolServiceKey);
    }

    private String getInstanceParameter(String key) {
        String value = this.instance.getMetadata().get(key);
        if (StringUtils.isNotEmpty(value)) {
            return value;
        }
        return this.instance.getExtendParam(key);
    }

    private Map<String, String> getInstanceMetadata() {
        return this.instance.getMetadata();
    }

    @Override
    public FrameworkModel getOrDefaultFrameworkModel() {
        return instance.getOrDefaultApplicationModel().getFrameworkModel();
    }

    @Override
    public ApplicationModel getOrDefaultApplicationModel() {
        return instance.getOrDefaultApplicationModel();
    }

    @Override
    public ApplicationModel getApplicationModel() {
        return instance.getApplicationModel();
    }

    @Override
    public ScopeModel getScopeModel() {
        return Optional.ofNullable(RpcContext.getServiceContext().getConsumerUrl())
            .map(URL::getScopeModel)
            .orElse(super.getScopeModel());
    }

    @Override
    public ServiceModel getServiceModel() {
        return RpcContext.getServiceContext().getConsumerUrl().getServiceModel();
    }

    public Set<String> getProviderFirstParams() {
        return providerFirstParams;
    }

    public void setProviderFirstParams(Set<String> providerFirstParams) {
        this.providerFirstParams = providerFirstParams;
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
        if (instance == null) {
            return "{}";
        }
        if (metadataInfo == null) {
            return instance.toString();
        }

        String protocolServiceKey = getProtocolServiceKey();
        if (StringUtils.isNotEmpty(protocolServiceKey)) {
            return instance.toString() + ", " + metadataInfo.getServiceString(protocolServiceKey);
        }

        return instance.toString();
    }
}
