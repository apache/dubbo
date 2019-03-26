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

import org.apache.dubbo.common.utils.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;

@Deprecated
public class URL extends org.apache.dubbo.common.URL {

    public URL(org.apache.dubbo.common.URL url) {
        super(url.getProtocol(), url.getUsername(), url.getPassword(), url.getHost(), url.getPort(), url.getPath(), url.getParameters());
    }

    public URL(String protocol, String host, int port) {
        super(protocol, null, null, host, port, null, (Map<String, String>) null);
    }

    public URL(String protocol, String host, int port, String[] pairs) {
        super(protocol, null, null, host, port, null, CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol, String host, int port, Map<String, String> parameters) {
        super(protocol, null, null, host, port, null, parameters);
    }

    public URL(String protocol, String host, int port, String path) {
        super(protocol, null, null, host, port, path, (Map<String, String>) null);
    }

    public URL(String protocol, String host, int port, String path, String... pairs) {
        super(protocol, null, null, host, port, path, CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        super(protocol, null, null, host, port, path, parameters);
    }

    public URL(String protocol, String username, String password, String host, int port, String path) {
        super(protocol, username, password, host, port, path, (Map<String, String>) null);
    }

    public URL(String protocol, String username, String password, String host, int port, String path, String... pairs) {
        super(protocol, username, password, host, port, path, CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol, String username, String password, String host, int port, String path, Map<String, String> parameters) {
        super(protocol, username, password, host, port, path, parameters);
    }

    public static URL valueOf(String url) {
        org.apache.dubbo.common.URL result = org.apache.dubbo.common.URL.valueOf(url);
        return new URL(result);
    }

    public static String encode(String value) {
        return org.apache.dubbo.common.URL.encode(value);
    }

    public static String decode(String value) {
        return org.apache.dubbo.common.URL.decode(value);
    }

    @Override
    public String getProtocol() {
        return super.getProtocol();
    }

    @Override
    public URL setProtocol(String protocol) {
        return new URL(protocol, super.getUsername(), super.getPassword(), super.getHost(), super.getPort(), super.getPath(), super.getParameters());
    }

    @Override
    public String getUsername() {
        return super.getUsername();
    }

    @Override
    public URL setUsername(String username) {
        return new URL(super.getProtocol(), username, super.getPassword(), super.getHost(), super.getPort(), super.getPath(), super.getParameters());
    }

    @Override
    public String getPassword() {
        return super.getPassword();
    }

    @Override
    public URL setPassword(String password) {
        return new URL(super.getProtocol(), super.getUsername(), password, super.getHost(), super.getPort(), super.getPath(), super.getParameters());
    }

    @Override
    public String getAuthority() {
        return super.getAuthority();
    }

    @Override
    public String getHost() {
        return super.getHost();
    }

    @Override
    public URL setHost(String host) {
        return new URL(super.getProtocol(), super.getUsername(), super.getPassword(), host, super.getPort(), super.getPath(), super.getParameters());
    }

    @Override
    public String getIp() {
        return super.getIp();
    }

    @Override
    public int getPort() {
        return super.getPort();
    }

    @Override
    public URL setPort(int port) {
        return new URL(super.getProtocol(), super.getUsername(), super.getPassword(), super.getHost(), port, super.getPath(), super.getParameters());
    }

    @Override
    public int getPort(int defaultPort) {
        return super.getPort();
    }

    @Override
    public String getAddress() {
        return super.getAddress();
    }

    @Override
    public URL setAddress(String address) {
        org.apache.dubbo.common.URL result = super.setAddress(address);
        return new URL(result);
    }

    @Override
    public String getBackupAddress() {
        return super.getBackupAddress();
    }

    @Override
    public String getBackupAddress(int defaultPort) {
        return super.getBackupAddress(defaultPort);
    }

//    public List<URL> getBackupUrls() {
//        List<org.apache.dubbo.common.URL> res = super.getBackupUrls();
//        return res.stream().map(url -> new URL(url)).collect(Collectors.toList());
//    }

    @Override
    public String getPath() {
        return super.getPath();
    }

    @Override
    public URL setPath(String path) {
        return new URL(super.getProtocol(), super.getUsername(), super.getPassword(), super.getHost(), super.getPort(), path, super.getParameters());
    }

    @Override
    public String getAbsolutePath() {
        return super.getAbsolutePath();
    }

    @Override
    public Map<String, String> getParameters() {
        return super.getParameters();
    }

    @Override
    public String getParameterAndDecoded(String key) {
        return super.getParameterAndDecoded(key);
    }

    @Override
    public String getParameterAndDecoded(String key, String defaultValue) {
        return org.apache.dubbo.common.URL.decode(getParameter(key, defaultValue));
    }

    @Override
    public String getParameter(String key) {
        return super.getParameter(key);
    }

    @Override
    public String getParameter(String key, String defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    @Override
    public String[] getParameter(String key, String[] defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    @Override
    public URL getUrlParameter(String key) {
        org.apache.dubbo.common.URL result = super.getUrlParameter(key);
        return new URL(result);
    }

    @Override
    public double getParameter(String key, double defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    @Override
    public float getParameter(String key, float defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    @Override
    public long getParameter(String key, long defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    @Override
    public int getParameter(String key, int defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    @Override
    public short getParameter(String key, short defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    @Override
    public byte getParameter(String key, byte defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    @Override
    public float getPositiveParameter(String key, float defaultValue) {
        return super.getPositiveParameter(key, defaultValue);
    }

    @Override
    public double getPositiveParameter(String key, double defaultValue) {
        return super.getPositiveParameter(key, defaultValue);
    }

    @Override
    public long getPositiveParameter(String key, long defaultValue) {
        return super.getPositiveParameter(key, defaultValue);
    }

    @Override
    public int getPositiveParameter(String key, int defaultValue) {
        return super.getPositiveParameter(key, defaultValue);
    }

    @Override
    public short getPositiveParameter(String key, short defaultValue) {
        return super.getPositiveParameter(key, defaultValue);
    }

    @Override
    public byte getPositiveParameter(String key, byte defaultValue) {
        return super.getPositiveParameter(key, defaultValue);
    }

    @Override
    public char getParameter(String key, char defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    @Override
    public boolean getParameter(String key, boolean defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    @Override
    public boolean hasParameter(String key) {
        return super.hasParameter(key);
    }

    @Override
    public String getMethodParameterAndDecoded(String method, String key) {
        return super.getMethodParameterAndDecoded(method, key);
    }

    @Override
    public String getMethodParameterAndDecoded(String method, String key, String defaultValue) {
        return super.getMethodParameterAndDecoded(method, key, defaultValue);
    }

    @Override
    public String getMethodParameter(String method, String key) {
        return super.getMethodParameter(method, key);
    }

    @Override
    public String getMethodParameter(String method, String key, String defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public double getMethodParameter(String method, String key, double defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public float getMethodParameter(String method, String key, float defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public long getMethodParameter(String method, String key, long defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public int getMethodParameter(String method, String key, int defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public short getMethodParameter(String method, String key, short defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public byte getMethodParameter(String method, String key, byte defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public double getMethodPositiveParameter(String method, String key, double defaultValue) {
        return super.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public float getMethodPositiveParameter(String method, String key, float defaultValue) {
        return super.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public long getMethodPositiveParameter(String method, String key, long defaultValue) {
        return super.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public int getMethodPositiveParameter(String method, String key, int defaultValue) {
        return super.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public short getMethodPositiveParameter(String method, String key, short defaultValue) {
        return super.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public byte getMethodPositiveParameter(String method, String key, byte defaultValue) {
        return super.getMethodPositiveParameter(method, key, defaultValue);
    }

    @Override
    public char getMethodParameter(String method, String key, char defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public boolean getMethodParameter(String method, String key, boolean defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    @Override
    public boolean hasMethodParameter(String method, String key) {
        return super.hasMethodParameter(method, key);
    }

    @Override
    public boolean isLocalHost() {
        return super.isLocalHost();
    }

    @Override
    public boolean isAnyHost() {
        return super.isAnyHost();
    }

    @Override
    public URL addParameterAndEncoded(String key, String value) {
        if (value == null || value.length() == 0) {
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
        org.apache.dubbo.common.URL result = super.addParameter(key, value);
        return new URL(result);
    }

    @Override
    public URL addParameterIfAbsent(String key, String value) {
        org.apache.dubbo.common.URL result = super.addParameterIfAbsent(key, value);
        return new URL(result);
    }

    @Override
    public URL addParameters(Map<String, String> parameters) {
        org.apache.dubbo.common.URL result = super.addParameters(parameters);
        return new URL(result);
    }

    @Override
    public URL addParametersIfAbsent(Map<String, String> parameters) {
        org.apache.dubbo.common.URL result = super.addParametersIfAbsent(parameters);
        return new URL(result);
    }

    @Override
    public URL addParameters(String... pairs) {
        org.apache.dubbo.common.URL result = super.addParameters(pairs);
        return new URL(result);
    }

    @Override
    public URL addParameterString(String query) {
        org.apache.dubbo.common.URL result = super.addParameterString(query);
        return new URL(result);
    }

    @Override
    public URL removeParameter(String key) {
        org.apache.dubbo.common.URL result = super.removeParameter(key);
        return new URL(result);
    }

    @Override
    public URL removeParameters(Collection<String> keys) {
        org.apache.dubbo.common.URL result = super.removeParameters(keys);
        return new URL(result);
    }

    @Override
    public URL removeParameters(String... keys) {
        org.apache.dubbo.common.URL result = super.removeParameters(keys);
        return new URL(result);
    }

    @Override
    public URL clearParameters() {
        org.apache.dubbo.common.URL result = super.clearParameters();
        return new URL(result);
    }

    @Override
    public String getRawParameter(String key) {
        return super.getRawParameter(key);
    }

    @Override
    public Map<String, String> toMap() {
        return super.toMap();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String toString(String... parameters) {
        return super.toString(parameters);
    }

    @Override
    public String toIdentityString() {
        return super.toIdentityString();
    }

    @Override
    public String toIdentityString(String... parameters) {
        return super.toIdentityString(parameters);
    }

    @Override
    public String toFullString() {
        return super.toFullString();
    }

    @Override
    public String toFullString(String... parameters) {
        return super.toFullString(parameters);
    }

    @Override
    public String toParameterString() {
        return super.toParameterString();
    }

    @Override
    public String toParameterString(String... parameters) {
        return super.toParameterString(parameters);
    }

    @Override
    public java.net.URL toJavaURL() {
        return super.toJavaURL();
    }

    @Override
    public InetSocketAddress toInetSocketAddress() {
        return super.toInetSocketAddress();
    }

    @Override
    public String getServiceKey() {
        return super.getServiceKey();
    }

    @Override
    public String toServiceStringWithoutResolving() {
        return super.toServiceStringWithoutResolving();
    }

    @Override
    public String toServiceString() {
        return super.toServiceString();
    }

    @Override
    public String getServiceInterface() {
        return super.getServiceInterface();
    }

    @Override
    public URL setServiceInterface(String service) {
        org.apache.dubbo.common.URL result = super.setServiceInterface(service);
        return new URL(result);
    }

    public org.apache.dubbo.common.URL getOriginalURL() {
        return new org.apache.dubbo.common.URL(super.getProtocol(), super.getUsername(), super.getPassword(),
                super.getHost(), super.getPort(), super.getPath(), super.getParameters());
    }
}
