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
package org.apache.dubbo.common.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * JSONObject.
 */
@Deprecated
public class JSONObject implements JSONNode {
    private Map<String, Object> mMap = new HashMap<String, Object>();

    /**
     * get.
     *
     * @param key key.
     * @return boolean or long or double or String or JSONArray or JSONObject or null.
     */
    public Object get(String key) {
        return mMap.get(key);
    }

    /**
     * get boolean value.
     *
     * @param key key.
     * @param def default value.
     * @return value or default value.
     */
    public boolean getBoolean(String key, boolean def) {
        Object tmp = mMap.get(key);
        return tmp instanceof Boolean ? (Boolean) tmp : def;
    }

    /**
     * get int value.
     *
     * @param key key.
     * @param def default value.
     * @return value or default value.
     */
    public int getInt(String key, int def) {
        Object tmp = mMap.get(key);
        return tmp instanceof Number ? ((Number) tmp).intValue() : def;
    }

    /**
     * get long value.
     *
     * @param key key.
     * @param def default value.
     * @return value or default value.
     */
    public long getLong(String key, long def) {
        Object tmp = mMap.get(key);
        return tmp instanceof Number ? ((Number) tmp).longValue() : def;
    }

    /**
     * get float value.
     *
     * @param key key.
     * @param def default value.
     * @return value or default value.
     */
    public float getFloat(String key, float def) {
        Object tmp = mMap.get(key);
        return tmp instanceof Number ? ((Number) tmp).floatValue() : def;
    }

    /**
     * get double value.
     *
     * @param key key.
     * @param def default value.
     * @return value or default value.
     */
    public double getDouble(String key, double def) {
        Object tmp = mMap.get(key);
        return tmp instanceof Number ? ((Number) tmp).doubleValue() : def;
    }

    /**
     * get string value.
     *
     * @param key key.
     * @return value or default value.
     */
    public String getString(String key) {
        Object tmp = mMap.get(key);
        return tmp == null ? null : tmp.toString();
    }

    /**
     * get JSONArray value.
     *
     * @param key key.
     * @return value or default value.
     */
    public JSONArray getArray(String key) {
        Object tmp = mMap.get(key);
        return tmp == null ? null : tmp instanceof JSONArray ? (JSONArray) tmp : null;
    }

    /**
     * get JSONObject value.
     *
     * @param key key.
     * @return value or default value.
     */
    public JSONObject getObject(String key) {
        Object tmp = mMap.get(key);
        return tmp == null ? null : tmp instanceof JSONObject ? (JSONObject) tmp : null;
    }

    /**
     * get key iterator.
     *
     * @return key iterator.
     */
    public Iterator<String> keys() {
        return mMap.keySet().iterator();
    }

    /**
     * contains key.
     *
     * @param key key.
     * @return contains or not.
     */
    public boolean contains(String key) {
        return mMap.containsKey(key);
    }

    /**
     * put value.
     *
     * @param name  name.
     * @param value value.
     */
    public void put(String name, Object value) {
        mMap.put(name, value);
    }

    /**
     * put all.
     *
     * @param names  name array.
     * @param values value array.
     */
    public void putAll(String[] names, Object[] values) {
        for (int i = 0, len = Math.min(names.length, values.length); i < len; i++) {
            mMap.put(names[i], values[i]);
        }
    }

    /**
     * put all.
     *
     * @param map map.
     */
    public void putAll(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            mMap.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * write json.
     *
     * @param jc json converter.
     * @param jb json builder.
     */
    @Override
    public void writeJSON(JSONConverter jc, JSONWriter jb, boolean writeClass) throws IOException {
        String key;
        Object value;
        jb.objectBegin();
        for (Map.Entry<String, Object> entry : mMap.entrySet()) {
            key = entry.getKey();
            jb.objectItem(key);
            value = entry.getValue();
            if (value == null) {
                jb.valueNull();
            } else {
                jc.writeValue(value, jb, writeClass);
            }
        }
        jb.objectEnd();
    }
}
