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

import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.*;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;

@Deprecated
public class URL extends org.apache.dubbo.common.URL {

    private org.apache.dubbo.common.URL url;

    protected URL() {
        super();
        url = new org.apache.dubbo.common.URL(null, null);
    }

    public URL(org.apache.dubbo.common.URL url) {
        super(url.getProtocol(), url.getUsername(), url.getPassword(), url.getHost(), url.getPort(), url.getPath(), url.getParameters());
        this.url = url;
    }

    public URL(String protocol, String host, int port) {
        super(protocol, null, null, host, port, null, (Map<String, String>) null);
        url = new org.apache.dubbo.common.URL(protocol, null, null, host, port, null, (Map<String, String>) null);
    }

    public URL(String protocol, String host, int port, String[] pairs) {
        super(protocol, null, null, host, port, null, CollectionUtils.toStringMap(pairs));
        url = new org.apache.dubbo.common.URL(protocol, null, null, host, port, null,
            CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol, String host, int port, Map<String, String> parameters) {
        super(protocol, null, null, host, port, null, parameters);
        url = new org.apache.dubbo.common.URL(protocol, null, null, host, port, null, parameters);
    }

    public URL(String protocol, String host, int port, String path) {
        super(protocol, null, null, host, port, path, (Map<String, String>) null);
        url = new org.apache.dubbo.common.URL(protocol, null, null, host, port, path, (Map<String, String>) null);
    }

    public URL(String protocol, String host, int port, String path, String... pairs) {
        super(protocol, null, null, host, port, path, CollectionUtils.toStringMap(pairs));
        url = new org.apache.dubbo.common.URL(protocol, null, null, host, port, path,
            CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        super(protocol, null, null, host, port, path, parameters);
        url = new org.apache.dubbo.common.URL(protocol, null, null, host, port, path, parameters);
    }

    public URL(String protocol, String username, String password, String host, int port, String path) {
        super(protocol, username, password, host, port, path, (Map<String, String>) null);
        url = new org.apache.dubbo.common.URL(protocol, username, password, host, port, path,
            (Map<String, String>) null);
    }

    public URL(String protocol, String username, String password, String host, int port, String path, String... pairs) {
        super(protocol, username, password, host, port, path, CollectionUtils.toStringMap(pairs));
        url = new org.apache.dubbo.common.URL(protocol, username, password, host, port, path,
            CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol, String username, String password, String host, int port, String path, Map<String, String> parameters) {
        super(protocol, username, password, host, port, path, parameters);
        url = new org.apache.dubbo.common.URL(protocol, username, password, host, port, path, parameters);
    }

    public static URL valueOf(String url) {
        return new URL(org.apache.dubbo.common.URL.valueOf(url));
    }

    public static String encode(String value) {
        return org.apache.dubbo.common.URL.encode(value);
    }

    public static String decode(String value) {
        return org.apache.dubbo.common.URL.decode(value);
    }

    @Override
    public String getProtocol() {
        return url.getProtocol();
    }

    @Override
    public URL setProtocol(String protocol) {
        return new URL(url.setProtocol(protocol));
    }

    @Override
    public String getUsername() {
        return url.getUsername();
    }

    @Override
    public URL setUsername(String username) {
        return new URL(url.setUsername(username));
    }

    @Override
    public String getPassword() {
        return url.getPassword();
    }

    @Override
    public URL setPassword(String password) {
        return new URL(url.setPassword(password));
    }

    @Override
    public String getAuthority() {
        // Compatible with old version logic：The previous Authority only contained username and password information.
        return url.getUserInformation();
    }

    @Override
    public String getHost() {
        return url.getHost();
    }

    @Override
    public URL setHost(String host) {
        return new URL(url.setHost(host));
    }

    @Override
    public String getIp() {
        return url.getIp();
    }

    @Override
    public int getPort() {
        return url.getPort();
    }

    @Override
    public URL setPort(int port) {
        return new URL(url.setPort(port));
    }

    @Override
    public int getPort(int defaultPort) {
        return url.getPort();
    }

    @Override
    public String getAddress() {
        return url.getAddress();
    }

    @Override
    public URL setAddress(String address) {
        return new URL(url.setAddress(address));
    }

    @Override
    public String getBackupAddress() {
        return url.getBackupAddress();
    }

    @Override
    public String getBackupAddress(int defaultPort) {
        return url.getBackupAddress(defaultPort);
    }

    @Override
    public String getPath() {
        return url.getPath();
    }

    @Override
    public URL setPath(String path) {
        return new URL(url.setPath(path));
    }

    @Override
    public String getAbsolutePath() {
        return url.getAbsolutePath();
    }

    @Override
    public Map<String, String> getParameters() {
        return url.getParameters();
    }

    @Override
    public String getParameterAndDecoded(String key) {
        return url.getParameterAndDecoded(key);
    }

    @Override
    public String getParameterAndDecoded(String key, String defaultValue) {
        return org.apache.dubbo.common.URL.decode(getParameter(key, defaultValue));
    }

    @Override
    public String getParameter(String key) {
        return url.getParameter(key);
    }

    @Override
    public String getParameter(String key, String defaultValue) {
        return url.getParameter(key, defaultValue);
    }

    @Override
    public String[] getParameter(String key, String[] defaultValue) {
        return url.getParameter(key, defaultValue);
    }

    @Override
    public URL getUrlParameter(String key) {
        return new URL(url.getUrlParameter(key));
    }

    @Override
    public double getParameter(String key, double defaultValue) {
        return url.getParameter(key, defaultValue);
    }

    @Override
    public float getParameter(String key, float defaultValue) {
        return url.getParameter(key, defaultValue);
    }

    @Override
    public long getParameter(String key, long defaultValue) {
        return url.getParameter(key, defaultValue);
    }

    @Override
    public int getParameter(String key, int defaultValue) {
        return url.getParameter(key, defaultValue);
    }

    @Override
    public short getParameter(String key, short defaultValue) {
        return url.getParameter(key, defaultValue);
    }

    @Override
    public byte getParameter(String key, byte defaultValue) {
        return url.getParameter(key, defaultValue);
    }

    @Override
    public float getPositiveParameter(String key, float defaultValue) {
        return url.getPositiveParameter(key, defaultValue);
    }

    @Override
    public double getPositiveParameter(String key, double defaultValue) {
        return url.getPositiveParameter(key, defaultValue);
    }

    @Override
    public long getPositiveParameter(String key, long defaultValue) {
        return url.getPositiveParameter(key, defaultValue);
    }

    @Override
    public int getPositiveParameter(String key, int defaultValue) {
        return url.getPositiveParameter(key, defaultValue);
    }

    @Override
    public short getPositiveParameter(String key, short defaultValue) {
        return url.getPositiveParameter(key, defaultValue);
    }

    @Override
    public byte getPositiveParameter(String key, byte defaultValue) {
        return url.getPositiveParameter(key, defaultValue);
    }

    @Override
    public char getParameter(String key, char defaultValue) {
        return url.getParameter(key, defaultValue);
    }

    @Override
    public boolean getParameter(String key, boolean defaultValue) {
        return url.getParameter(key, defaultValue);
    }

    @Override
    public boolean hasParameter(String key) {
        return url.hasParameter(key);
    }

    @Override
    public String getMethodParameterAndDecoded(String method, String key) {
        return url.getMethodParameterAndDecoded(method, key);
    }

    @Override
    public String getMethodParameterAndDecoded(String method, String key, String defaultValue) {
        return url.getMethodParameterAndDecoded(method, key, defaultValue);
    }

    @Override
    public String getMethodParameter(String method, String key) {
        return url.getMethodParameter(method, key);
    }

    @Override
    public String getMethodParameter(String method, String key, String defaultValue) {
        return url.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public double getMethodParameter(String method, String key, double defaultValue) {
        return url.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public float getMethodParameter(String method, String key, float defaultValue) {
        return url.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public long getMethodParameter(String method, String key, long defaultValue) {
        return url.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public int getMethodParameter(String method, String key, int defaultValue) {
        return url.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public short getMethodParameter(String method, String key, short defaultValue) {
        return url.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public byte getMethodParameter(String method, String key, byte defaultValue) {
        return url.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public double getMethodPositiveParameter(String method, String key, double defaultValue) {
        return url.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public float getMethodPositiveParameter(String method, String key, float defaultValue) {
        return url.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public long getMethodPositiveParameter(String method, String key, long defaultValue) {
        return url.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public int getMethodPositiveParameter(String method, String key, int defaultValue) {
        return url.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public short getMethodPositiveParameter(String method, String key, short defaultValue) {
        return url.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public byte getMethodPositiveParameter(String method, String key, byte defaultValue) {
        return url.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public char getMethodParameter(String method, String key, char defaultValue) {
        return url.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public boolean getMethodParameter(String method, String key, boolean defaultValue) {
        return url.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public boolean hasMethodParameter(String method, String key) {
        return url.hasMethodParameter(method, key);
    }

    @Override
    public boolean isLocalHost() {
        return url.isLocalHost();
    }

    @Override
    public boolean isAnyHost() {
        return url.isAnyHost();
    }

    @Override
    public URLAddress getUrlAddress() {
        return url.getUrlAddress();
    }

    @Override
    public URLParam getUrlParam() {
        return url.getUrlParam();
    }

    @Override
    public String getSide(String defaultValue) {
        return url.getSide(defaultValue);
    }

    @Override
    public String getSide() {
        return url.getSide();
    }

    @Override
    public ScopeModel getScopeModel() {
        return url.getScopeModel();
    }

    @Override
    public FrameworkModel getOrDefaultFrameworkModel() {
        return url.getOrDefaultFrameworkModel();
    }

    @Override
    public ApplicationModel getOrDefaultApplicationModel() {
        return url.getOrDefaultApplicationModel();
    }

    @Override
    public ApplicationModel getApplicationModel() {
        return url.getApplicationModel();
    }

    @Override
    public ModuleModel getOrDefaultModuleModel() {
        return url.getOrDefaultModuleModel();
    }

    @Override
    public org.apache.dubbo.common.URL setServiceModel(ServiceModel serviceModel) {
        return url.setServiceModel(serviceModel);
    }

    @Override
    public ServiceModel getServiceModel() {
        return url.getServiceModel();
    }

    @Override
    public URL addParameterAndEncoded(String key, String value) {
        if (StringUtils.isEmpty(value)) {
            return this;
        }
        return addParameter(key, encode(value));
    }

    @Override
    public URL addParameter(String key, boolean value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URL addParameter(String key, char value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URL addParameter(String key, byte value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URL addParameter(String key, short value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URL addParameter(String key, int value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URL addParameter(String key, long value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URL addParameter(String key, float value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URL addParameter(String key, double value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URL addParameter(String key, Enum<?> value) {
        if (value == null) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URL addParameter(String key, Number value) {
        if (value == null) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URL addParameter(String key, CharSequence value) {
        if (value == null || value.length() == 0) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URL addParameter(String key, String value) {
        return new URL(url.addParameter(key, value));
    }

    @Override
    public URL addParameterIfAbsent(String key, String value) {
        return new URL(url.addParameterIfAbsent(key, value));
    }

    @Override
    public URL addParameters(Map<String, String> parameters) {
        return new URL(url.addParameters(parameters));
    }

    @Override
    public URL addParametersIfAbsent(Map<String, String> parameters) {
        return new URL(url.addParametersIfAbsent(parameters));
    }

    @Override
    public URL addParameters(String... pairs) {
        return new URL(url.addParameters(pairs));
    }

    @Override
    public URL addParameterString(String query) {
        return new URL(url.addParameterString(query));
    }

    @Override
    public URL removeParameter(String key) {
        return new URL(url.removeParameter(key));
    }

    @Override
    public URL removeParameters(Collection<String> keys) {
        return new URL(url.removeParameters(keys));
    }

    @Override
    public URL removeParameters(String... keys) {
        return new URL(url.removeParameters(keys));
    }

    @Override
    public URL clearParameters() {
        return new URL(url.clearParameters());
    }

    @Override
    public String getRawParameter(String key) {
        return url.getRawParameter(key);
    }

    @Override
    public Map<String, String> toMap() {
        return url.toMap();
    }

    @Override
    public String toString() {
        return url.toString();
    }

    @Override
    public String toString(String... parameters) {
        return url.toString(parameters);
    }

    @Override
    public String toIdentityString() {
        return url.toIdentityString();
    }

    @Override
    public String toIdentityString(String... parameters) {
        return url.toIdentityString(parameters);
    }

    @Override
    public String toFullString() {
        return url.toFullString();
    }

    @Override
    public String toFullString(String... parameters) {
        return url.toFullString(parameters);
    }

    @Override
    public String toParameterString() {
        return url.toParameterString();
    }

    @Override
    public String toParameterString(String... parameters) {
        return url.toParameterString(parameters);
    }

    @Override
    public java.net.URL toJavaURL() {
        return url.toJavaURL();
    }

    @Override
    public String getApplication(String defaultValue) {
        return url.getApplication(defaultValue);
    }

    @Override
    public String getApplication() {
        return url.getApplication();
    }

    @Override
    public String getRemoteApplication() {
        return url.getRemoteApplication();
    }

    @Override
    public String getGroup() {
        return url.getGroup();
    }

    @Override
    public String getVersion() {
        return url.getVersion();
    }

    @Override
    public String getCategory() {
        return url.getCategory();
    }

    @Override
    public org.apache.dubbo.common.URL putAttribute(String key, Object obj) {
        return url.putAttribute(key, obj);
    }

    @Override
    public org.apache.dubbo.common.URL removeAttribute(String key) {
        return url.removeAttribute(key);
    }

    @Override
    public double getServiceParameter(String service, String key, double defaultValue) {
        return url.getServiceParameter(service, key, defaultValue);
    }

    @Override
    public float getServiceParameter(String service, String key, float defaultValue) {
        return url.getServiceParameter(service, key, defaultValue);
    }

    @Override
    public long getServiceParameter(String service, String key, long defaultValue) {
        return url.getServiceParameter(service, key, defaultValue);
    }

    @Override
    public short getServiceParameter(String service, String key, short defaultValue) {
        return url.getServiceParameter(service, key, defaultValue);
    }

    @Override
    public byte getServiceParameter(String service, String key, byte defaultValue) {
        return url.getServiceParameter(service, key, defaultValue);
    }

    @Override
    public double getServiceMethodParameter(String service, String method, String key, double defaultValue) {
        return url.getServiceMethodParameter(service, method, key, defaultValue);
    }

    @Override
    public float getServiceMethodParameter(String service, String method, String key, float defaultValue) {
        return url.getServiceMethodParameter(service, method, key, defaultValue);
    }

    @Override
    public long getServiceMethodParameter(String service, String method, String key, long defaultValue) {
        return url.getServiceMethodParameter(service, method, key, defaultValue);
    }

    @Override
    public int getServiceMethodParameter(String service, String method, String key, int defaultValue) {
        return url.getServiceMethodParameter(service, method, key, defaultValue);
    }

    @Override
    public short getServiceMethodParameter(String service, String method, String key, short defaultValue) {
        return url.getServiceMethodParameter(service, method, key, defaultValue);
    }

    @Override
    public byte getServiceMethodParameter(String service, String method, String key, byte defaultValue) {
        return url.getServiceMethodParameter(service, method, key, defaultValue);
    }

    @Override
    public boolean hasServiceMethodParameter(String service, String method, String key) {
        return url.hasServiceMethodParameter(service, method, key);
    }

    @Override
    public boolean hasServiceMethodParameter(String service, String method) {
        return url.hasServiceMethodParameter(service, method);
    }

    @Override
    public String getServiceMethodParameter(String service, String method, String key) {
        return url.getServiceMethodParameter(service, method, key);
    }

    @Override
    public InetSocketAddress toInetSocketAddress() {
        return url.toInetSocketAddress();
    }

    @Override
    public String getServiceKey() {
        return url.getServiceKey();
    }

    @Override
    public String toServiceStringWithoutResolving() {
        return url.toServiceStringWithoutResolving();
    }

    @Override
    public String toServiceString() {
        return url.toServiceString();
    }

    @Override
    public String getServiceInterface() {
        return url.getServiceInterface();
    }

    @Override
    public URL setServiceInterface(String service) {
        return new URL(url.setServiceInterface(service));
    }

    public org.apache.dubbo.common.URL getOriginalURL() {
        return url;
    }
}
