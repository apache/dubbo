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
package org.apache.dubbo.common.url.component;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLStrParser;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;

public class URLParam implements Serializable {
    private static final long serialVersionUID = -1985165475234910535L;

    private final String rawParam;
    private final Map<String, String> params;

    //cache
    private transient Map<String, Map<String, String>> methodParameters;
    private transient long timestamp;

    public URLParam(Map<String, String> params) {
        this(params, null);
    }

    public URLParam(Map<String, String> params, String rawParam) {
        this.params = Collections.unmodifiableMap((params == null ? new HashMap<>() : new HashMap<>(params)));
        this.rawParam = rawParam;

        this.timestamp = System.currentTimeMillis();
    }

    public Map<String, Map<String, String>> getMethodParameters() {
        if (methodParameters == null) {
            methodParameters = Collections.unmodifiableMap(initMethodParameters(this.params));
        }
        return methodParameters;
    }

    public static Map<String, Map<String, String>> initMethodParameters(Map<String, String> parameters) {
        Map<String, Map<String, String>> methodParameters = new HashMap<>();
        if (parameters == null) {
            return methodParameters;
        }

        String methodsString = parameters.get(METHODS_KEY);
        if (StringUtils.isNotEmpty(methodsString)) {
            String[] methods = methodsString.split(",");
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String key = entry.getKey();
                for (String method : methods) {
                    String methodPrefix = method + '.';
                    if (key.startsWith(methodPrefix)) {
                        String realKey = key.substring(methodPrefix.length());
                        URL.putMethodParameter(method, realKey, entry.getValue(), methodParameters);
                    }
                }
            }
        } else {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String key = entry.getKey();
                int methodSeparator = key.indexOf('.');
                if (methodSeparator > 0) {
                    String method = key.substring(0, methodSeparator);
                    String realKey = key.substring(methodSeparator + 1);
                    URL.putMethodParameter(method, realKey, entry.getValue(), methodParameters);
                }
            }
        }
        return methodParameters;
    }

    public Map<String, String> getParameters() {
        return params;
    }

    public URLParam addParameter(String key, String value) {
        if (StringUtils.isEmpty(key)
                || StringUtils.isEmpty(value)) {
            return this;
        }
        // if value doesn't change, return immediately
        if (value.equals(getParameters().get(key))) { // value != null
            return this;
        }

        Map<String, String> map = new HashMap<>(getParameters());
        map.put(key, value);
        return new URLParam(map, rawParam);
    }

    public URLParam addParameterIfAbsent(String key, String value) {
        if (StringUtils.isEmpty(key)
                || StringUtils.isEmpty(value)) {
            return this;
        }
        if (hasParameter(key)) {
            return this;
        }

        Map<String, String> map = new HashMap<>(getParameters());
        map.put(key, value);

        return new URLParam(map, rawParam);
    }

    /**
     * Add parameters to a new url.
     *
     * @param parameters parameters in key-value pairs
     * @return A new URL
     */
    public URLParam addParameters(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return this;
        }

        boolean hasAndEqual = true;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String value = getParameters().get(entry.getKey());
            if (value == null) {
                if (entry.getValue() != null) {
                    hasAndEqual = false;
                    break;
                }
            } else {
                if (!value.equals(entry.getValue())) {
                    hasAndEqual = false;
                    break;
                }
            }
        }
        // return immediately if there's no change
        if (hasAndEqual) {
            return this;
        }

        Map<String, String> map = new HashMap<>((int)(getParameters().size() + parameters.size() / 0.75f) + 1);
        map.putAll(getParameters());
        map.putAll(parameters);
        return new URLParam(map, rawParam);
    }

    public URLParam addParametersIfAbsent(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return this;
        }

        Map<String, String> map = new HashMap<>((int)(getParameters().size() + parameters.size() / 0.75f) + 1);
        map.putAll(parameters);
        map.putAll(getParameters());
        return new URLParam(map, rawParam);
    }

    public URLParam removeParameters(String... keys) {
        if (keys == null || keys.length == 0) {
            return this;
        }

        Map<String, String> map = new HashMap<>(getParameters());
        for (String key : keys) {
            map.remove(key);
        }
        if (map.size() == getParameters().size()) {
            return this;
        }
        return new URLParam(map, rawParam);
    }

    public URLParam clearParameters() {
        return new URLParam(new HashMap<>());
    }

    public boolean hasParameter(String key) {
        String value = getParameter(key);
        return value != null && value.length() > 0;
    }

    public String getParameter(String key) {
        return params.get(key);
    }

    public String getRawParam() {
        return rawParam;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(params);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof URLParam)) return false;
        URLParam that = (URLParam) obj;
        return Objects.equals(this.getParameters(), that.getParameters());
    }

    @Override
    public String toString() {
        if (StringUtils.isNotEmpty(rawParam)) {
            return rawParam;
        }
        if (params == null) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : new TreeMap<>(params).entrySet()) {
            if (StringUtils.isNotEmpty(entry.getKey())) {
                if (first) {
                    first = false;
                } else {
                    buf.append("&");
                }
                buf.append(entry.getKey());
                buf.append("=");
                buf.append(entry.getValue() == null ? "" : entry.getValue().trim());
            }
        }
        return buf.toString();
    }

    public static URLParam parse(String rawParam, boolean encoded, Map<String, String> extraParameters) {
        Map<String, String> parameters = URLStrParser.parseParams(rawParam, encoded);
        if (CollectionUtils.isNotEmptyMap(extraParameters)) {
            parameters.putAll(extraParameters);
        }
        return new URLParam(parameters, rawParam);
    }

    public static URLParam parse(String rawParam) {
        String[] parts = rawParam.split("&");
        Map<String, String> parameters = new HashMap<>((int) (parts.length/.75f) + 1);
        for (String part : parts) {
            part = part.trim();
            if (part.length() > 0) {
                int j = part.indexOf('=');
                if (j >= 0) {
                    String key = part.substring(0, j);
                    String value = part.substring(j + 1);
                    parameters.put(key, value);
                    // compatible with lower versions registering "default." keys
                    if (key.startsWith(DEFAULT_KEY_PREFIX)) {
                        parameters.putIfAbsent(key.substring(DEFAULT_KEY_PREFIX.length()), value);
                    }
                } else {
                    parameters.put(part, part);
                }
            }
        }
        return new URLParam(parameters, rawParam);
    }
}
