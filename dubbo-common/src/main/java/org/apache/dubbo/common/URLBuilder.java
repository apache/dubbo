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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY_PREFIX;

public final class URLBuilder {
    private String protocol;

    private String username;

    private String password;

    // by default, host to registry
    private String host;

    // by default, port to registry
    private int port;

    private String path;

    private Map<String, String> parameters;

    public URLBuilder() {
        protocol = null;
        username = null;
        password = null;
        host = null;
        port = 0;
        path = null;
        parameters = new HashMap<>();
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

    public URLBuilder(String protocol, String username, String password, String host, int port, String path, Map<String, String> parameters) {
        this.protocol = protocol;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.path = path;
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }

    public static URLBuilder from(URL url) {
        String protocol = url.getProtocol();
        String username = url.getUsername();
        String password = url.getPassword();
        String host = url.getHost();
        int port = url.getPort();
        String path = url.getPath();
        Map<String, String> parameters = new HashMap<>(url.getParameters());
        return new URLBuilder(
                protocol,
                username,
                password,
                host,
                port,
                path,
                parameters);
    }

    public URL build() {
        if (StringUtils.isEmpty(username) && StringUtils.isNotEmpty(password)) {
            throw new IllegalArgumentException("Invalid url, password without username!");
        }
        port = port < 0 ? 0 : port;
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
        return new URL(protocol, username, password, host, port, path, parameters);
    }


    public URLBuilder setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public URLBuilder setUsername(String username) {
        this.username = username;
        return this;
    }

    public URLBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public URLBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    public URLBuilder setPort(int port) {
        this.port = port;
        return this;
    }

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

    public URLBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    public URLBuilder addParameterAndEncoded(String key, String value) {
        if (StringUtils.isEmpty(value)) {
            return this;
        }
        return addParameter(key, URL.encode(value));
    }

    public URLBuilder addParameter(String key, boolean value) {
        return addParameter(key, String.valueOf(value));
    }

    public URLBuilder addParameter(String key, char value) {
        return addParameter(key, String.valueOf(value));
    }

    public URLBuilder addParameter(String key, byte value) {
        return addParameter(key, String.valueOf(value));
    }

    public URLBuilder addParameter(String key, short value) {
        return addParameter(key, String.valueOf(value));
    }

    public URLBuilder addParameter(String key, int value) {
        return addParameter(key, String.valueOf(value));
    }

    public URLBuilder addParameter(String key, long value) {
        return addParameter(key, String.valueOf(value));
    }

    public URLBuilder addParameter(String key, float value) {
        return addParameter(key, String.valueOf(value));
    }

    public URLBuilder addParameter(String key, double value) {
        return addParameter(key, String.valueOf(value));
    }

    public URLBuilder addParameter(String key, Enum<?> value) {
        if (value == null) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    public URLBuilder addParameter(String key, Number value) {
        if (value == null) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    public URLBuilder addParameter(String key, CharSequence value) {
        if (value == null || value.length() == 0) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    public URLBuilder addParameter(String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }
        // if value doesn't change, return immediately
        if (value.equals(parameters.get(key))) { // value != null
            return this;
        }

        parameters.put(key, value);
        return this;
    }

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

    public URLBuilder addParametersIfAbsent(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return this;
        }
        this.parameters.putAll(parameters);
        return this;
    }

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

    public URLBuilder addParameterString(String query) {
        if (StringUtils.isEmpty(query)) {
            return this;
        }
        return addParameters(StringUtils.parseQueryString(query));
    }

    public URLBuilder removeParameter(String key) {
        if (StringUtils.isEmpty(key)) {
            return this;
        }
        return removeParameters(key);
    }

    public URLBuilder removeParameters(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return this;
        }
        return removeParameters(keys.toArray(new String[0]));
    }

    public URLBuilder removeParameters(String... keys) {
        if (keys == null || keys.length == 0) {
            return this;
        }
        for (String key : keys) {
            parameters.remove(key);
        }
        return this;
    }

    public URLBuilder clearParameters() {
        parameters.clear();
        return this;
    }

    public boolean hasParameter(String key) {
        String value = getParameter(key);
        return value != null && value.length() > 0;
    }

    public String getParameter(String key) {
        String value = parameters.get(key);
        if (StringUtils.isEmpty(value)) {
            value = parameters.get(DEFAULT_KEY_PREFIX + key);
        }
        return value;
    }
}
