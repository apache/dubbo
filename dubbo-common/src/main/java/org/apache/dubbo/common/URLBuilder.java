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
package org.apache.dubbo.common;

import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.dubbo.common.constants.CommonConstants.SCOPE_MODEL;

public final class URLBuilder extends ServiceConfigURL {
    private String protocol;

    private String username;

    private String password;

    // by default, host to registry
    private String host;

    // by default, port to registry
    private int port;

    private String path;

    private final Map<String, String> parameters;

    private final Map<String, Object> attributes;

    private Map<String, Map<String, String>> methodParameters;

    public URLBuilder() {
        protocol = null;
        username = null;
        password = null;
        host = null;
        port = 0;
        path = null;
        parameters = new HashMap<>();
        attributes = new HashMap<>();
        methodParameters = new HashMap<>();
    }

    public URLBuilder(String protocol, String host, int port) {
        this(protocol, null, null, host, port, null, null);
    }

    public URLBuilder(String protocol, String host, int port, String[] pairs) {
        this(protocol, null, null, host, port, null, CollectionUtils.toStringMap(pairs));
    }

    public URLBuilder(String protocol, String host, int port, Map<String, String> parameters) {
        this(protocol, null, null, host, port, null, parameters);
    }

    public URLBuilder(String protocol, String host, int port, String path) {
        this(protocol, null, null, host, port, path, null);
    }

    public URLBuilder(String protocol, String host, int port, String path, String... pairs) {
        this(protocol, null, null, host, port, path, CollectionUtils.toStringMap(pairs));
    }

    public URLBuilder(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this(protocol, null, null, host, port, path, parameters);
    }

    public URLBuilder(String protocol,
                      String username,
                      String password,
                      String host,
                      int port,
                      String path,
                      Map<String, String> parameters) {
        this(protocol, username, password, host, port, path, parameters, null);
    }

    public URLBuilder(String protocol,
                      String username,
                      String password,
                      String host,
                      int port,
                      String path,
                      Map<String, String> parameters,
                      Map<String, Object> attributes) {
        this.protocol = protocol;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.path = path;
        this.parameters = parameters != null ? parameters : new HashMap<>();
        this.attributes = attributes != null ? attributes : new HashMap<>();
    }

    public static URLBuilder from(URL url) {
        String protocol = url.getProtocol();
        String username = url.getUsername();
        String password = url.getPassword();
        String host = url.getHost();
        int port = url.getPort();
        String path = url.getPath();
        Map<String, String> parameters = new HashMap<>(url.getParameters());
        Map<String, Object> attributes = new HashMap<>(url.getAttributes());
        return new URLBuilder(
            protocol,
            username,
            password,
            host,
            port,
            path,
            parameters,
            attributes);
    }

    public ServiceConfigURL build() {
        if (StringUtils.isEmpty(username) && StringUtils.isNotEmpty(password)) {
            throw new IllegalArgumentException("Invalid url, password without username!");
        }
        port = Math.max(port, 0);
        // trim the leading "/"
        int firstNonSlash = 0;
        if (path != null) {
            while (firstNonSlash < path.length() && path.charAt(firstNonSlash) == '/') {
                firstNonSlash++;
            }
            if (firstNonSlash >= path.length()) {
                path = "";
            } else if (firstNonSlash > 0) {
                path = path.substring(firstNonSlash);
            }
        }
        return new ServiceConfigURL(protocol, username, password, host, port, path, parameters, attributes);
    }

    @Override
    public URLBuilder putAttribute(String key, Object obj) {
        attributes.put(key, obj);
        return this;
    }

    @Override
    public URLBuilder removeAttribute(String key) {
        attributes.remove(key);
        return this;
    }

    @Override
    public URLBuilder setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    @Override
    public URLBuilder setUsername(String username) {
        this.username = username;
        return this;
    }

    @Override
    public URLBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public URLBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    @Override
    public URLBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    @Override
    public URLBuilder setAddress(String address) {
        int i = address.lastIndexOf(':');
        String host;
        int port = this.port;
        if (i >= 0) {
            host = address.substring(0, i);
            port = Integer.parseInt(address.substring(i + 1));
        } else {
            host = address;
        }
        this.host = host;
        this.port = port;
        return this;
    }

    @Override
    public URLBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public URLBuilder setScopeModel(ScopeModel scopeModel) {
        this.attributes.put(SCOPE_MODEL, scopeModel);
        return this;
    }

    @Override
    public URLBuilder addParameterAndEncoded(String key, String value) {
        if (StringUtils.isEmpty(value)) {
            return this;
        }
        return addParameter(key, URL.encode(value));
    }

    @Override
    public URLBuilder addParameter(String key, boolean value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URLBuilder addParameter(String key, char value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URLBuilder addParameter(String key, byte value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URLBuilder addParameter(String key, short value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URLBuilder addParameter(String key, int value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URLBuilder addParameter(String key, long value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URLBuilder addParameter(String key, float value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URLBuilder addParameter(String key, double value) {
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URLBuilder addParameter(String key, Enum<?> value) {
        if (value == null) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URLBuilder addParameter(String key, Number value) {
        if (value == null) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URLBuilder addParameter(String key, CharSequence value) {
        if (value == null || value.length() == 0) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    @Override
    public URLBuilder addParameter(String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }

        parameters.put(key, value);
        return this;
    }

    public URLBuilder addMethodParameter(String method, String key, String value) {
        if (StringUtils.isEmpty(method) || StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }
        URL.putMethodParameter(method, key, value, methodParameters);
        return this;
    }

    @Override
    public URLBuilder addParameterIfAbsent(String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }
        if (hasParameter(key)) {
            return this;
        }
        parameters.put(key, value);
        return this;
    }

    public URLBuilder addMethodParameterIfAbsent(String method, String key, String value) {
        if (StringUtils.isEmpty(method) || StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }
        if (hasMethodParameter(method, key)) {
            return this;
        }
        URL.putMethodParameter(method, key, value, methodParameters);
        return this;
    }

    @Override
    public URLBuilder addParameters(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return this;
        }

        boolean hasAndEqual = true;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String oldValue = this.parameters.get(entry.getKey());
            String newValue = entry.getValue();
            if (!Objects.equals(oldValue, newValue)) {
                hasAndEqual = false;
                break;
            }
        }
        // return immediately if there's no change
        if (hasAndEqual) {
            return this;
        }

        this.parameters.putAll(parameters);
        return this;
    }

    public URLBuilder addMethodParameters(Map<String, Map<String, String>> methodParameters) {
        if (CollectionUtils.isEmptyMap(methodParameters)) {
            return this;
        }

        this.methodParameters.putAll(methodParameters);
        return this;
    }

    @Override
    public URLBuilder addParametersIfAbsent(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return this;
        }
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            this.parameters.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public URLBuilder addParameters(String... pairs) {
        if (pairs == null || pairs.length == 0) {
            return this;
        }
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Map pairs can not be odd number.");
        }
        Map<String, String> map = new HashMap<>();
        int len = pairs.length / 2;
        for (int i = 0; i < len; i++) {
            map.put(pairs[2 * i], pairs[2 * i + 1]);
        }
        return addParameters(map);
    }

    @Override
    public URLBuilder addParameterString(String query) {
        if (StringUtils.isEmpty(query)) {
            return this;
        }
        return addParameters(StringUtils.parseQueryString(query));
    }

    @Override
    public URLBuilder removeParameter(String key) {
        if (StringUtils.isEmpty(key)) {
            return this;
        }
        return removeParameters(key);
    }

    @Override
    public URLBuilder removeParameters(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return this;
        }
        return removeParameters(keys.toArray(new String[0]));
    }

    @Override
    public URLBuilder removeParameters(String... keys) {
        if (keys == null || keys.length == 0) {
            return this;
        }
        for (String key : keys) {
            parameters.remove(key);
        }
        return this;
    }

    @Override
    public URLBuilder clearParameters() {
        parameters.clear();
        return this;
    }

    @Override
    public boolean hasParameter(String key) {
        String value = getParameter(key);
        return StringUtils.isNotEmpty(value);
    }

    @Override
    public boolean hasMethodParameter(String method, String key) {
        if (method == null) {
            String suffix = "." + key;
            for (String fullKey : parameters.keySet()) {
                if (fullKey.endsWith(suffix)) {
                    return true;
                }
            }
            return false;
        }
        if (key == null) {
            String prefix = method + ".";
            for (String fullKey : parameters.keySet()) {
                if (fullKey.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
        String value = getMethodParameter(method, key);
        return value != null && value.length() > 0;
    }

    @Override
    public String getParameter(String key) {
        return parameters.get(key);
    }

    @Override
    public String getMethodParameter(String method, String key) {
        Map<String, String> keyMap = methodParameters.get(method);
        String value = null;
        if (keyMap != null) {
            value = keyMap.get(key);
        }
        return value;
    }
}
