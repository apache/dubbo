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

    public String getProtocol() {
        return super.getProtocol();
    }

    public URL setProtocol(String protocol) {
        return new URL(protocol, super.getUsername(), super.getPassword(), super.getHost(), super.getPort(), super.getPath(), super.getParameters());
    }

    public String getUsername() {
        return super.getUsername();
    }

    public URL setUsername(String username) {
        return new URL(super.getProtocol(), username, super.getPassword(), super.getHost(), super.getPort(), super.getPath(), super.getParameters());
    }

    public String getPassword() {
        return super.getPassword();
    }

    public URL setPassword(String password) {
        return new URL(super.getProtocol(), super.getUsername(), password, super.getHost(), super.getPort(), super.getPath(), super.getParameters());
    }

    public String getAuthority() {
        return super.getAuthority();
    }

    public String getHost() {
        return super.getHost();
    }

    public URL setHost(String host) {
        return new URL(super.getProtocol(), super.getUsername(), super.getPassword(), host, super.getPort(), super.getPath(), super.getParameters());
    }

    public String getIp() {
        return super.getIp();
    }

    public int getPort() {
        return super.getPort();
    }

    public URL setPort(int port) {
        return new URL(super.getProtocol(), super.getUsername(), super.getPassword(), super.getHost(), port, super.getPath(), super.getParameters());
    }

    public int getPort(int defaultPort) {
        return super.getPort();
    }

    public String getAddress() {
        return super.getAddress();
    }

    public URL setAddress(String address) {
        org.apache.dubbo.common.URL result = super.setAddress(address);
        return new URL(result);
    }

    public String getBackupAddress() {
        return super.getBackupAddress();
    }

    public String getBackupAddress(int defaultPort) {
        return super.getBackupAddress(defaultPort);
    }

//    public List<URL> getBackupUrls() {
//        List<org.apache.dubbo.common.URL> res = super.getBackupUrls();
//        return res.stream().map(url -> new URL(url)).collect(Collectors.toList());
//    }

    public String getPath() {
        return super.getPath();
    }

    public URL setPath(String path) {
        return new URL(super.getProtocol(), super.getUsername(), super.getPassword(), super.getHost(), super.getPort(), path, super.getParameters());
    }

    public String getAbsolutePath() {
        return super.getAbsolutePath();
    }

    public Map<String, String> getParameters() {
        return super.getParameters();
    }

    public String getParameterAndDecoded(String key) {
        return super.getParameterAndDecoded(key);
    }

    public String getParameterAndDecoded(String key, String defaultValue) {
        return super.decode(getParameter(key, defaultValue));
    }

    public String getParameter(String key) {
        return super.getParameter(key);
    }

    public String getParameter(String key, String defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    public String[] getParameter(String key, String[] defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    public URL getUrlParameter(String key) {
        org.apache.dubbo.common.URL result = super.getUrlParameter(key);
        return new URL(result);
    }

    public double getParameter(String key, double defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    public float getParameter(String key, float defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    public long getParameter(String key, long defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    public int getParameter(String key, int defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    public short getParameter(String key, short defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    public byte getParameter(String key, byte defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    public float getPositiveParameter(String key, float defaultValue) {
        return super.getPositiveParameter(key, defaultValue);
    }

    public double getPositiveParameter(String key, double defaultValue) {
        return super.getPositiveParameter(key, defaultValue);
    }

    public long getPositiveParameter(String key, long defaultValue) {
        return super.getPositiveParameter(key, defaultValue);
    }

    public int getPositiveParameter(String key, int defaultValue) {
        return super.getPositiveParameter(key, defaultValue);
    }

    public short getPositiveParameter(String key, short defaultValue) {
        return super.getPositiveParameter(key, defaultValue);
    }

    public byte getPositiveParameter(String key, byte defaultValue) {
        return super.getPositiveParameter(key, defaultValue);
    }

    public char getParameter(String key, char defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    public boolean getParameter(String key, boolean defaultValue) {
        return super.getParameter(key, defaultValue);
    }

    public boolean hasParameter(String key) {
        return super.hasParameter(key);
    }

    public String getMethodParameterAndDecoded(String method, String key) {
        return super.getMethodParameterAndDecoded(method, key);
    }

    public String getMethodParameterAndDecoded(String method, String key, String defaultValue) {
        return super.getMethodParameterAndDecoded(method, key, defaultValue);
    }

    public String getMethodParameter(String method, String key) {
        return super.getMethodParameter(method, key);
    }

    public String getMethodParameter(String method, String key, String defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    public double getMethodParameter(String method, String key, double defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    public float getMethodParameter(String method, String key, float defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    public long getMethodParameter(String method, String key, long defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    public int getMethodParameter(String method, String key, int defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    public short getMethodParameter(String method, String key, short defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    public byte getMethodParameter(String method, String key, byte defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    public double getMethodPositiveParameter(String method, String key, double defaultValue) {
        return super.getMethodPositiveParameter(method, key, defaultValue);
    }

    public float getMethodPositiveParameter(String method, String key, float defaultValue) {
        return super.getMethodPositiveParameter(method, key, defaultValue);
    }

    public long getMethodPositiveParameter(String method, String key, long defaultValue) {
        return super.getMethodPositiveParameter(method, key, defaultValue);
    }

    public int getMethodPositiveParameter(String method, String key, int defaultValue) {
        return super.getMethodPositiveParameter(method, key, defaultValue);
    }

    public short getMethodPositiveParameter(String method, String key, short defaultValue) {
        return super.getMethodPositiveParameter(method, key, defaultValue);
    }

    public byte getMethodPositiveParameter(String method, String key, byte defaultValue) {
        return super.getMethodPositiveParameter(method, key, defaultValue);
    }

    public char getMethodParameter(String method, String key, char defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    public boolean getMethodParameter(String method, String key, boolean defaultValue) {
        return super.getMethodParameter(method, key, defaultValue);
    }

    public boolean hasMethodParameter(String method, String key) {
        return super.hasMethodParameter(method, key);
    }

    public boolean isLocalHost() {
        return super.isLocalHost();
    }

    public boolean isAnyHost() {
        return super.isAnyHost();
    }

    public URL addParameterAndEncoded(String key, String value) {
        if (value == null || value.length() == 0) {
            return this;
        }
        return addParameter(key, encode(value));
    }

    public URL addParameter(String key, boolean value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, char value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, byte value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, short value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, int value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, long value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, float value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, double value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, Enum<?> value) {
        if (value == null) return this;
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, Number value) {
        if (value == null) return this;
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, CharSequence value) {
        if (value == null || value.length() == 0) return this;
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, String value) {
        org.apache.dubbo.common.URL result = super.addParameter(key, value);
        return new URL(result);
    }

    public URL addParameterIfAbsent(String key, String value) {
        org.apache.dubbo.common.URL result = super.addParameterIfAbsent(key, value);
        return new URL(result);
    }

    public URL addParameters(Map<String, String> parameters) {
        org.apache.dubbo.common.URL result = super.addParameters(parameters);
        return new URL(result);
    }

    public URL addParametersIfAbsent(Map<String, String> parameters) {
        org.apache.dubbo.common.URL result = super.addParametersIfAbsent(parameters);
        return new URL(result);
    }

    public URL addParameters(String... pairs) {
        org.apache.dubbo.common.URL result = super.addParameters(pairs);
        return new URL(result);
    }

    public URL addParameterString(String query) {
        org.apache.dubbo.common.URL result = super.addParameterString(query);
        return new URL(result);
    }

    public URL removeParameter(String key) {
        org.apache.dubbo.common.URL result = super.removeParameter(key);
        return new URL(result);
    }

    public URL removeParameters(Collection<String> keys) {
        org.apache.dubbo.common.URL result = super.removeParameters(keys);
        return new URL(result);
    }

    public URL removeParameters(String... keys) {
        org.apache.dubbo.common.URL result = super.removeParameters(keys);
        return new URL(result);
    }

    public URL clearParameters() {
        org.apache.dubbo.common.URL result = super.clearParameters();
        return new URL(result);
    }

    public String getRawParameter(String key) {
        return super.getRawParameter(key);
    }

    public Map<String, String> toMap() {
        return super.toMap();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public String toString(String... parameters) {
        return super.toString(parameters);
    }

    public String toIdentityString() {
        return super.toIdentityString();
    }

    public String toIdentityString(String... parameters) {
        return super.toIdentityString(parameters);
    }

    public String toFullString() {
        return super.toFullString();
    }

    public String toFullString(String... parameters) {
        return super.toFullString(parameters);
    }

    public String toParameterString() {
        return super.toParameterString();
    }

    public String toParameterString(String... parameters) {
        return super.toParameterString(parameters);
    }

    public java.net.URL toJavaURL() {
        return super.toJavaURL();
    }

    public InetSocketAddress toInetSocketAddress() {
        return super.toInetSocketAddress();
    }

    public String getServiceKey() {
        return super.getServiceKey();
    }

    public String toServiceStringWithoutResolving() {
        return super.toServiceStringWithoutResolving();
    }

    public String toServiceString() {
        return super.toServiceString();
    }

    public String getServiceInterface() {
        return super.getServiceInterface();
    }

    public URL setServiceInterface(String service) {
        org.apache.dubbo.common.URL result = super.setServiceInterface(service);
        return new URL(result);
    }

    public org.apache.dubbo.common.URL getOriginalURL() {
        return new org.apache.dubbo.common.URL(super.getProtocol(), super.getUsername(), super.getPassword(),
                super.getHost(), super.getPort(), super.getPath(), super.getParameters());
    }
}
