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
<<<<<<< HEAD
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
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;

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
=======
import org.apache.dubbo.common.url.component.ServiceConfigURL;
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
>>>>>>> origin/3.2
    }

    public ServiceInstance getInstance() {
        return instance;
    }

    public MetadataInfo getMetadataInfo() {
        return metadataInfo;
    }

    @Override
    public String getServiceInterface() {
<<<<<<< HEAD
        return RpcContext.getContext().getInterfaceName();
    }

    public String getGroup() {
        return RpcContext.getContext().getGroup();
    }

    public String getVersion() {
        return RpcContext.getContext().getVersion();
=======
        return RpcContext.getServiceContext().getInterfaceName();
    }

    @Override
    public String getGroup() {
        return RpcContext.getServiceContext().getGroup();
    }

    @Override
    public String getVersion() {
        return RpcContext.getServiceContext().getVersion();
>>>>>>> origin/3.2
    }

    @Override
    public String getProtocol() {
<<<<<<< HEAD
        return RpcContext.getContext().getProtocol();
=======
        return protocol;
>>>>>>> origin/3.2
    }

    @Override
    public String getProtocolServiceKey() {
<<<<<<< HEAD
        return RpcContext.getContext().getProtocolServiceKey();
=======
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
>>>>>>> origin/3.2
    }

    @Override
    public String getServiceKey() {
<<<<<<< HEAD
        return RpcContext.getContext().getServiceKey();
=======
        return RpcContext.getServiceContext().getServiceKey();
    }

    @Override
    public URL setProtocol(String protocol) {
        return new ServiceConfigURL(protocol, getUsername(), getPassword(), getHost(), getPort(), getPath(), getParameters(), attributes);
    }

    @Override
    public URL setHost(String host) {
        return new ServiceConfigURL(getProtocol(), getUsername(), getPassword(), host, getPort(), getPath(), getParameters(), attributes);
    }

    @Override
    public URL setPort(int port) {
        return new ServiceConfigURL(getProtocol(), getUsername(), getPassword(), getHost(), port, getPath(), getParameters(), attributes);
    }

    @Override
    public URL setPath(String path) {
        return new ServiceConfigURL(getProtocol(), getUsername(), getPassword(), getHost(), getPort(), path, getParameters(), attributes);
>>>>>>> origin/3.2
    }

    @Override
    public String getAddress() {
        return instance.getAddress();
    }

    @Override
<<<<<<< HEAD
    public String getPath() {
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(getProtocolServiceKey());
=======
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
>>>>>>> origin/3.2
        return serviceInfo.getPath();
    }

    @Override
<<<<<<< HEAD
=======
    public String getOriginalParameter(String key) {
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

        String protocolServiceKey = getProtocolServiceKey();
        if (isEmpty(protocolServiceKey)) {
            return getInstanceParameter(key);
        }
        return getServiceParameter(protocolServiceKey, key);
    }

    @Override
>>>>>>> origin/3.2
    public String getParameter(String key) {
        if (VERSION_KEY.equals(key)) {
            return getVersion();
        } else if (GROUP_KEY.equals(key)) {
            return getGroup();
        } else if (INTERFACE_KEY.equals(key)) {
            return getServiceInterface();
        } else if (REMOTE_APPLICATION_KEY.equals(key)) {
            return instance.getServiceName();
        } else if (SIDE_KEY.equals(key)) {
<<<<<<< HEAD
            return CONSUMER_SIDE;
        }

        String protocolServiceKey = getProtocolServiceKey();
        if (protocolServiceKey == null) {
=======
            return getSide();
        }

        if (consumerParamFirst(key)) {
            URL consumerUrl = RpcContext.getServiceContext().getConsumerUrl();
            if (consumerUrl != null) {
                String v = consumerUrl.getParameter(key);
                if (StringUtils.isNotEmpty(v)) {
                    return v;
                }
            }
        }

        String protocolServiceKey = getProtocolServiceKey();
        if (isEmpty(protocolServiceKey)) {
>>>>>>> origin/3.2
            return getInstanceParameter(key);
        }
        return getServiceParameter(protocolServiceKey, key);
    }

    @Override
<<<<<<< HEAD
    public String getServiceParameter(String service, String key) {
        String value = getInstanceParameter(key);
        if (StringUtils.isEmpty(value) && metadataInfo != null) {
            value = metadataInfo.getParameter(key, service);
        }
        return value;
=======
    public String getOriginalServiceParameter(String service, String key) {
        if (metadataInfo != null) {
            String value = metadataInfo.getParameter(key, service);
            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
        }

        return getInstanceParameter(key);
    }

    @Override
    public String getServiceParameter(String service, String key) {
        if (consumerParamFirst(key)) {
            URL consumerUrl = RpcContext.getServiceContext().getConsumerUrl();
            if (consumerUrl != null) {
                String v = consumerUrl.getServiceParameter(service, key);
                if (StringUtils.isNotEmpty(v)) {
                    return v;
                }
            }
        }

        if (metadataInfo != null) {
            String value = metadataInfo.getParameter(key, service);
            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
        }

        return getInstanceParameter(key);
>>>>>>> origin/3.2
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
<<<<<<< HEAD
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(protocolServiceKey);
=======
        if (consumerParamFirst(key)) {
            URL consumerUrl = RpcContext.getServiceContext().getConsumerUrl();
            if (consumerUrl != null) {
                String v = consumerUrl.getServiceMethodParameter(protocolServiceKey, method, key);
                if (StringUtils.isNotEmpty(v)) {
                    return v;
                }
            }
        }

        MetadataInfo.ServiceInfo serviceInfo = getServiceInfo(protocolServiceKey);
        if (null == serviceInfo) {
            return getParameter(key);
        }

>>>>>>> origin/3.2
        String value = serviceInfo.getMethodParameter(method, key, null);
        if (StringUtils.isNotEmpty(value)) {
            return value;
        }
        return getParameter(key);
    }

    @Override
    public String getMethodParameter(String method, String key) {
<<<<<<< HEAD
        String protocolServiceKey = getProtocolServiceKey();
        if (protocolServiceKey == null) {
=======
        if (consumerParamFirst(key)) {
            URL consumerUrl = RpcContext.getServiceContext().getConsumerUrl();
            if (consumerUrl != null) {
                String v = consumerUrl.getMethodParameter(method, key);
                if (StringUtils.isNotEmpty(v)) {
                    return v;
                }
            }
        }

        String protocolServiceKey = getProtocolServiceKey();
        if (isEmpty(protocolServiceKey)) {
>>>>>>> origin/3.2
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
<<<<<<< HEAD
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(protocolServiceKey);

        if (method == null) {
=======
        if (consumerParamFirst(key)) {
            URL consumerUrl = RpcContext.getServiceContext().getConsumerUrl();
            if (consumerUrl != null) {
                if (consumerUrl.hasServiceMethodParameter(protocolServiceKey, method, key)) {
                    return true;
                }
            }
        }

        MetadataInfo.ServiceInfo serviceInfo = getServiceInfo(protocolServiceKey);

        if (isEmpty(method)) {
>>>>>>> origin/3.2
            String suffix = "." + key;
            for (String fullKey : getParameters().keySet()) {
                if (fullKey.endsWith(suffix)) {
                    return true;
                }
            }
            return false;
        }
<<<<<<< HEAD
        if (key == null) {
=======
        if (isEmpty(key)) {
>>>>>>> origin/3.2
            String prefix = method + ".";
            for (String fullKey : getParameters().keySet()) {
                if (fullKey.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

<<<<<<< HEAD
=======
        if (null == serviceInfo) {
            return false;
        }

>>>>>>> origin/3.2
        return serviceInfo.hasMethodParameter(method, key);
    }

    @Override
    public boolean hasMethodParameter(String method, String key) {
<<<<<<< HEAD
        String protocolServiceKey = getProtocolServiceKey();
        if (protocolServiceKey == null) {
=======
        if (consumerParamFirst(key)) {
            URL consumerUrl = RpcContext.getServiceContext().getConsumerUrl();
            if (consumerUrl != null) {
                if (consumerUrl.hasMethodParameter(method, key)) {
                    return true;
                }
            }
        }

        String protocolServiceKey = getProtocolServiceKey();
        if (isEmpty(protocolServiceKey)) {
>>>>>>> origin/3.2
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
<<<<<<< HEAD
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(protocolServiceKey);
=======
        URL consumerUrl = RpcContext.getServiceContext().getConsumerUrl();
        if (consumerUrl != null) {
            if (consumerUrl.hasServiceMethodParameter(protocolServiceKey, method)) {
                return true;
            }
        }

        MetadataInfo.ServiceInfo serviceInfo = getServiceInfo(protocolServiceKey);
        if (null == serviceInfo) {
            return false;
        }

>>>>>>> origin/3.2
        return serviceInfo.hasMethodParameter(method);
    }

    @Override
    public boolean hasMethodParameter(String method) {
<<<<<<< HEAD
        String protocolServiceKey = getProtocolServiceKey();
        if (protocolServiceKey == null) {
=======
        URL consumerUrl = RpcContext.getServiceContext().getConsumerUrl();
        if (consumerUrl != null) {
            if (consumerUrl.hasMethodParameter(method)) {
                return true;
            }
        }

        String protocolServiceKey = getProtocolServiceKey();
        if (isEmpty(protocolServiceKey)) {
>>>>>>> origin/3.2
            return false;
        }
        return hasServiceMethodParameter(protocolServiceKey, method);
    }

<<<<<<< HEAD
=======
    @Override
    public Map<String, String> getOriginalServiceParameters(String protocolServiceKey) {
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

        return params;
    }


>>>>>>> origin/3.2
    /**
     * Avoid calling this method in RPC call.
     *
     * @return
     */
    @Override
    public Map<String, String> getServiceParameters(String protocolServiceKey) {
<<<<<<< HEAD
        Map<String, String> instanceParams = getInstanceMetadata();
=======
        Map<String, String> instanceParams = getInstance().getAllParams();
>>>>>>> origin/3.2
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
<<<<<<< HEAD
=======

        URL consumerUrl = RpcContext.getServiceContext().getConsumerUrl();
        if (consumerUrl != null) {
            Map<String, String> consumerParams = new HashMap<>(consumerUrl.getParameters());
            if (CollectionUtils.isNotEmpty(providerFirstParams)) {
                providerFirstParams.forEach(consumerParams::remove);
            }
            params.putAll(consumerParams);
        }
>>>>>>> origin/3.2
        return params;
    }

    @Override
<<<<<<< HEAD
    public Map<String, String> getParameters() {
        String protocolServiceKey = getProtocolServiceKey();
        if (protocolServiceKey == null) {
=======
    public Map<String, String> getOriginalParameters() {
        String protocolServiceKey = getProtocolServiceKey();
        if (isEmpty(protocolServiceKey)) {
            return getInstance().getAllParams();
        }
        return getOriginalServiceParameters(protocolServiceKey);
    }

    @Override
    public Map<String, String> getParameters() {
        String protocolServiceKey = getProtocolServiceKey();
        if (isEmpty(protocolServiceKey)) {
>>>>>>> origin/3.2
            return getInstance().getAllParams();
        }
        return getServiceParameters(protocolServiceKey);
    }

    @Override
    public URL addParameter(String key, String value) {
<<<<<<< HEAD
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }

        getInstance().getExtendParams().put(key, value);
=======
        if (isEmpty(key) || isEmpty(value)) {
            return this;
        }

        getInstance().putExtendParam(key, value);
>>>>>>> origin/3.2
        return this;
    }

    @Override
    public URL addParameterIfAbsent(String key, String value) {
<<<<<<< HEAD
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }

        getInstance().getExtendParams().putIfAbsent(key, value);
=======
        if (isEmpty(key) || isEmpty(value)) {
            return this;
        }

        getInstance().putExtendParamIfAbsent(key, value);
>>>>>>> origin/3.2
        return this;
    }

    public URL addServiceParameter(String protocolServiceKey, String key, String value) {
<<<<<<< HEAD
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }

        getMetadataInfo().getServiceInfo(protocolServiceKey).addParameter(key, value);
=======
        if (isEmpty(key) || isEmpty(value)) {
            return this;
        }

        MetadataInfo.ServiceInfo serviceInfo = getServiceInfo(protocolServiceKey);
        if (null != serviceInfo) {
            serviceInfo.addParameter(key, value);
        }

>>>>>>> origin/3.2
        return this;
    }

    public URL addServiceParameterIfAbsent(String protocolServiceKey, String key, String value) {
<<<<<<< HEAD
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }

        getMetadataInfo().getServiceInfo(protocolServiceKey).addParameterIfAbsent(key, value);
=======
        if (isEmpty(key) || isEmpty(value)) {
            return this;
        }

        MetadataInfo.ServiceInfo serviceInfo = getServiceInfo(protocolServiceKey);
        if (null != serviceInfo) {
            serviceInfo.addParameterIfAbsent(key, value);
        }

>>>>>>> origin/3.2
        return this;
    }

    public URL addConsumerParams(String protocolServiceKey, Map<String, String> params) {
<<<<<<< HEAD
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
=======
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

    private MetadataInfo.ServiceInfo getServiceInfo(String protocolServiceKey) {
        return metadataInfo.getValidServiceInfo(protocolServiceKey);
>>>>>>> origin/3.2
    }

    private String getInstanceParameter(String key) {
        String value = this.instance.getMetadata().get(key);
        if (StringUtils.isNotEmpty(value)) {
            return value;
        }
<<<<<<< HEAD
        return this.instance.getExtendParams().get(key);
=======
        return this.instance.getExtendParam(key);
>>>>>>> origin/3.2
    }

    private Map<String, String> getInstanceMetadata() {
        return this.instance.getMetadata();
    }

    @Override
<<<<<<< HEAD
=======
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

    private boolean consumerParamFirst(String key) {
        if (CollectionUtils.isNotEmpty(providerFirstParams)) {
            return !providerFirstParams.contains(key);
        } else {
            return true;
        }
    }

    @Override
>>>>>>> origin/3.2
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

<<<<<<< HEAD
    public String getServiceString(String service) {
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(service);
        if (serviceInfo == null) {
            return instance.toString();
        }
        return instance.toString() + serviceInfo.toString();
    }

=======
>>>>>>> origin/3.2
    @Override
    public String toString() {
        if (instance == null) {
            return "{}";
        }
        if (metadataInfo == null) {
            return instance.toString();
        }
<<<<<<< HEAD
        return instance.toString() + metadataInfo.toString();
=======

        String protocolServiceKey = getProtocolServiceKey();
        if (StringUtils.isNotEmpty(protocolServiceKey)) {
            return instance.toString() + ", " + metadataInfo.getServiceString(protocolServiceKey);
        }

        return instance.toString();
>>>>>>> origin/3.2
    }
}
