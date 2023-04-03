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

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;

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
 * <br/>
 * <p>
 * NOTE: URLParam is not support serialization! {@link DynamicParamTable} is related with
 * current running environment. If you want to make URL as a parameter, please call
 * {@link URL#toSerializableURL()} to create {@link URLPlainParam} instead.
 *
 * @since 3.0
 */
public class URLParam {

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
     * an array which contains value-offset
     */
    private final int[] VALUE;

    /**
     * store extra parameters which key not match in {@link DynamicParamTable}
     */
    private final Map<String, String> EXTRA_PARAMS;

    /**
     * store method related parameters
     * <p>
     * K - key
     * V -
     * K - method
     * V - value
     * <p>
     * e.g. method1.mock=true => ( mock, (method1, true) )
     */
    private final Map<String, Map<String, String>> METHOD_PARAMETERS;

    private transient long timestamp;

    /**
     * Whether to enable DynamicParamTable compression
     */
    protected boolean enableCompressed;

    private final static URLParam EMPTY_PARAM = new URLParam(new BitSet(0), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), "");

    protected URLParam() {
        this.rawParam = null;
        this.KEY = null;
        this.VALUE = null;
        this.EXTRA_PARAMS = null;
        this.METHOD_PARAMETERS = null;
        this.enableCompressed = true;
    }

    protected URLParam(BitSet key, Map<Integer, Integer> value, Map<String, String> extraParams, Map<String, Map<String, String>> methodParameters, String rawParam) {
        this.KEY = key;
        this.VALUE = new int[value.size()];
        for (int i = key.nextSetBit(0), offset = 0; i >= 0; i = key.nextSetBit(i + 1)) {
            if (value.containsKey(i)) {
                VALUE[offset++] = value.get(i);
            } else {
                throw new IllegalArgumentException();
            }
        }
        this.EXTRA_PARAMS = Collections.unmodifiableMap((extraParams == null ? new HashMap<>() : new HashMap<>(extraParams)));
        this.METHOD_PARAMETERS = Collections.unmodifiableMap((methodParameters == null) ? Collections.emptyMap() : new LinkedHashMap<>(methodParameters));
        this.rawParam = rawParam;

        this.timestamp = System.currentTimeMillis();
        this.enableCompressed = true;
    }

    protected URLParam(BitSet key, int[] value, Map<String, String> extraParams, Map<String, Map<String, String>> methodParameters, String rawParam) {
        this.KEY = key;
        this.VALUE = value;
        this.EXTRA_PARAMS = Collections.unmodifiableMap((extraParams == null ? new HashMap<>() : new HashMap<>(extraParams)));
        this.METHOD_PARAMETERS = Collections.unmodifiableMap((methodParameters == null) ? Collections.emptyMap() : new LinkedHashMap<>(methodParameters));
        this.rawParam = rawParam;
        this.timestamp = System.currentTimeMillis();
        this.enableCompressed = true;
    }

    /**
     * Weather there contains some parameter match method
     *
     * @param method method name
     * @return contains or not
     */
    public boolean hasMethodParameter(String method) {
        if (method == null) {
            return false;
        }

        String methodsString = getParameter(METHODS_KEY);
        if (StringUtils.isNotEmpty(methodsString)) {
            if (!methodsString.contains(method)) {
                return false;
            }
        }

        for (Map.Entry<String, Map<String, String>> methods : METHOD_PARAMETERS.entrySet()) {
            if (methods.getValue().containsKey(method)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get method related parameter. If not contains, use getParameter(key) instead.
     * Specially, in some situation like `method1.1.callback=true`, key is `1.callback`.
     *
     * @param method method name
     * @param key    key
     * @return value
     */
    public String getMethodParameter(String method, String key) {
        String strictResult = getMethodParameterStrict(method, key);
        return StringUtils.isNotEmpty(strictResult) ? strictResult : getParameter(key);
    }

    /**
     * Get method related parameter. If not contains, return null.
     * Specially, in some situation like `method1.1.callback=true`, key is `1.callback`.
     *
     * @param method method name
     * @param key    key
     * @return value
     */
    public String getMethodParameterStrict(String method, String key) {
        String methodsString = getParameter(METHODS_KEY);
        if (StringUtils.isNotEmpty(methodsString)) {
            if (!methodsString.contains(method)) {
                return null;
            }
        }

        Map<String, String> methodMap = METHOD_PARAMETERS.get(key);
        if (CollectionUtils.isNotEmptyMap(methodMap)) {
            return methodMap.get(method);
        } else {
            return null;
        }
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
            Set<String> set = new LinkedHashSet<>((int) ((urlParam.VALUE.length + urlParam.EXTRA_PARAMS.size()) / 0.75) + 1);
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
            Set<String> set = new LinkedHashSet<>((int) ((urlParam.VALUE.length + urlParam.EXTRA_PARAMS.size()) / 0.75) + 1);
            for (int i = urlParam.KEY.nextSetBit(0); i >= 0; i = urlParam.KEY.nextSetBit(i + 1)) {
                String value;
                int offset = urlParam.keyIndexToOffset(i);
                value = DynamicParamTable.getValue(i, offset);
                set.add(value);
            }

            for (Entry<String, String> entry : urlParam.EXTRA_PARAMS.entrySet()) {
                set.add(entry.getValue());
            }
            return Collections.unmodifiableSet(set);
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            Set<Entry<String, String>> set = new LinkedHashSet<>((int) ((urlParam.KEY.cardinality() + urlParam.EXTRA_PARAMS.size()) / 0.75) + 1);
            for (int i = urlParam.KEY.nextSetBit(0); i >= 0; i = urlParam.KEY.nextSetBit(i + 1)) {
                String value;
                int offset = urlParam.keyIndexToOffset(i);
                value = DynamicParamTable.getValue(i, offset);
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
     * Get any method related parameter which match key
     *
     * @param key key
     * @return result ( if any, random choose one )
     */
    public String getAnyMethodParameter(String key) {
        Map<String, String> methodMap = METHOD_PARAMETERS.get(key);
        if (CollectionUtils.isNotEmptyMap(methodMap)) {
            String methods = getParameter(METHODS_KEY);
            if (StringUtils.isNotEmpty(methods)) {
                for (String method : methods.split(",")) {
                    String value = methodMap.get(method);
                    if (StringUtils.isNotEmpty(value)) {
                        return value;
                    }
                }
            } else {
                return methodMap.values().iterator().next();
            }
        }
        return null;
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
        Map<String, String> urlParamMap = getParameters();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String value = urlParamMap.get(entry.getKey());
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
        int[] newValueArray = null;
        Map<Integer, Integer> newValueMap = null;
        Map<String, String> newExtraParams = null;
        Map<String, Map<String, String>> newMethodParams = null;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (skipIfPresent && hasParameter(entry.getKey())) {
                continue;
            }
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            int keyIndex = DynamicParamTable.getKeyIndex(enableCompressed, entry.getKey());
            if (keyIndex < 0) {
                // entry key is not present in DynamicParamTable, add it to EXTRA_PARAMS
                if (newExtraParams == null) {
                    newExtraParams = new HashMap<>(EXTRA_PARAMS);
                }
                newExtraParams.put(entry.getKey(), entry.getValue());
                String[] methodSplit = entry.getKey().split("\\.");
                if (methodSplit.length == 2) {
                    if (newMethodParams == null) {
                        newMethodParams = new HashMap<>(METHOD_PARAMETERS);
                    }
                    Map<String, String> methodMap = newMethodParams.computeIfAbsent(methodSplit[1], (k) -> new HashMap<>());
                    methodMap.put(methodSplit[0], entry.getValue());
                }
            } else {
                if (KEY.get(keyIndex)) {
                    // contains key, replace value
                    if (parameters.size() > ADD_PARAMETER_ON_MOVE_THRESHOLD) {
                        // recover VALUE back to Map, use map to replace key pair
                        if (newValueMap == null) {
                            newValueMap = recoverValue();
                        }
                        newValueMap.put(keyIndex, DynamicParamTable.getValueIndex(entry.getKey(), entry.getValue()));
                    } else {
                        newValueArray = replaceOffset(VALUE, keyIndexToIndex(KEY, keyIndex), DynamicParamTable.getValueIndex(entry.getKey(), entry.getValue()));
                    }
                } else {
                    // key is absent, add it
                    if (newKey == null) {
                        newKey = (BitSet) KEY.clone();
                    }
                    newKey.set(keyIndex);

                    if (parameters.size() > ADD_PARAMETER_ON_MOVE_THRESHOLD) {
                        // recover VALUE back to Map
                        if (newValueMap == null) {
                            newValueMap = recoverValue();
                        }
                        newValueMap.put(keyIndex, DynamicParamTable.getValueIndex(entry.getKey(), entry.getValue()));
                    } else {
                        // add parameter by moving array, only support for adding once
                        newValueArray = addByMove(VALUE, keyIndexToIndex(newKey, keyIndex), DynamicParamTable.getValueIndex(entry.getKey(), entry.getValue()));
                    }
                }
            }
        }
        if (newKey == null) {
            newKey = KEY;
        }
        if (newValueArray == null && newValueMap == null) {
            newValueArray = VALUE;
        }
        if (newExtraParams == null) {
            newExtraParams = EXTRA_PARAMS;
        }
        if (newMethodParams == null) {
            newMethodParams = METHOD_PARAMETERS;
        }
        if (newValueMap == null) {
            return new URLParam(newKey, newValueArray, newExtraParams, newMethodParams, null);
        } else {
            return new URLParam(newKey, newValueMap, newExtraParams, newMethodParams, null);
        }
    }

    private Map<Integer, Integer> recoverValue() {
        Map<Integer, Integer> map = new HashMap<>((int) (KEY.size() / 0.75) + 1);
        for (int i = KEY.nextSetBit(0), offset = 0; i >= 0; i = KEY.nextSetBit(i + 1)) {
            map.put(i, VALUE[offset++]);
        }
        return map;
    }

    private int[] addByMove(int[] array, int index, Integer value) {
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

    private int[] replaceOffset(int[] array, int index, Integer value) {
        if (index < 0 || index > array.length) {
            throw new IllegalArgumentException();
        }
        // copy-on-write
        int[] result = new int[array.length];

        System.arraycopy(array, 0, result, 0, array.length);
        result[index] = value;

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
        int[] newValueArray = null;
        Map<String, String> newExtraParams = null;
        Map<String, Map<String, String>> newMethodParams = null;
        for (String key : keys) {
            int keyIndex = DynamicParamTable.getKeyIndex(enableCompressed, key);
            if (keyIndex >= 0 && KEY.get(keyIndex)) {
                if (newKey == null) {
                    newKey = (BitSet) KEY.clone();
                }
                newKey.clear(keyIndex);
                // which offset is in VALUE array, set value as -1, compress in the end
                if (newValueArray == null) {
                    newValueArray = new int[VALUE.length];
                    System.arraycopy(VALUE, 0, newValueArray, 0, VALUE.length);
                }
                // KEY is immutable
                newValueArray[keyIndexToIndex(KEY, keyIndex)] = -1;
            }
            if (EXTRA_PARAMS.containsKey(key)) {
                if (newExtraParams == null) {
                    newExtraParams = new HashMap<>(EXTRA_PARAMS);
                }
                newExtraParams.remove(key);

                String[] methodSplit = key.split("\\.");
                if (methodSplit.length == 2) {
                    if (newMethodParams == null) {
                        newMethodParams = new HashMap<>(METHOD_PARAMETERS);
                    }
                    Map<String, String> methodMap = newMethodParams.get(methodSplit[1]);
                    if (CollectionUtils.isNotEmptyMap(methodMap)) {
                        methodMap.remove(methodSplit[0]);
                    }
                }
            }
            // ignore if key is absent
        }
        if (newKey == null) {
            newKey = KEY;
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
        if (newMethodParams == null) {
            newMethodParams = METHOD_PARAMETERS;
        }
        if (newKey.cardinality() + newExtraParams.size() == 0) {
            // empty, directly return cache
            return EMPTY_PARAM;
        } else {
            return new URLParam(newKey, newValueArray, newExtraParams, newMethodParams, null);
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
        int keyIndex = DynamicParamTable.getKeyIndex(enableCompressed, key);
        if (keyIndex < 0) {
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
        int keyIndex = DynamicParamTable.getKeyIndex(enableCompressed, key);
        if (keyIndex < 0) {
            return EXTRA_PARAMS.get(key);
        }
        if (KEY.get(keyIndex)) {
            String value;
            int offset = keyIndexToOffset(keyIndex);
            value = DynamicParamTable.getValue(keyIndex, offset);

            return value;
//            if (StringUtils.isEmpty(value)) {
//                // Forward compatible, make sure key dynamic increment can work.
//                // In that case, some values which are proceed before increment will set in EXTRA_PARAMS.
//                return EXTRA_PARAMS.get(key);
//            } else {
//                return value;
//            }
        }
        return null;
    }


    private int keyIndexToIndex(BitSet key, int keyIndex) {
        return key.get(0, keyIndex).cardinality();
    }

    private int keyIndexToOffset(int keyIndex) {
        int arrayOffset = keyIndexToIndex(KEY, keyIndex);
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

    protected Map<String, Map<String, String>> getMethodParameters() {
        return METHOD_PARAMETERS;
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

        if (Objects.equals(KEY, urlParam.KEY)
            && Arrays.equals(VALUE, urlParam.VALUE)) {
            if (CollectionUtils.isNotEmptyMap(EXTRA_PARAMS)) {
                if (CollectionUtils.isEmptyMap(urlParam.EXTRA_PARAMS) || EXTRA_PARAMS.size() != urlParam.EXTRA_PARAMS.size()) {
                    return false;
                }
                for (Map.Entry<String, String> entry : EXTRA_PARAMS.entrySet()) {
                    if (TIMESTAMP_KEY.equals(entry.getKey())) {
                        continue;
                    }
                    if (!entry.getValue().equals(urlParam.EXTRA_PARAMS.get(entry.getKey()))) {
                        return false;
                    }
                }
                return true;
            }
            return CollectionUtils.isEmptyMap(urlParam.EXTRA_PARAMS);
        }
        return false;
    }

    private int hashCodeCache = -1;

    @Override
    public int hashCode() {
        if (hashCodeCache == -1) {
            for (Map.Entry<String, String> entry : EXTRA_PARAMS.entrySet()) {
                if (!TIMESTAMP_KEY.equals(entry.getKey())) {
                    hashCodeCache = hashCodeCache * 31 + Objects.hashCode(entry);
                }
            }
            for (Integer value : VALUE) {
                hashCodeCache = hashCodeCache * 31 + value;
            }
            hashCodeCache = hashCodeCache * 31 + ((KEY == null) ? 0 : KEY.hashCode());
        }
        return hashCodeCache;
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
            String value = DynamicParamTable.getValue(i, keyIndexToOffset(i));
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
     * @param rawParam        original rawParam string
     * @param encoded         if parameters are URL encoded
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

        int capacity = (int) (parts.length / .75f) + 1;
        BitSet keyBit = new BitSet(capacity);
        Map<Integer, Integer> valueMap = new HashMap<>(capacity);
        Map<String, String> extraParam = new HashMap<>(capacity);
        Map<String, Map<String, String>> methodParameters = new HashMap<>(capacity);

        for (String part : parts) {
            part = part.trim();
            if (part.length() > 0) {
                int j = part.indexOf('=');
                if (j >= 0) {
                    String key = part.substring(0, j);
                    String value = part.substring(j + 1);
                    addParameter(keyBit, valueMap, extraParam, methodParameters, key, value, false);
                    // compatible with lower versions registering "default." keys
                    if (key.startsWith(DEFAULT_KEY_PREFIX)) {
                        addParameter(keyBit, valueMap, extraParam, methodParameters, key.substring(DEFAULT_KEY_PREFIX.length()), value, true);
                    }
                } else {
                    addParameter(keyBit, valueMap, extraParam, methodParameters, part, part, false);
                }
            }
        }
        return new URLParam(keyBit, valueMap, extraParam, methodParameters, rawParam);
    }

    /**
     * Parse URLParam
     * Init URLParam by constructor is not allowed
     *
     * @param params   params map added into URLParam
     * @param rawParam original rawParam string, directly add to rawParam field,
     *                 will not affect real key-pairs store in URLParam.
     *                 Please make sure it can correspond with params or will
     *                 cause unexpected result when calling {@link URLParam#getRawParam()}
     *                 and {@link URLParam#toString()} ()}. If you not sure, you can call
     *                 {@link URLParam#parse(String)} to init.
     * @return a new URLParam
     */
    public static URLParam parse(Map<String, String> params, String rawParam) {
        if (CollectionUtils.isNotEmptyMap(params)) {
            int capacity = (int) (params.size() / .75f) + 1;
            BitSet keyBit = new BitSet(capacity);
            Map<Integer, Integer> valueMap = new HashMap<>(capacity);
            Map<String, String> extraParam = new HashMap<>(capacity);
            Map<String, Map<String, String>> methodParameters = new HashMap<>(capacity);

            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                addParameter(keyBit, valueMap, extraParam, methodParameters, key, value, false);
                // compatible with lower versions registering "default." keys
                if (key.startsWith(DEFAULT_KEY_PREFIX)) {
                    addParameter(keyBit, valueMap, extraParam, methodParameters, key.substring(DEFAULT_KEY_PREFIX.length()), value, true);
                }
            }
            return new URLParam(keyBit, valueMap, extraParam, methodParameters, rawParam);
        } else {
            return EMPTY_PARAM;
        }
    }

    private static void addParameter(BitSet keyBit, Map<Integer, Integer> valueMap, Map<String, String> extraParam,
                                     Map<String, Map<String, String>> methodParameters, String key, String value, boolean skipIfPresent) {
        int keyIndex = DynamicParamTable.getKeyIndex(true, key);
        if (skipIfPresent) {
            if (keyIndex < 0) {
                if (extraParam.containsKey(key)) {
                    return;
                }
            } else {
                if (keyBit.get(keyIndex)) {
                    return;
                }
            }
        }

        if (keyIndex < 0) {
            extraParam.put(key, value);
            String[] methodSplit = key.split("\\.", 2);
            if (methodSplit.length == 2) {
                Map<String, String> methodMap = methodParameters.computeIfAbsent(methodSplit[1], (k) -> new HashMap<>());
                methodMap.put(methodSplit[0], value);
            }
        } else {
            valueMap.put(keyIndex, DynamicParamTable.getValueIndex(key, value));
            keyBit.set(keyIndex);
        }
    }
}
