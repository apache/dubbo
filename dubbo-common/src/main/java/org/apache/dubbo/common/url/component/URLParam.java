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
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;

/**
 * A class which store parameters for {@link URL}
 * <br/>
 * Using {@link DynamicParamTable} to compress common keys (i.e. side, version)
 * <br/>
 * {@link DynamicParamTable} allow to use only two integer value named `key` and
 * `value-offset` to find a unique string to string key-pair. Also, `value-offset`
 * is not required if the real value is the default value.
 * <br/>
 * URLParam should operate as Copy-On-Write, each modify actions will return a new Object
 *
 * @since 3.0
 */
public class URLParam implements Serializable {
    private static final long serialVersionUID = -1985165475234910535L;

    /**
     * Maximum size of key-pairs requested using array moving to add into URLParam.
     * If user request like addParameter for only one key-pair, adding value into a array
     * on moving is more efficient. However when add more than ADD_PARAMETER_ON_MOVE_THRESHOLD
     * size of key-pairs, recover compressed array back to map can reduce operation count
     * when putting objects.
     */
    private static final int ADD_PARAMETER_ON_MOVE_THRESHOLD = 1;

    /**
     * the original parameters string, empty if parameters have been modified or init by {@link Map}
     */
    private final String rawParam;

    /**
     * using bit to save if index exist even if value is default value
     */
    private final BitSet KEY;

    /**
     * using bit to save if value is default value (reduce VALUE size)
     */
    private final BitSet DEFAULT_KEY;

    /**
     * a compressed (those key not exist or value is default value are bring removed in this array) array which contains value-offset
     */
    private final int[] VALUE;

    /**
     * store extra parameters which key not match in {@link DynamicParamTable}
     */
    private final Map<String, String> EXTRA_PARAMS;

    //cache
    private transient Map<String, Map<String, String>> methodParameters;
    private transient long timestamp;

    private final static URLParam EMPTY_PARAM = new URLParam(new BitSet(0), Collections.emptyMap(), Collections.emptyMap(), "");

    private URLParam() {
        this.rawParam = null;
        this.KEY = null;
        this.DEFAULT_KEY = null;
        this.VALUE = null;
        this.EXTRA_PARAMS = null;
    }

    private URLParam(BitSet key, Map<Integer, Integer> value, Map<String, String> extraParams, String rawParam) {
        this.KEY = key;
        this.DEFAULT_KEY = new BitSet(KEY.size());
        this.VALUE = new int[value.size()];

        // compress VALUE
        for (int i = key.nextSetBit(0), offset = 0; i >= 0; i = key.nextSetBit(i + 1)) {
            if (value.containsKey(i)) {
                VALUE[offset++] = value.get(i);
            } else {
                DEFAULT_KEY.set(i);
            }
        }

        this.EXTRA_PARAMS = Collections.unmodifiableMap((extraParams == null ? new HashMap<>() : new HashMap<>(extraParams)));
        this.rawParam = rawParam;

        this.timestamp = System.currentTimeMillis();
    }

    private URLParam(BitSet key, BitSet defaultKey, int[] value, Map<String, String> extraParams, String rawParam) {
        this.KEY = key;
        this.DEFAULT_KEY = defaultKey;

        this.VALUE = value;

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

    /**
     * An embedded Map adapt to URLParam
     * <br/>
     * copy-on-write mode, urlParam reference will be changed after modify actions.
     * If wishes to get the result after modify, please use {@link URLParamMap#getUrlParam()}
     */
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
                throw new UnsupportedOperationException();
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
            Set<String> set = new HashSet<>((int) ((urlParam.VALUE.length + urlParam.EXTRA_PARAMS.size()) / 0.75) + 1);
            for (int i = urlParam.KEY.nextSetBit(0); i >= 0; i = urlParam.KEY.nextSetBit(i + 1)) {
                set.add(DynamicParamTable.getKey(i));
            }
            for (Entry<String, String> entry : urlParam.EXTRA_PARAMS.entrySet()) {
                set.add(entry.getKey());
            }
            return Collections.unmodifiableSet(set);
        }

        @Override
        public Collection<String> values() {
            Set<String> set = new HashSet<>((int) ((urlParam.VALUE.length + urlParam.EXTRA_PARAMS.size()) / 0.75) + 1);
            for (int i = urlParam.KEY.nextSetBit(0); i >= 0; i = urlParam.KEY.nextSetBit(i + 1)) {
                String value;
                if (urlParam.DEFAULT_KEY.get(i)) {
                    value = DynamicParamTable.getDefaultValue(i);
                } else {
                    int offset = urlParam.keyIndexToOffset(i);
                    value = DynamicParamTable.getValue(i, offset);
                }
                set.add(value);
            }

            for (Entry<String, String> entry : urlParam.EXTRA_PARAMS.entrySet()) {
                set.add(entry.getValue());
            }
            return Collections.unmodifiableSet(set);
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            Set<Entry<String, String>> set = new HashSet<>((int) ((urlParam.KEY.cardinality() + urlParam.EXTRA_PARAMS.size()) / 0.75) + 1);
            for (int i = urlParam.KEY.nextSetBit(0); i >= 0; i = urlParam.KEY.nextSetBit(i + 1)) {
                String value;
                if (urlParam.DEFAULT_KEY.get(i)) {
                    value = DynamicParamTable.getDefaultValue(i);
                } else {
                    int offset = urlParam.keyIndexToOffset(i);
                    value = DynamicParamTable.getValue(i, offset);
                }
                set.add(new Node(DynamicParamTable.getKey(i), value));
            }

            for (Entry<String, String> entry : urlParam.EXTRA_PARAMS.entrySet()) {
                set.add(new Node(entry.getKey(), entry.getValue()));
            }
            return Collections.unmodifiableSet(set);
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

    /**
     * Get a Map like URLParam
     *
     * @return a {@link URLParamMap} adapt to URLParam
     */
    public Map<String, String> getParameters() {
        return new URLParamMap(this);
    }

    /**
     * Add parameters to a new URLParam.
     *
     * @param key   key
     * @param value value
     * @return A new URLParam
     */
    public URLParam addParameter(String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }
        return addParameters(Collections.singletonMap(key, value));
    }

    /**
     * Add absent parameters to a new URLParam.
     *
     * @param key   key
     * @param value value
     * @return A new URLParam
     */
    public URLParam addParameterIfAbsent(String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return this;
        }
        if (hasParameter(key)) {
            return this;
        }
        return addParametersIfAbsent(Collections.singletonMap(key, value));
    }

    /**
     * Add parameters to a new URLParam.
     * If key-pair is present, this will cover it.
     *
     * @param parameters parameters in key-value pairs
     * @return A new URLParam
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

    /**
     * Add absent parameters to a new URLParam.
     *
     * @param parameters parameters in key-value pairs
     * @return A new URL
     */
    public URLParam addParametersIfAbsent(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return this;
        }

        return doAddParameters(parameters, true);
    }

    private URLParam doAddParameters(Map<String, String> parameters, boolean skipIfPresent) {
        // lazy init, null if no modify
        BitSet newKey = null;
        BitSet defaultKey = null;
        int[] newValueArray = null;
        Map<Integer, Integer> newValueMap = null;
        Map<String, String> newExtraParams = null;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (skipIfPresent && hasParameter(entry.getKey())) {
                continue;
            }
            Integer keyIndex = DynamicParamTable.getKeyIndex(entry.getKey());
            if (keyIndex == null) {
                // entry key is not present in DynamicParamTable, add it to EXTRA_PARAMS
                if (newExtraParams == null) {
                    newExtraParams = new HashMap<>(EXTRA_PARAMS);
                }
                newExtraParams.put(entry.getKey(), entry.getValue());
            } else {
                if (newKey == null) {
                    newKey = (BitSet) KEY.clone();
                }
                newKey.set(keyIndex);

                if (parameters.size() > ADD_PARAMETER_ON_MOVE_THRESHOLD) {
                    // recover VALUE back to Map
                    if (newValueMap == null) {
                        newValueMap = recoverCompressedValue();
                    }
                    newValueMap.put(keyIndex, DynamicParamTable.getValueIndex(entry.getKey(), entry.getValue()));
                } else if (!DynamicParamTable.isDefaultValue(entry.getKey(), entry.getValue())) {
                    // add parameter by moving array
                    newValueArray = addByMove(VALUE, KEY.get(0, keyIndex).cardinality(), DynamicParamTable.getValueIndex(entry.getKey(), entry.getValue()));
                } else {
                    // value is default value, add to defaultKey directly
                    if (defaultKey == null) {
                        defaultKey = (BitSet) DEFAULT_KEY.clone();
                    }
                    defaultKey.set(keyIndex);
                }
            }
        }
        if (newKey == null) {
            newKey = KEY;
        }
        if (defaultKey == null) {
            defaultKey = DEFAULT_KEY;
        }
        if (newValueArray == null && newValueMap == null) {
            newValueArray = VALUE;
        }
        if (newExtraParams == null) {
            newExtraParams = EXTRA_PARAMS;
        }
        if (newValueMap == null) {
            return new URLParam(newKey, defaultKey, newValueArray, newExtraParams, null);
        } else {
            return new URLParam(newKey, newValueMap, newExtraParams, null);
        }
    }

    private Map<Integer, Integer> recoverCompressedValue() {
        Map<Integer, Integer> map = new HashMap<>((int) (KEY.size() / 0.75) + 1);
        for (int i = KEY.nextSetBit(0), offset = 0; i >= 0; i = KEY.nextSetBit(i + 1)) {
            if (!DEFAULT_KEY.get(i)) {
                map.put(i, VALUE[offset++]);
            }
        }
        return map;
    }

    private int[] addByMove(int[] array, int index, int value) {
        if (index < 0 || index > array.length) {
            throw new IllegalArgumentException();
        }
        // copy-on-write
        int[] result = new int[array.length + 1];

        System.arraycopy(array, 0, result, 0, index);
        result[index] = value;
        System.arraycopy(array, index, result, index + 1, array.length - index);

        return result;
    }

    /**
     * remove specified parameters in URLParam
     *
     * @param keys keys to being removed
     * @return A new URLParam
     */
    public URLParam removeParameters(String... keys) {
        if (keys == null || keys.length == 0) {
            return this;
        }
        // lazy init, null if no modify
        BitSet newKey = null;
        BitSet defaultKey = null;
        int[] newValueArray = null;
        Map<String, String> newExtraParams = null;
        for (String key : keys) {
            Integer keyIndex = DynamicParamTable.getKeyIndex(key);
            if (keyIndex != null && KEY.get(keyIndex)) {
                if (newKey == null) {
                    newKey = (BitSet) KEY.clone();
                }
                newKey.clear(keyIndex);
                if (DEFAULT_KEY.get(keyIndex)) {
                    // is default value, remove in DEFAULT_KEY
                    if (defaultKey == null) {
                        defaultKey = (BitSet) DEFAULT_KEY.clone();
                    }
                    defaultKey.clear(keyIndex);
                } else {
                    // which offset is in VALUE array, set value as -1, compress in the end
                    if (newValueArray == null) {
                        newValueArray = new int[VALUE.length];
                        System.arraycopy(VALUE, 0, newValueArray, 0, VALUE.length);
                    }
                    // KEY is immutable
                    newValueArray[KEY.get(0, keyIndex).cardinality()] = -1;
                }
            }
            if (EXTRA_PARAMS.containsKey(key)) {
                if (newExtraParams == null) {
                    newExtraParams = new HashMap<>(EXTRA_PARAMS);
                }
                newExtraParams.remove(key);
            }
            // ignore if key is absent
        }
        if (newKey == null) {
            newKey = KEY;
        }
        if (defaultKey == null) {
            defaultKey = DEFAULT_KEY;
        }
        if (newValueArray == null) {
            newValueArray = VALUE;
        } else {
            // remove -1 value
            newValueArray = compressArray(newValueArray);
        }
        if (newExtraParams == null) {
            newExtraParams = EXTRA_PARAMS;
        }
        if (newKey.cardinality() + newExtraParams.size() == 0) {
            // empty, directly return cache
            return EMPTY_PARAM;
        } else {
            return new URLParam(newKey, defaultKey, newValueArray, newExtraParams, null);
        }
    }

    private int[] compressArray(int[] array) {
        int total = 0;
        for (int i : array) {
            if (i > -1) {
                total++;
            }
        }
        if (total == 0) {
            return new int[0];
        }

        int[] result = new int[total];
        for (int i = 0, offset = 0; i < array.length; i++) {
            // skip if value if less than 0
            if (array[i] > -1) {
                result[offset++] = array[i];
            }
        }
        return result;
    }

    /**
     * remove all of the parameters in URLParam
     *
     * @return An empty URLParam
     */
    public URLParam clearParameters() {
        return EMPTY_PARAM;
    }

    /**
     * check if specified key is present in URLParam
     *
     * @param key specified key
     * @return present or not
     */
    public boolean hasParameter(String key) {
        Integer keyIndex = DynamicParamTable.getKeyIndex(key);
        if (keyIndex == null) {
            return EXTRA_PARAMS.containsKey(key);
        }
        return KEY.get(keyIndex);
    }

    /**
     * get value of specified key in URLParam
     *
     * @param key specified key
     * @return value, null if key is absent
     */
    public String getParameter(String key) {
        Integer keyIndex = DynamicParamTable.getKeyIndex(key);
        if (keyIndex == null) {
            if (EXTRA_PARAMS.containsKey(key)) {
                return EXTRA_PARAMS.get(key);
            }
            return null;
        }
        if (KEY.get(keyIndex)) {
            String value;
            if (DEFAULT_KEY.get(keyIndex)) {
                value = DynamicParamTable.getDefaultValue(keyIndex);
            } else {
                int offset = keyIndexToOffset(keyIndex);
                value = DynamicParamTable.getValue(keyIndex, offset);
            }
            if (StringUtils.isEmpty(value)) {
                // Forward compatible, make sure key dynamic increment can work.
                // In that case, some values which are proceed before increment will set in EXTRA_PARAMS.
                return EXTRA_PARAMS.get(key);
            } else {
                return value;
            }
        }
        return null;
    }

    private int keyIndexToOffset(int keyIndex) {
        int arrayOffset = KEY.get(0, keyIndex).cardinality();
        return VALUE[arrayOffset];
    }

    /**
     * get raw string like parameters
     *
     * @return raw string like parameters
     */
    public String getRawParam() {
        if (StringUtils.isNotEmpty(rawParam)) {
            return rawParam;
        } else {
            // empty if parameters have been modified or init by Map
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
                && Objects.equals(DEFAULT_KEY, urlParam.DEFAULT_KEY)
                && Arrays.equals(VALUE, urlParam.VALUE)
                && Objects.equals(EXTRA_PARAMS, urlParam.EXTRA_PARAMS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(KEY, DEFAULT_KEY, Arrays.hashCode(VALUE), EXTRA_PARAMS);
    }

    @Override
    public String toString() {
        if (StringUtils.isNotEmpty(rawParam)) {
            return rawParam;
        }
        if ((KEY.cardinality() + EXTRA_PARAMS.size()) == 0) {
            return "";
        }

        StringJoiner stringJoiner = new StringJoiner("&");
        for (int i = KEY.nextSetBit(0); i >= 0; i = KEY.nextSetBit(i + 1)) {
            String key = DynamicParamTable.getKey(i);
            String value = DEFAULT_KEY.get(i) ?
                    DynamicParamTable.getDefaultValue(i) : DynamicParamTable.getValue(i, keyIndexToOffset(i));
            value = value == null ? "" : value.trim();
            stringJoiner.add(String.format("%s=%s", key, value));
        }
        for (Map.Entry<String, String> entry : EXTRA_PARAMS.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            value = value == null ? "" : value.trim();
            stringJoiner.add(String.format("%s=%s", key, value));
        }

        return stringJoiner.toString();
    }

    /**
     * Parse URLParam
     * Init URLParam by constructor is not allowed
     * rawParam field in result will be null while {@link URLParam#getRawParam()} will automatically create it
     *
     * @param params params map added into URLParam
     * @return a new URLParam
     */
    public static URLParam parse(Map<String, String> params) {
        return parse(params, null);
    }

    /**
     * Parse URLParam
     * Init URLParam by constructor is not allowed
     *
     * @param rawParam original rawParam string
     * @param encoded if parameters are URL encoded
     * @param extraParameters extra parameters to add into URLParam
     * @return a new URLParam
     */
    public static URLParam parse(String rawParam, boolean encoded, Map<String, String> extraParameters) {
        Map<String, String> parameters = URLStrParser.parseParams(rawParam, encoded);
        if (CollectionUtils.isNotEmptyMap(extraParameters)) {
            parameters.putAll(extraParameters);
        }
        return parse(parameters, rawParam);
    }

    /**
     * Parse URLParam
     * Init URLParam by constructor is not allowed
     *
     * @param rawParam original rawParam string
     * @return a new URLParam
     */
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
        return new URLParam(keyBit, valueMap, extraParam, rawParam);
    }

    /**
     * Parse URLParam
     * Init URLParam by constructor is not allowed
     *
     * @param params params map added into URLParam
     * @param rawParam original rawParam string, directly add to rawParam field,
     *                 will not effect real key-pairs store in URLParam.
     *                 Please make sure it can correspond with params or will
     *                 cause unexpected result when calling {@link URLParam#getRawParam()}
     *                 and {@link URLParam#toString()} ()}. If you not sure, you can call
     *                 {@link URLParam#parse(String)} to init.
     * @return a new URLParam
     */
    public static URLParam parse(Map<String, String> params, String rawParam) {
        if (CollectionUtils.isNotEmptyMap(params)) {
            BitSet keyBit = new BitSet((int) (params.size() / .75f) + 1);
            Map<Integer, Integer> valueMap = new HashMap<>((int) (params.size() / .75f) + 1);
            Map<String, String> extraParam = new HashMap<>((int) (params.size() / .75f) + 1);

            for (Map.Entry<String, String> entry : params.entrySet()) {
                addParameter(keyBit, valueMap, extraParam, entry.getKey(), entry.getValue(), false);
            }
            return new URLParam(keyBit, valueMap, extraParam, rawParam);
        } else {
            return EMPTY_PARAM;
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
