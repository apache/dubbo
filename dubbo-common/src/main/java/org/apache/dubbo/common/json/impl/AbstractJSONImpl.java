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
package org.apache.dubbo.common.json.impl;

import org.apache.dubbo.common.json.JSON;
import org.apache.dubbo.common.utils.CollectionUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class AbstractJSONImpl implements JSON {
    @Override
    public boolean isSupport() {
        try {
            Map<String, String> map = new HashMap<>();
            map.put("json", "test");
            if (!CollectionUtils.mapEquals(map, toJavaObject(toJson(map), Map.class))) {
                return false;
            }

            List<String> list = new LinkedList<>();
            list.add("json");
            return CollectionUtils.equals(list, toJavaList(toJson(list), String.class));
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public List<?> getList(Map<String, ?> obj, String key) {
        assert obj != null;
        assert key != null;
        if (!obj.containsKey(key)) {
            return null;
        }
        Object value = obj.get(key);
        if (!(value instanceof List)) {
            throw new ClassCastException(
                String.format("value '%s' for key '%s' in '%s' is not List", value, key, obj));
        }
        return (List<?>) value;
    }

    /**
     * Gets a list from an object for the given key, and verifies all entries are objects.  If the key
     * is not present, this returns null.  If the value is not a List or an entry is not an object,
     * throws an exception.
     */
    @Override
    public List<Map<String, ?>> getListOfObjects(Map<String, ?> obj, String key) {
        assert obj != null;
        List<?> list = getList(obj, key);
        if (list == null) {
            return null;
        }
        return checkObjectList(list);
    }

    /**
     * Gets a list from an object for the given key, and verifies all entries are strings.  If the key
     * is not present, this returns null.  If the value is not a List or an entry is not a string,
     * throws an exception.
     */
    @Override
    public List<String> getListOfStrings(Map<String, ?> obj, String key) {
        assert obj != null;
        List<?> list = getList(obj, key);
        if (list == null) {
            return null;
        }
        return checkStringList(list);
    }

    /**
     * Gets an object from an object for the given key.  If the key is not present, this returns null.
     * If the value is not a Map, throws an exception.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, ?> getObject(Map<String, ?> obj, String key) {
        assert obj != null;
        assert key != null;
        if (!obj.containsKey(key)) {
            return null;
        }
        Object value = obj.get(key);
        if (!(value instanceof Map)) {
            throw new ClassCastException(
                String.format("value '%s' for key '%s' in '%s' is not object", value, key, obj));
        }
        return (Map<String, ?>) value;
    }

    /**
     * Gets a number from an object for the given key.  If the key is not present, this returns null.
     * If the value does not represent a double, throws an exception.
     */
    @Override
    public Double getNumberAsDouble(Map<String, ?> obj, String key) {
        assert obj != null;
        assert key != null;
        if (!obj.containsKey(key)) {
            return null;
        }
        Object value = obj.get(key);
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    String.format("value '%s' for key '%s' is not a double", value, key));
            }
        }
        throw new IllegalArgumentException(
            String.format("value '%s' for key '%s' in '%s' is not a number", value, key, obj));
    }

    /**
     * Gets a number from an object for the given key, casted to an integer.  If the key is not
     * present, this returns null.  If the value does not represent an integer, throws an exception.
     */
    @Override
    public Integer getNumberAsInteger(Map<String, ?> obj, String key) {
        assert obj != null;
        assert key != null;
        if (!obj.containsKey(key)) {
            return null;
        }
        Object value = obj.get(key);
        if (value instanceof Double) {
            Double d = (Double) value;
            int i = d.intValue();
            if (i != d) {
                throw new ClassCastException("Number expected to be integer: " + d);
            }
            return i;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    String.format("value '%s' for key '%s' is not an integer", value, key));
            }
        }
        throw new IllegalArgumentException(
            String.format("value '%s' for key '%s' is not an integer", value, key));
    }

    /**
     * Gets a number from an object for the given key, casted to an long.  If the key is not
     * present, this returns null.  If the value does not represent a long integer, throws an
     * exception.
     */
    @Override
    public Long getNumberAsLong(Map<String, ?> obj, String key) {
        assert obj != null;
        assert key != null;
        if (!obj.containsKey(key)) {
            return null;
        }
        Object value = obj.get(key);
        if (value instanceof Double) {
            Double d = (Double) value;
            long l = d.longValue();
            if (l != d) {
                throw new ClassCastException("Number expected to be long: " + d);
            }
            return l;
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    String.format("value '%s' for key '%s' is not a long integer", value, key));
            }
        }
        throw new IllegalArgumentException(
            String.format("value '%s' for key '%s' is not a long integer", value, key));
    }

    /**
     * Gets a string from an object for the given key.  If the key is not present, this returns null.
     * If the value is not a String, throws an exception.
     */
    @Override
    public String getString(Map<String, ?> obj, String key) {
        assert obj != null;
        assert key != null;
        if (!obj.containsKey(key)) {
            return null;
        }
        Object value = obj.get(key);
        if (!(value instanceof String)) {
            throw new ClassCastException(
                String.format("value '%s' for key '%s' in '%s' is not String", value, key, obj));
        }
        return (String) value;
    }

    /**
     * Casts a list of unchecked JSON values to a list of checked objects in Java type.
     * If the given list contains a value that is not a Map, throws an exception.
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, ?>> checkObjectList(List<?> rawList) {
        assert rawList != null;
        for (int i = 0; i < rawList.size(); i++) {
            if (!(rawList.get(i) instanceof Map)) {
                throw new ClassCastException(
                    String.format("value %s for idx %d in %s is not object", rawList.get(i), i, rawList));
            }
        }
        return (List<Map<String, ?>>) rawList;
    }


    /**
     * Casts a list of unchecked JSON values to a list of String. If the given list
     * contains a value that is not a String, throws an exception.
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<String> checkStringList(List<?> rawList) {
        assert rawList != null;
        for (int i = 0; i < rawList.size(); i++) {
            if (!(rawList.get(i) instanceof String)) {
                throw new ClassCastException(
                    String.format(
                        "value '%s' for idx %d in '%s' is not string", rawList.get(i), i, rawList));
            }
        }
        return (List<String>) rawList;
    }

}
