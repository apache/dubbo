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
package com.alibaba.dubbo.common;

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ServiceModel;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Deprecated
public class DelegateURL extends com.alibaba.dubbo.common.URL {
    protected final org.apache.dubbo.common.URL apacheUrl;

    public DelegateURL(org.apache.dubbo.common.URL apacheUrl) {
        this.apacheUrl = apacheUrl;
    }

    public static com.alibaba.dubbo.common.URL valueOf(String url) {
        return new DelegateURL(org.apache.dubbo.common.URL.valueOf(url));
    }

    @Override
    public String getProtocol() {
        return apacheUrl.getProtocol();
    }

    @Override
    public com.alibaba.dubbo.common.URL setProtocol(String protocol) {
        return new DelegateURL(apacheUrl.setProtocol(protocol));
    }

    @Override
    public String getUsername() {
        return apacheUrl.getUsername();
    }

    @Override
    public com.alibaba.dubbo.common.URL setUsername(String username) {
        return new DelegateURL(apacheUrl.setUsername(username));
    }

    @Override
    public String getPassword() {
        return apacheUrl.getPassword();
    }

    @Override
    public com.alibaba.dubbo.common.URL setPassword(String password) {
        return new DelegateURL(apacheUrl.setPassword(password));
    }

    @Override
    public String getAuthority() {
        return apacheUrl.getAuthority();
    }

    @Override
    public String getHost() {
        return apacheUrl.getHost();
    }

    @Override
    public com.alibaba.dubbo.common.URL setHost(String host) {
        return new DelegateURL(apacheUrl.setHost(host));
    }

    @Override
    public String getIp() {
        return apacheUrl.getIp();
    }

    @Override
    public int getPort() {
        return apacheUrl.getPort();
    }

    @Override
    public com.alibaba.dubbo.common.URL setPort(int port) {
        return new DelegateURL(apacheUrl.setPort(port));
    }

    @Override
    public int getPort(int defaultPort) {
        return apacheUrl.getPort(defaultPort);
    }

    @Override
    public String getAddress() {
        return apacheUrl.getAddress();
    }

    @Override
    public com.alibaba.dubbo.common.URL setAddress(String address) {
        return new DelegateURL(apacheUrl.setAddress(address));
    }

    @Override
    public String getBackupAddress() {
        return apacheUrl.getBackupAddress();
    }

    @Override
    public String getBackupAddress(int defaultPort) {
        return apacheUrl.getBackupAddress(defaultPort);
    }

    @Override
    public String getPath() {
        return apacheUrl.getPath();
    }

    @Override
    public com.alibaba.dubbo.common.URL setPath(String path) {
        return new DelegateURL(apacheUrl.setPath(path));
    }

    @Override
    public String getAbsolutePath() {
        return apacheUrl.getAbsolutePath();
    }

    @Override
    public Map<String, String> getParameters() {
        return apacheUrl.getParameters();
    }

    @Override
    public String getParameterAndDecoded(String key) {
        return apacheUrl.getParameterAndDecoded(key);
    }

    @Override
    public String getParameterAndDecoded(String key, String defaultValue) {
        return apacheUrl.getParameterAndDecoded(key, defaultValue);
    }

    @Override
    public String getParameter(String key) {
        return apacheUrl.getParameter(key);
    }

    @Override
    public String getParameter(String key, String defaultValue) {
        return apacheUrl.getParameter(key, defaultValue);
    }

    @Override
    public String[] getParameter(String key, String[] defaultValue) {
        return apacheUrl.getParameter(key, defaultValue);
    }

    @Override
    public com.alibaba.dubbo.common.URL getUrlParameter(String key) {
        return new DelegateURL(apacheUrl.getUrlParameter(key));
    }

    @Override
    public double getParameter(String key, double defaultValue) {
        return apacheUrl.getParameter(key, defaultValue);
    }

    @Override
    public float getParameter(String key, float defaultValue) {
        return apacheUrl.getParameter(key, defaultValue);
    }

    @Override
    public long getParameter(String key, long defaultValue) {
        return apacheUrl.getParameter(key, defaultValue);
    }

    @Override
    public int getParameter(String key, int defaultValue) {
        return apacheUrl.getParameter(key, defaultValue);
    }

    @Override
    public short getParameter(String key, short defaultValue) {
        return apacheUrl.getParameter(key, defaultValue);
    }

    @Override
    public byte getParameter(String key, byte defaultValue) {
        return apacheUrl.getParameter(key, defaultValue);
    }

    @Override
    public float getPositiveParameter(String key, float defaultValue) {
        return apacheUrl.getPositiveParameter(key, defaultValue);
    }

    @Override
    public double getPositiveParameter(String key, double defaultValue) {
        return apacheUrl.getPositiveParameter(key, defaultValue);
    }

    @Override
    public long getPositiveParameter(String key, long defaultValue) {
        return apacheUrl.getPositiveParameter(key, defaultValue);
    }

    @Override
    public int getPositiveParameter(String key, int defaultValue) {
        return apacheUrl.getPositiveParameter(key, defaultValue);
    }

    @Override
    public short getPositiveParameter(String key, short defaultValue) {
        return apacheUrl.getPositiveParameter(key, defaultValue);
    }

    @Override
    public byte getPositiveParameter(String key, byte defaultValue) {
        return apacheUrl.getPositiveParameter(key, defaultValue);
    }

    @Override
    public char getParameter(String key, char defaultValue) {
        return apacheUrl.getParameter(key, defaultValue);
    }

    @Override
    public boolean getParameter(String key, boolean defaultValue) {
        return apacheUrl.getParameter(key, defaultValue);
    }

    @Override
    public boolean hasParameter(String key) {
        return apacheUrl.hasParameter(key);
    }

    @Override
    public String getMethodParameterAndDecoded(String method, String key) {
        return apacheUrl.getMethodParameterAndDecoded(method, key);
    }

    @Override
    public String getMethodParameterAndDecoded(String method, String key, String defaultValue) {
        return apacheUrl.getMethodParameterAndDecoded(method, key, defaultValue);
    }

    @Override
    public String getMethodParameter(String method, String key) {
        return apacheUrl.getMethodParameter(method, key);
    }

    @Override
    public String getMethodParameter(String method, String key, String defaultValue) {
        return apacheUrl.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public double getMethodParameter(String method, String key, double defaultValue) {
        return apacheUrl.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public float getMethodParameter(String method, String key, float defaultValue) {
        return apacheUrl.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public long getMethodParameter(String method, String key, long defaultValue) {
        return apacheUrl.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public int getMethodParameter(String method, String key, int defaultValue) {
        return apacheUrl.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public short getMethodParameter(String method, String key, short defaultValue) {
        return apacheUrl.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public byte getMethodParameter(String method, String key, byte defaultValue) {
        return apacheUrl.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public double getMethodPositiveParameter(String method, String key, double defaultValue) {
        return apacheUrl.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public float getMethodPositiveParameter(String method, String key, float defaultValue) {
        return apacheUrl.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public long getMethodPositiveParameter(String method, String key, long defaultValue) {
        return apacheUrl.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public int getMethodPositiveParameter(String method, String key, int defaultValue) {
        return apacheUrl.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public short getMethodPositiveParameter(String method, String key, short defaultValue) {
        return apacheUrl.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public byte getMethodPositiveParameter(String method, String key, byte defaultValue) {
        return apacheUrl.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public char getMethodParameter(String method, String key, char defaultValue) {
        return apacheUrl.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public boolean getMethodParameter(String method, String key, boolean defaultValue) {
        return apacheUrl.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public boolean hasMethodParameter(String method, String key) {
        return apacheUrl.hasMethodParameter(method, key);
    }

    @Override
    public boolean isLocalHost() {
        return apacheUrl.isLocalHost();
    }

    @Override
    public boolean isAnyHost() {
        return apacheUrl.isAnyHost();
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameterAndEncoded(String key, String value) {
        return new DelegateURL(apacheUrl.addParameterAndEncoded(key, value));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameter(String key, boolean value) {
        return new DelegateURL(apacheUrl.addParameter(key, value));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameter(String key, char value) {
        return new DelegateURL(apacheUrl.addParameter(key, value));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameter(String key, byte value) {
        return new DelegateURL(apacheUrl.addParameter(key, value));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameter(String key, short value) {
        return new DelegateURL(apacheUrl.addParameter(key, value));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameter(String key, int value) {
        return new DelegateURL(apacheUrl.addParameter(key, value));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameter(String key, long value) {
        return new DelegateURL(apacheUrl.addParameter(key, value));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameter(String key, float value) {
        return new DelegateURL(apacheUrl.addParameter(key, value));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameter(String key, double value) {
        return new DelegateURL(apacheUrl.addParameter(key, value));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameter(String key, Enum<?> value) {
        return new DelegateURL(apacheUrl.addParameter(key, value));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameter(String key, Number value) {
        return new DelegateURL(apacheUrl.addParameter(key, value));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameter(String key, CharSequence value) {
        return new DelegateURL(apacheUrl.addParameter(key, value));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameter(String key, String value) {
        return new DelegateURL(apacheUrl.addParameter(key, value));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameterIfAbsent(String key, String value) {
        return new DelegateURL(apacheUrl.addParameterIfAbsent(key, value));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameters(Map<String, String> parameters) {
        return new DelegateURL(apacheUrl.addParameters(parameters));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParametersIfAbsent(Map<String, String> parameters) {
        return new DelegateURL(apacheUrl.addParametersIfAbsent(parameters));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameters(String... pairs) {
        return new DelegateURL(apacheUrl.addParameters(pairs));
    }

    @Override
    public com.alibaba.dubbo.common.URL addParameterString(String query) {
        return new DelegateURL(apacheUrl.addParameterString(query));
    }

    @Override
    public com.alibaba.dubbo.common.URL removeParameter(String key) {
        return new DelegateURL(apacheUrl.removeParameter(key));
    }

    @Override
    public com.alibaba.dubbo.common.URL removeParameters(Collection<String> keys) {
        return new DelegateURL(apacheUrl.removeParameters(keys));
    }

    @Override
    public com.alibaba.dubbo.common.URL removeParameters(String... keys) {
        return new DelegateURL(apacheUrl.removeParameters(keys));
    }

    @Override
    public com.alibaba.dubbo.common.URL clearParameters() {
        return new DelegateURL(apacheUrl.clearParameters());
    }

    @Override
    public String getRawParameter(String key) {
        return apacheUrl.getRawParameter(key);
    }

    @Override
    public Map<String, String> toMap() {
        return apacheUrl.toMap();
    }

    @Override
    public String toString() {
        return apacheUrl.toString();
    }

    @Override
    public String toString(String... parameters) {
        return apacheUrl.toString(parameters);
    }

    @Override
    public String toIdentityString() {
        return apacheUrl.toIdentityString();
    }

    @Override
    public String toIdentityString(String... parameters) {
        return apacheUrl.toIdentityString(parameters);
    }

    @Override
    public String toFullString() {
        return apacheUrl.toFullString();
    }

    @Override
    public String toFullString(String... parameters) {
        return apacheUrl.toFullString(parameters);
    }

    @Override
    public String toParameterString() {
        return apacheUrl.toParameterString();
    }

    @Override
    public String toParameterString(String... parameters) {
        return apacheUrl.toParameterString(parameters);
    }

    @Override
    public java.net.URL toJavaURL() {
        return apacheUrl.toJavaURL();
    }

    @Override
    public InetSocketAddress toInetSocketAddress() {
        return apacheUrl.toInetSocketAddress();
    }

    @Override
    public String getServiceKey() {
        return apacheUrl.getServiceKey();
    }

    @Override
    public String toServiceStringWithoutResolving() {
        return apacheUrl.toServiceStringWithoutResolving();
    }

    @Override
    public String toServiceString() {
        return apacheUrl.toServiceString();
    }

    @Override
    public String getServiceInterface() {
        return apacheUrl.getServiceInterface();
    }

    @Override
    public com.alibaba.dubbo.common.URL setServiceInterface(String service) {
        return new DelegateURL(apacheUrl.setServiceInterface(service));
    }

    @Override
    public org.apache.dubbo.common.URL getOriginalURL() {
        return apacheUrl;
    }

    @Override
    public URLAddress getUrlAddress() {
        return apacheUrl.getUrlAddress();
    }

    @Override
    public URLParam getUrlParam() {
        return apacheUrl.getUrlParam();
    }

    @Override
    public String getUserInformation() {
        return apacheUrl.getUserInformation();
    }

    @Override
    public List<org.apache.dubbo.common.URL> getBackupUrls() {
        return apacheUrl.getBackupUrls();
    }

    @Override
    public Map<String, String> getOriginalParameters() {
        return apacheUrl.getOriginalParameters();
    }

    @Override
    public Map<String, String> getAllParameters() {
        return apacheUrl.getAllParameters();
    }

    @Override
    public Map<String, String> getParameters(Predicate<String> nameToSelect) {
        return apacheUrl.getParameters(nameToSelect);
    }

    @Override
    public String getOriginalParameter(String key) {
        return apacheUrl.getOriginalParameter(key);
    }

    @Override
    public List<String> getParameter(String key, List<String> defaultValue) {
        return apacheUrl.getParameter(key, defaultValue);
    }

    @Override
    public <T> T getParameter(String key, Class<T> valueType) {
        return apacheUrl.getParameter(key, valueType);
    }

    @Override
    public <T> T getParameter(String key, Class<T> valueType, T defaultValue) {
        return apacheUrl.getParameter(key, valueType, defaultValue);
    }

    @Override
    public org.apache.dubbo.common.URL setScopeModel(ScopeModel scopeModel) {
        return apacheUrl.setScopeModel(scopeModel);
    }

    @Override
    public ScopeModel getScopeModel() {
        return apacheUrl.getScopeModel();
    }

    @Override
    public FrameworkModel getOrDefaultFrameworkModel() {
        return apacheUrl.getOrDefaultFrameworkModel();
    }

    @Override
    public ApplicationModel getOrDefaultApplicationModel() {
        return apacheUrl.getOrDefaultApplicationModel();
    }

    @Override
    public ApplicationModel getApplicationModel() {
        return apacheUrl.getApplicationModel();
    }

    @Override
    public ModuleModel getOrDefaultModuleModel() {
        return apacheUrl.getOrDefaultModuleModel();
    }

    @Override
    public org.apache.dubbo.common.URL setServiceModel(ServiceModel serviceModel) {
        return apacheUrl.setServiceModel(serviceModel);
    }

    @Override
    public ServiceModel getServiceModel() {
        return apacheUrl.getServiceModel();
    }

    @Override
    public String getMethodParameterStrict(String method, String key) {
        return apacheUrl.getMethodParameterStrict(method, key);
    }

    @Override
    public String getAnyMethodParameter(String key) {
        return apacheUrl.getAnyMethodParameter(key);
    }

    @Override
    public boolean hasMethodParameter(String method) {
        return apacheUrl.hasMethodParameter(method);
    }

    @Override
    public Map<String, String> toOriginalMap() {
        return apacheUrl.toOriginalMap();
    }

    @Override
    public String getColonSeparatedKey() {
        return apacheUrl.getColonSeparatedKey();
    }

    @Override
    public String getDisplayServiceKey() {
        return apacheUrl.getDisplayServiceKey();
    }

    @Override
    public String getPathKey() {
        return apacheUrl.getPathKey();
    }

    public static String buildKey(String path, String group, String version) {
        return org.apache.dubbo.common.URL.buildKey(path, group, version);
    }

    @Override
    public String getProtocolServiceKey() {
        return apacheUrl.getProtocolServiceKey();
    }

    @Override
    @Deprecated
    public String getServiceName() {
        return apacheUrl.getServiceName();
    }

    @Override
    @Deprecated
    public int getIntParameter(String key) {
        return apacheUrl.getIntParameter(key);
    }

    @Override
    @Deprecated
    public int getIntParameter(String key, int defaultValue) {
        return apacheUrl.getIntParameter(key, defaultValue);
    }

    @Override
    @Deprecated
    public int getPositiveIntParameter(String key, int defaultValue) {
        return apacheUrl.getPositiveIntParameter(key, defaultValue);
    }

    @Override
    @Deprecated
    public boolean getBooleanParameter(String key) {
        return apacheUrl.getBooleanParameter(key);
    }

    @Override
    @Deprecated
    public boolean getBooleanParameter(String key, boolean defaultValue) {
        return apacheUrl.getBooleanParameter(key, defaultValue);
    }

    @Override
    @Deprecated
    public int getMethodIntParameter(String method, String key) {
        return apacheUrl.getMethodIntParameter(method, key);
    }

    @Override
    @Deprecated
    public int getMethodIntParameter(String method, String key, int defaultValue) {
        return apacheUrl.getMethodIntParameter(method, key, defaultValue);
    }

    @Override
    @Deprecated
    public int getMethodPositiveIntParameter(String method, String key, int defaultValue) {
        return apacheUrl.getMethodPositiveIntParameter(method, key, defaultValue);
    }

    @Override
    @Deprecated
    public boolean getMethodBooleanParameter(String method, String key) {
        return apacheUrl.getMethodBooleanParameter(method, key);
    }

    @Override
    @Deprecated
    public boolean getMethodBooleanParameter(String method, String key, boolean defaultValue) {
        return apacheUrl.getMethodBooleanParameter(method, key, defaultValue);
    }

    @Override
    public Configuration toConfiguration() {
        return apacheUrl.toConfiguration();
    }

    @Override
    public int hashCode() {
        return apacheUrl.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return apacheUrl.equals(obj);
    }

    public static void putMethodParameter(
            String method, String key, String value, Map<String, Map<String, String>> methodParameters) {
        org.apache.dubbo.common.URL.putMethodParameter(method, key, value, methodParameters);
    }

    @Override
    public String getApplication(String defaultValue) {
        return apacheUrl.getApplication(defaultValue);
    }

    @Override
    public String getApplication() {
        return apacheUrl.getApplication();
    }

    @Override
    public String getRemoteApplication() {
        return apacheUrl.getRemoteApplication();
    }

    @Override
    public String getGroup() {
        return apacheUrl.getGroup();
    }

    @Override
    public String getGroup(String defaultValue) {
        return apacheUrl.getGroup(defaultValue);
    }

    @Override
    public String getVersion() {
        return apacheUrl.getVersion();
    }

    @Override
    public String getVersion(String defaultValue) {
        return apacheUrl.getVersion(defaultValue);
    }

    @Override
    public String getConcatenatedParameter(String key) {
        return apacheUrl.getConcatenatedParameter(key);
    }

    @Override
    public String getCategory(String defaultValue) {
        return apacheUrl.getCategory(defaultValue);
    }

    @Override
    public String[] getCategory(String[] defaultValue) {
        return apacheUrl.getCategory(defaultValue);
    }

    @Override
    public String getCategory() {
        return apacheUrl.getCategory();
    }

    @Override
    public String getSide(String defaultValue) {
        return apacheUrl.getSide(defaultValue);
    }

    @Override
    public String getSide() {
        return apacheUrl.getSide();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return apacheUrl.getAttributes();
    }

    @Override
    public org.apache.dubbo.common.URL addAttributes(Map<String, Object> attributeMap) {
        return apacheUrl.addAttributes(attributeMap);
    }

    @Override
    public Object getAttribute(String key) {
        return apacheUrl.getAttribute(key);
    }

    @Override
    public Object getAttribute(String key, Object defaultValue) {
        return apacheUrl.getAttribute(key, defaultValue);
    }

    @Override
    public org.apache.dubbo.common.URL putAttribute(String key, Object obj) {
        return apacheUrl.putAttribute(key, obj);
    }

    @Override
    public org.apache.dubbo.common.URL removeAttribute(String key) {
        return apacheUrl.removeAttribute(key);
    }

    @Override
    public boolean hasAttribute(String key) {
        return apacheUrl.hasAttribute(key);
    }

    @Override
    public Map<String, String> getOriginalServiceParameters(String service) {
        return apacheUrl.getOriginalServiceParameters(service);
    }

    @Override
    public Map<String, String> getServiceParameters(String service) {
        return apacheUrl.getServiceParameters(service);
    }

    @Override
    public String getOriginalServiceParameter(String service, String key) {
        return apacheUrl.getOriginalServiceParameter(service, key);
    }

    @Override
    public String getServiceParameter(String service, String key) {
        return apacheUrl.getServiceParameter(service, key);
    }

    @Override
    public String getServiceParameter(String service, String key, String defaultValue) {
        return apacheUrl.getServiceParameter(service, key, defaultValue);
    }

    @Override
    public int getServiceParameter(String service, String key, int defaultValue) {
        return apacheUrl.getServiceParameter(service, key, defaultValue);
    }

    @Override
    public double getServiceParameter(String service, String key, double defaultValue) {
        return apacheUrl.getServiceParameter(service, key, defaultValue);
    }

    @Override
    public float getServiceParameter(String service, String key, float defaultValue) {
        return apacheUrl.getServiceParameter(service, key, defaultValue);
    }

    @Override
    public long getServiceParameter(String service, String key, long defaultValue) {
        return apacheUrl.getServiceParameter(service, key, defaultValue);
    }

    @Override
    public short getServiceParameter(String service, String key, short defaultValue) {
        return apacheUrl.getServiceParameter(service, key, defaultValue);
    }

    @Override
    public byte getServiceParameter(String service, String key, byte defaultValue) {
        return apacheUrl.getServiceParameter(service, key, defaultValue);
    }

    @Override
    public char getServiceParameter(String service, String key, char defaultValue) {
        return apacheUrl.getServiceParameter(service, key, defaultValue);
    }

    @Override
    public boolean getServiceParameter(String service, String key, boolean defaultValue) {
        return apacheUrl.getServiceParameter(service, key, defaultValue);
    }

    @Override
    public boolean hasServiceParameter(String service, String key) {
        return apacheUrl.hasServiceParameter(service, key);
    }

    @Override
    public float getPositiveServiceParameter(String service, String key, float defaultValue) {
        return apacheUrl.getPositiveServiceParameter(service, key, defaultValue);
    }

    @Override
    public double getPositiveServiceParameter(String service, String key, double defaultValue) {
        return apacheUrl.getPositiveServiceParameter(service, key, defaultValue);
    }

    @Override
    public long getPositiveServiceParameter(String service, String key, long defaultValue) {
        return apacheUrl.getPositiveServiceParameter(service, key, defaultValue);
    }

    @Override
    public int getPositiveServiceParameter(String service, String key, int defaultValue) {
        return apacheUrl.getPositiveServiceParameter(service, key, defaultValue);
    }

    @Override
    public short getPositiveServiceParameter(String service, String key, short defaultValue) {
        return apacheUrl.getPositiveServiceParameter(service, key, defaultValue);
    }

    @Override
    public byte getPositiveServiceParameter(String service, String key, byte defaultValue) {
        return apacheUrl.getPositiveServiceParameter(service, key, defaultValue);
    }

    @Override
    public String getServiceMethodParameterAndDecoded(String service, String method, String key) {
        return apacheUrl.getServiceMethodParameterAndDecoded(service, method, key);
    }

    @Override
    public String getServiceMethodParameterAndDecoded(String service, String method, String key, String defaultValue) {
        return apacheUrl.getServiceMethodParameterAndDecoded(service, method, key, defaultValue);
    }

    @Override
    public String getServiceMethodParameterStrict(String service, String method, String key) {
        return apacheUrl.getServiceMethodParameterStrict(service, method, key);
    }

    @Override
    public String getServiceMethodParameter(String service, String method, String key) {
        return apacheUrl.getServiceMethodParameter(service, method, key);
    }

    @Override
    public String getServiceMethodParameter(String service, String method, String key, String defaultValue) {
        return apacheUrl.getServiceMethodParameter(service, method, key, defaultValue);
    }

    @Override
    public double getServiceMethodParameter(String service, String method, String key, double defaultValue) {
        return apacheUrl.getServiceMethodParameter(service, method, key, defaultValue);
    }

    @Override
    public float getServiceMethodParameter(String service, String method, String key, float defaultValue) {
        return apacheUrl.getServiceMethodParameter(service, method, key, defaultValue);
    }

    @Override
    public long getServiceMethodParameter(String service, String method, String key, long defaultValue) {
        return apacheUrl.getServiceMethodParameter(service, method, key, defaultValue);
    }

    @Override
    public int getServiceMethodParameter(String service, String method, String key, int defaultValue) {
        return apacheUrl.getServiceMethodParameter(service, method, key, defaultValue);
    }

    @Override
    public short getServiceMethodParameter(String service, String method, String key, short defaultValue) {
        return apacheUrl.getServiceMethodParameter(service, method, key, defaultValue);
    }

    @Override
    public byte getServiceMethodParameter(String service, String method, String key, byte defaultValue) {
        return apacheUrl.getServiceMethodParameter(service, method, key, defaultValue);
    }

    @Override
    public boolean hasServiceMethodParameter(String service, String method, String key) {
        return apacheUrl.hasServiceMethodParameter(service, method, key);
    }

    @Override
    public boolean hasServiceMethodParameter(String service, String method) {
        return apacheUrl.hasServiceMethodParameter(service, method);
    }

    @Override
    public org.apache.dubbo.common.URL toSerializableURL() {
        return apacheUrl.toSerializableURL();
    }
}
