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
import org.apache.dubbo.common.url.component.param.DynamicParamTable;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;

public class URLParam implements Serializable {
    private static final long serialVersionUID = -1985165475234910535L;

    private final String rawParam;

    private final BitSet KEY;
    private final Map<Integer, Integer> VALUE;
    private final Map<String, String> EXTRA_PARAMS;

    //cache
    private transient Map<String, Map<String, String>> methodParameters;
    private transient long timestamp;

    private URLParam(BitSet key, Map<Integer, Integer> value, Map<String, String> extraParams, String rawParam) {
        this.KEY = key;
        this.VALUE = Collections.unmodifiableMap((value == null ? new HashMap<>() : new HashMap<>(value)));
        this.EXTRA_PARAMS = Collections.unmodifiableMap((extraParams == null ? new HashMap<>() : new HashMap<>(extraParams)));
        this.rawParam = rawParam;

        this.timestamp = System.currentTimeMillis();
    }

    public Map<String, Map<String, String>> getMethodParameters() {
        // TODO: delete it
        if (methodParameters == null) {
            methodParameters = Collections.unmodifiableMap(initMethodParameters(getParameters()));
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

    public static class URLParamMap implements Map<String, String> {
        private URLParam urlParam;

        public URLParamMap(URLParam urlParam) {
            this.urlParam = urlParam;
        }

        public static class Node implements Map.Entry<String, String> {
            private final String key;
            private String value;

            public Node(String key, String value) {
                this.key = key;
                this.value = value;
            }

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getValue() {
                return value;
            }

            @Override
            public String setValue(String value) {
                this.value = value;
                return value;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Node node = (Node) o;
                return Objects.equals(key, node.key) && Objects.equals(value, node.value);
            }

            @Override
            public int hashCode() {
                return Objects.hash(key, value);
            }
        }

        @Override
        public int size() {
            return urlParam.KEY.cardinality() + urlParam.EXTRA_PARAMS.size();
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public boolean containsKey(Object key) {
            if (key instanceof String) {
                return urlParam.hasParameter((String) key);
            } else {
                return false;
            }
        }

        @Override
        public boolean containsValue(Object value) {
            return values().contains(value);
        }

        @Override
        public String get(Object key) {
            if (key instanceof String) {
                return urlParam.getParameter((String) key);
            } else {
                return null;
            }
        }

        @Override
        public String put(String key, String value) {
            String previous = urlParam.getParameter(key);
            urlParam = urlParam.addParameter(key, value);
            return previous;
        }

        @Override
        public String remove(Object key) {
            if (key instanceof String) {
                String previous = urlParam.getParameter((String) key);
                urlParam = urlParam.removeParameters((String) key);
                return previous;
            } else {
                return null;
            }
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> m) {
            urlParam = urlParam.addParameters((Map<String, String>) m);
        }

        @Override
        public void clear() {
            urlParam = urlParam.clearParameters();
        }

        @Override
        public Set<String> keySet() {
            Set<String> set = new HashSet<>((int) ((urlParam.VALUE.size() + urlParam.EXTRA_PARAMS.size()) / 0.75) + 1);
            for (Entry<Integer, Integer> entry : urlParam.VALUE.entrySet()) {
                if (urlParam.KEY.get(entry.getKey())) {
                    set.add(DynamicParamTable.getKey(entry.getKey()));
                }
            }
            for (Entry<String, String> entry : urlParam.EXTRA_PARAMS.entrySet()) {
                set.add(entry.getKey());
            }
            return set;
        }

        @Override
        public Collection<String> values() {
            Set<String> set = new HashSet<>((int) ((urlParam.VALUE.size() + urlParam.EXTRA_PARAMS.size()) / 0.75) + 1);
            for (Entry<Integer, Integer> entry : urlParam.VALUE.entrySet()) {
                if (urlParam.KEY.get(entry.getKey())) {
                    set.add(DynamicParamTable.getValue(entry.getKey(), entry.getValue()));
                }
            }
            for (Entry<String, String> entry : urlParam.EXTRA_PARAMS.entrySet()) {
                set.add(entry.getValue());
            }
            return set;
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            Set<Entry<String, String>> set = new HashSet<>((int) ((urlParam.VALUE.size() + urlParam.EXTRA_PARAMS.size()) / 0.75) + 1);
            for (Entry<Integer, Integer> entry : urlParam.VALUE.entrySet()) {
                if (urlParam.KEY.get(entry.getKey())) {
                    set.add(new Node(DynamicParamTable.getKey(entry.getKey()), DynamicParamTable.getValue(entry.getKey(), entry.getValue())));
                }
            }
            for (Entry<String, String> entry : urlParam.EXTRA_PARAMS.entrySet()) {
                set.add(new Node(entry.getKey(), entry.getValue()));
            }
            return set;
        }

        public URLParam getUrlParam() {
            return urlParam;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            URLParamMap that = (URLParamMap) o;
            return Objects.equals(urlParam, that.urlParam);
        }

        @Override
        public int hashCode() {
            return Objects.hash(urlParam);
        }
    }

    public Map<String, String> getParameters() {
        return new URLParamMap(this);
    }

    public URLParam addParameter(String key, String value) {
        if (StringUtils.isEmpty(key)
                || StringUtils.isEmpty(value)) {
            return this;
        }
        return addParameters(Collections.singletonMap(key, value));
    }

    public URLParam addParameterIfAbsent(String key, String value) {
        if (StringUtils.isEmpty(key)
                || StringUtils.isEmpty(value)) {
            return this;
        }
        if (hasParameter(key)) {
            return this;
        }
        return addParametersIfAbsent(Collections.singletonMap(key, value));
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

        return doAddParameters(parameters, false);
    }

    public URLParam addParametersIfAbsent(Map<String, String> parameters) {
        return doAddParameters(parameters, true);
    }

    public URLParam doAddParameters(Map<String, String> parameters, boolean skipIfPresent) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return this;
        }
        BitSet newKey = null;
        Map<Integer, Integer> newValue = null;
        Map<String, String> newExtraParams = null;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (skipIfPresent && hasParameter(entry.getKey())) {
                continue;
            }
            Integer keyIndex = DynamicParamTable.getKeyIndex(entry.getKey());
            if (keyIndex == null) {
                if (newExtraParams == null) {
                    newExtraParams = new HashMap<>(EXTRA_PARAMS);
                }
                newExtraParams.put(entry.getKey(), entry.getValue());
            } else {
                if (newKey == null) {
                    newKey = (BitSet) KEY.clone();
                    newValue = new HashMap<>(VALUE);
                }
                newKey.set(keyIndex);
                newValue.put(keyIndex, DynamicParamTable.getValueIndex(entry.getKey(), entry.getValue()));
            }
        }
        if (newKey == null) {
            newKey = KEY;
        }
        if (newValue == null) {
            newValue = VALUE;
        }
        if (newExtraParams == null) {
            newExtraParams = EXTRA_PARAMS;
        }
        return new URLParam(newKey, newValue, newExtraParams, null);
    }

    public URLParam removeParameters(String... keys) {
        if (keys == null || keys.length == 0) {
            return this;
        }
        BitSet newKey = (BitSet) KEY.clone();
        Map<String, String> newExtraParams = null;
        for (String key : keys) {
            Integer keyIndex = DynamicParamTable.getKeyIndex(key);
            if (keyIndex == null) {
                if (EXTRA_PARAMS.containsKey(key)) {
                    if (newExtraParams == null) {
                        newExtraParams = new HashMap<>(EXTRA_PARAMS);
                    }
                    newExtraParams.remove(key);
                }
                continue;
            }
            newKey.clear(keyIndex);
        }
        if (newExtraParams == null) {
            newExtraParams = EXTRA_PARAMS;
        }
        return new URLParam(newKey, VALUE, newExtraParams, null);
    }

    public URLParam clearParameters() {
        // TODO cache
        return new URLParam(new BitSet(0), Collections.emptyMap(), Collections.emptyMap(), "");
    }

    public boolean hasParameter(String key) {
        Integer keyIndex = DynamicParamTable.getKeyIndex(key);
        if (keyIndex == null) {
            return EXTRA_PARAMS.containsKey(key);
        }
        return KEY.get(keyIndex);
    }

    public String getParameter(String key) {
        Integer keyIndex = DynamicParamTable.getKeyIndex(key);
        if (keyIndex == null) {
            if (EXTRA_PARAMS.containsKey(key)) {
                return EXTRA_PARAMS.get(key);
            }
            return null;
        }
        if (KEY.get(keyIndex)) {
            String value = DynamicParamTable.getValue(keyIndex, VALUE.get(keyIndex));
            if (StringUtils.isEmpty(value)) {
                return DynamicParamTable.getDefaultValue(keyIndex);
            } else {
                return value;
            }
        }
        return null;
    }

    public String getRawParam() {
        if (StringUtils.isNotEmpty(rawParam)) {
            return rawParam;
        } else {
            return toString();
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        URLParam urlParam = (URLParam) o;
        return Objects.equals(KEY, urlParam.KEY)
                && Objects.equals(VALUE, urlParam.VALUE)
                && Objects.equals(EXTRA_PARAMS, urlParam.EXTRA_PARAMS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(KEY, VALUE, EXTRA_PARAMS);
    }

    @Override
    public String toString() {
        if (StringUtils.isNotEmpty(rawParam)) {
            return rawParam;
        }
        if (getParameters() == null) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : new TreeMap<>(getParameters()).entrySet()) {
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
        return parse(parameters, rawParam);
    }

    public static URLParam parse(Map<String, String> params) {
        return parse(params, null);
    }

    public static URLParam parse(String rawParam) {
        String[] parts = rawParam.split("&");

        BitSet keyBit = new BitSet((int) (parts.length / .75f) + 1);
        Map<Integer, Integer> valueMap = new HashMap<>((int) (parts.length / .75f) + 1);
        Map<String, String> extraParam = new HashMap<>((int) (parts.length / .75f) + 1);

        for (String part : parts) {
            part = part.trim();
            if (part.length() > 0) {
                int j = part.indexOf('=');
                if (j >= 0) {
                    String key = part.substring(0, j);
                    String value = part.substring(j + 1);
                    addParameter(keyBit, valueMap, extraParam, key, value, false);
                    // compatible with lower versions registering "default." keys
                    if (key.startsWith(DEFAULT_KEY_PREFIX)) {
                        addParameter(keyBit, valueMap, extraParam, key.substring(DEFAULT_KEY_PREFIX.length()), value, true);
                    }
                } else {
                    addParameter(keyBit, valueMap, extraParam, part, part, false);
                }
            }
        }
        return new URLParam(keyBit, new HashMap<>(valueMap), new HashMap<>(extraParam), rawParam);
    }


    public static URLParam parse(Map<String, String> params, String rawParam) {
        if (CollectionUtils.isNotEmptyMap(params)) {
            BitSet keyBit = new BitSet((int) (params.size() / .75f) + 1);
            Map<Integer, Integer> valueMap = new HashMap<>((int) (params.size() / .75f) + 1);
            Map<String, String> extraParam = new HashMap<>((int) (params.size() / .75f) + 1);

            for (Map.Entry<String, String> entry : params.entrySet()) {
                addParameter(keyBit, valueMap, extraParam, entry.getKey(), entry.getValue(), false);
            }
            return new URLParam(keyBit, new HashMap<>(valueMap), new HashMap<>(extraParam), rawParam);
        } else {
            return new URLParam(new BitSet(0), Collections.emptyMap(), Collections.emptyMap(), rawParam);
        }
    }

    private static void addParameter(BitSet keyBit, Map<Integer, Integer> valueMap, Map<String, String> extraParam,
                                     String key, String value, boolean skipIfPresent) {
        Integer keyIndex = DynamicParamTable.getKeyIndex(key);
        if (skipIfPresent) {
            if (keyIndex == null) {
                if (extraParam.containsKey(key)) {
                    return;
                }
            } else {
                if (keyBit.get(keyIndex)) {
                    return;
                }
            }
        }

        if (keyIndex == null) {
            extraParam.put(key, value);
        } else {
            if (!DynamicParamTable.isDefaultValue(key, value)) {
                valueMap.put(keyIndex, DynamicParamTable.getValueIndex(key, value));
            }
            keyBit.set(keyIndex);
        }
    }
}
