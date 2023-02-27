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
=======
import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ServiceModel;
>>>>>>> origin/3.2

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
<<<<<<< HEAD
import java.util.concurrent.ConcurrentHashMap;

public class OverrideInstanceAddressURL extends InstanceAddressURL {
    private static final long serialVersionUID = 6015101839312074851L;

    private final Map<String, String> overrideParams;
    private final InstanceAddressURL originUrl;

    private final transient Map<String, Map<String, Map<String, Number>>> methodNumberCache = new ConcurrentHashMap<>();
    private volatile transient Map<String, Map<String, Number>> methodNumbers;
    private final transient Map<String, Map<String, Number>> serviceNumberCache = new ConcurrentHashMap<>();
    private volatile transient Map<String, Number> numbers;

    public OverrideInstanceAddressURL(InstanceAddressURL originUrl) {
        this.originUrl = originUrl;
        this.overrideParams = new HashMap<>();
    }

    public OverrideInstanceAddressURL(InstanceAddressURL originUrl, Map<String, String> overrideParams) {
=======
import java.util.Set;

public class OverrideInstanceAddressURL extends InstanceAddressURL {
    private static final long serialVersionUID = 1373220432794558426L;

    private final URLParam overrideParams;
    private final InstanceAddressURL originUrl;

    public OverrideInstanceAddressURL(InstanceAddressURL originUrl) {
        this.originUrl = originUrl;
        this.overrideParams = URLParam.parse("");
    }

    public OverrideInstanceAddressURL(InstanceAddressURL originUrl, URLParam overrideParams) {
>>>>>>> origin/3.2
        this.originUrl = originUrl;
        this.overrideParams = overrideParams;
    }

    @Override
    public ServiceInstance getInstance() {
        return originUrl.getInstance();
    }

    @Override
    public MetadataInfo getMetadataInfo() {
        return originUrl.getMetadataInfo();
    }

    @Override
    public String getServiceInterface() {
        return originUrl.getServiceInterface();
    }

    @Override
    public String getGroup() {
        return originUrl.getGroup();
    }

    @Override
    public String getVersion() {
        return originUrl.getVersion();
    }

    @Override
    public String getProtocol() {
        return originUrl.getProtocol();
    }

    @Override
    public String getProtocolServiceKey() {
        return originUrl.getProtocolServiceKey();
    }

    @Override
    public String getServiceKey() {
        return originUrl.getServiceKey();
    }

    @Override
    public String getAddress() {
        return originUrl.getAddress();
    }

    @Override
    public String getHost() {
        return originUrl.getHost();
    }

    @Override
    public int getPort() {
        return originUrl.getPort();
    }

    @Override
    public String getIp() {
        return originUrl.getIp();
    }

    @Override
    public String getPath() {
        return originUrl.getPath();
    }

    @Override
    public String getParameter(String key) {
<<<<<<< HEAD
        String overrideParam = overrideParams.get(key);
=======
        String overrideParam = overrideParams.getParameter(key);
>>>>>>> origin/3.2
        return StringUtils.isNotEmpty(overrideParam) ? overrideParam : originUrl.getParameter(key);
    }

    @Override
    public String getServiceParameter(String service, String key) {
<<<<<<< HEAD
        String overrideParam = overrideParams.get(key);
=======
        String overrideParam = overrideParams.getParameter(key);
>>>>>>> origin/3.2
        return StringUtils.isNotEmpty(overrideParam) ? overrideParam : originUrl.getServiceParameter(service, key);
    }

    @Override
    public String getServiceMethodParameter(String protocolServiceKey, String method, String key) {
<<<<<<< HEAD
        String overrideParam = overrideParams.get(method + "." + key);
        return StringUtils.isNotEmpty(overrideParam) ?
                overrideParam :
                originUrl.getServiceMethodParameter(protocolServiceKey, method, key);
=======
        String overrideParam = overrideParams.getMethodParameter(method, key);
        return StringUtils.isNotEmpty(overrideParam) ?
            overrideParam :
            originUrl.getServiceMethodParameter(protocolServiceKey, method, key);
>>>>>>> origin/3.2
    }

    @Override
    public String getMethodParameter(String method, String key) {
<<<<<<< HEAD
        String overrideParam = overrideParams.get(method + "." + key);
        return StringUtils.isNotEmpty(overrideParam) ?
                overrideParam :
                originUrl.getMethodParameter(method, key);
=======
        String overrideParam = overrideParams.getMethodParameter(method, key);
        return StringUtils.isNotEmpty(overrideParam) ?
            overrideParam :
            originUrl.getMethodParameter(method, key);
>>>>>>> origin/3.2
    }

    @Override
    public boolean hasServiceMethodParameter(String protocolServiceKey, String method, String key) {
<<<<<<< HEAD
        return StringUtils.isNotEmpty(overrideParams.get(method + "." + key)) ||
                originUrl.hasServiceMethodParameter(protocolServiceKey, method, key);
=======
        return StringUtils.isNotEmpty(overrideParams.getMethodParameter(method, key)) ||
            originUrl.hasServiceMethodParameter(protocolServiceKey, method, key);
>>>>>>> origin/3.2
    }

    @Override
    public boolean hasMethodParameter(String method, String key) {
<<<<<<< HEAD
        return StringUtils.isNotEmpty(overrideParams.get(method + "." + key)) ||
                originUrl.hasMethodParameter(method, key);
=======
        return StringUtils.isNotEmpty(overrideParams.getMethodParameter(method, key)) ||
            originUrl.hasMethodParameter(method, key);
>>>>>>> origin/3.2
    }

    @Override
    public boolean hasServiceMethodParameter(String protocolServiceKey, String method) {
<<<<<<< HEAD
        return overrideParams.keySet().stream().anyMatch((e)->e.startsWith(method) || originUrl.hasServiceMethodParameter(protocolServiceKey, method));
=======
        return overrideParams.hasMethodParameter(method) || originUrl.hasServiceMethodParameter(protocolServiceKey, method);
>>>>>>> origin/3.2
    }

    @Override
    public boolean hasMethodParameter(String method) {
<<<<<<< HEAD
        return overrideParams.keySet().stream().anyMatch((e)->e.startsWith(method) || originUrl.hasMethodParameter(method));
=======
        return overrideParams.hasMethodParameter(method) || originUrl.hasMethodParameter(method);
>>>>>>> origin/3.2
    }

    @Override
    public Map<String, String> getServiceParameters(String protocolServiceKey) {
        Map<String, String> parameters = originUrl.getServiceParameters(protocolServiceKey);
<<<<<<< HEAD
        Map<String, String> overrideParameters = overrideParams;
=======
        Map<String, String> overrideParameters = overrideParams.getParameters();
>>>>>>> origin/3.2
        Map<String, String> result = new HashMap<>((int) (parameters.size() + overrideParameters.size() / 0.75f) + 1);
        result.putAll(parameters);
        result.putAll(overrideParameters);
        return result;
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> parameters = originUrl.getParameters();
<<<<<<< HEAD
        Map<String, String> overrideParameters = overrideParams;
=======
        Map<String, String> overrideParameters = overrideParams.getParameters();
>>>>>>> origin/3.2
        Map<String, String> result = new HashMap<>((int) (parameters.size() + overrideParameters.size() / 0.75f) + 1);
        result.putAll(parameters);
        result.putAll(overrideParameters);
        return result;
    }

    @Override
    public URL addParameter(String key, String value) {
<<<<<<< HEAD
        HashMap<String, String> map = new HashMap<>(overrideParams);
        map.put(key, value);
        return new OverrideInstanceAddressURL(originUrl, map);
=======
        return new OverrideInstanceAddressURL(originUrl, overrideParams.addParameter(key, value));
>>>>>>> origin/3.2
    }

    @Override
    public URL addParameterIfAbsent(String key, String value) {
<<<<<<< HEAD
        HashMap<String, String> map = new HashMap<>(overrideParams);
        map.putIfAbsent(key, value);
        return new OverrideInstanceAddressURL(originUrl, map);
=======
        return new OverrideInstanceAddressURL(originUrl, overrideParams.addParameterIfAbsent(key, value));
>>>>>>> origin/3.2
    }

    @Override
    public URL addServiceParameter(String protocolServiceKey, String key, String value) {
        return originUrl.addServiceParameter(protocolServiceKey, key, value);
    }

    @Override
    public URL addServiceParameterIfAbsent(String protocolServiceKey, String key, String value) {
        return originUrl.addServiceParameterIfAbsent(protocolServiceKey, key, value);
    }

    @Override
    public URL addConsumerParams(String protocolServiceKey, Map<String, String> params) {
        return originUrl.addConsumerParams(protocolServiceKey, params);
    }

    @Override
<<<<<<< HEAD
    public URL addParameters(Map<String, String> parameters) {
        HashMap<String, String> map = new HashMap<>(overrideParams);
        map.putAll(parameters);
        return new OverrideInstanceAddressURL(originUrl, map);
=======
    public String getAnyMethodParameter(String key) {
        String overrideParam = overrideParams.getAnyMethodParameter(key);
        return StringUtils.isNotEmpty(overrideParam) ? overrideParam : originUrl.getAnyMethodParameter(key);
    }

    @Override
    public URL addParameters(Map<String, String> parameters) {
        return new OverrideInstanceAddressURL(originUrl, overrideParams.addParameters(parameters));
>>>>>>> origin/3.2
    }

    @Override
    public URL addParametersIfAbsent(Map<String, String> parameters) {
<<<<<<< HEAD
        HashMap<String, String> map = new HashMap<>(overrideParams);
        parameters.forEach(map::putIfAbsent);
        return new OverrideInstanceAddressURL(originUrl, map);
    }

    @Override
    protected Map<String, Number> getServiceNumbers(String protocolServiceKey) {
        return serviceNumberCache.computeIfAbsent(protocolServiceKey, (k) -> new ConcurrentHashMap<>());
    }

    @Override
    protected Map<String, Number> getNumbers() {
        if (numbers == null) { // concurrent initialization is tolerant
            numbers = new ConcurrentHashMap<>();
        }
        return numbers;
    }

    @Override
    protected Map<String, Map<String, Number>> getServiceMethodNumbers(String protocolServiceKey) {
        return methodNumberCache.computeIfAbsent(protocolServiceKey, (k) -> new ConcurrentHashMap<>());
    }

    @Override
    protected Map<String, Map<String, Number>> getMethodNumbers() {
        if (methodNumbers == null) { // concurrent initialization is tolerant
            methodNumbers = new ConcurrentHashMap<>();
        }
        return methodNumbers;
    }

    public Map<String, String> getOverrideParams() {
        return overrideParams;
=======
        return new OverrideInstanceAddressURL(originUrl, overrideParams.addParametersIfAbsent(parameters));
    }

    @Override
    public URLParam getUrlParam() {
        return originUrl.getUrlParam();
    }

    @Override
    public URLAddress getUrlAddress() {
        return originUrl.getUrlAddress();
    }

    public URLParam getOverrideParams() {
        return overrideParams;
    }

    @Override
    public String getRemoteApplication() {
        return originUrl.getRemoteApplication();
    }

    @Override
    public String getSide() {
        return originUrl.getSide();
    }

    @Override
    public ScopeModel getScopeModel() {
        return originUrl.getScopeModel();
    }

    @Override
    public FrameworkModel getOrDefaultFrameworkModel() {
        return originUrl.getOrDefaultFrameworkModel();
    }

    @Override
    public ApplicationModel getOrDefaultApplicationModel() {
        return originUrl.getOrDefaultApplicationModel();
    }

    @Override
    public ApplicationModel getApplicationModel() {
        return originUrl.getApplicationModel();
    }

    @Override
    public ModuleModel getOrDefaultModuleModel() {
        return originUrl.getOrDefaultModuleModel();
    }

    @Override
    public ServiceModel getServiceModel() {
        return originUrl.getServiceModel();
    }

    @Override
    public Set<String> getProviderFirstParams() {
        return originUrl.getProviderFirstParams();
    }

    @Override
    public void setProviderFirstParams(Set<String> providerFirstParams) {
        originUrl.setProviderFirstParams(providerFirstParams);
>>>>>>> origin/3.2
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        OverrideInstanceAddressURL that = (OverrideInstanceAddressURL) o;
        return Objects.equals(overrideParams, that.overrideParams) && Objects.equals(originUrl, that.originUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), overrideParams, originUrl);
    }

    @Override
    public String toString() {
        return originUrl.toString() + ", overrideParams: " + overrideParams.toString();
    }

<<<<<<< HEAD
=======
    private Object readResolve() {
        // create a new object from the deserialized one
        return new OverrideInstanceAddressURL(this.originUrl, this.overrideParams);
    }
    @Override
    protected OverrideInstanceAddressURL newURL(URLAddress urlAddress, URLParam urlParam) {
        return new OverrideInstanceAddressURL(originUrl, overrideParams);
    }
>>>>>>> origin/3.2
}
