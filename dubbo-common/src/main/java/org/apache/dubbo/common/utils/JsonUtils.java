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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.json.JsonUtil;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;

import static org.apache.dubbo.common.constants.CommonConstants.DubboProperty.DUBBO_PREFER_JSON_FRAMEWORK_NAME;

public class JsonUtils {

    private static volatile JsonUtil jsonUtil;

    public static JsonUtil getJson() {
        if (jsonUtil == null) {
            synchronized (JsonUtils.class) {
                if (jsonUtil == null) {
                    jsonUtil = createJsonUtil();
                }
            }
        }
        return jsonUtil;
    }

    private static JsonUtil createJsonUtil() {
        Map<String, JsonUtil> extensions = new HashMap<>();
        String preferName = SystemPropertyConfigUtils.getSystemProperty(DUBBO_PREFER_JSON_FRAMEWORK_NAME);

        ClassLoader classLoader = JsonUtil.class.getClassLoader();
        JsonUtil jsonUtil = loadExtensions(preferName, classLoader, extensions);
        if (jsonUtil != null) {
            return jsonUtil;
        }

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl != null && tccl != classLoader) {
            jsonUtil = loadExtensions(preferName, classLoader, extensions);
            if (jsonUtil != null) {
                return jsonUtil;
            }
        }

        TreeMap<Integer, JsonUtil> sortedExtensions = new TreeMap<>();
        for (JsonUtil extension : extensions.values()) {
            Activate activate = extension.getClass().getAnnotation(Activate.class);
            sortedExtensions.put(activate == null ? 0 : activate.order(), extension);
        }

        if (sortedExtensions.isEmpty()) {
            throw new IllegalStateException("Dubbo unable to find out any json framework (e.g. fastjson2, "
                    + "fastjson, gson, jackson) from jvm env. Please import at least one json framework.");
        }

        return sortedExtensions.firstEntry().getValue();
    }

    private static JsonUtil loadExtensions(String name, ClassLoader classLoader, Map<String, JsonUtil> extensions) {
        ServiceLoader<JsonUtil> loader = ServiceLoader.load(JsonUtil.class, classLoader);
        for (Iterator<JsonUtil> it = loader.iterator(); it.hasNext(); ) {
            try {
                JsonUtil extension = it.next();
                if (extension.isSupport()) {
                    if (name != null && name.equals(extension.getName())) {
                        return extension;
                    }
                    extensions.put(extension.getName(), extension);
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /**
     * @deprecated for unit test only
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    protected static void setJson(JsonUtil json) {
        jsonUtil = json;
    }

    public static <T> T toJavaObject(String json, Type type) {
        return getJson().toJavaObject(json, type);
    }

    public static <T> List<T> toJavaList(String json, Class<T> clazz) {
        return getJson().toJavaList(json, clazz);
    }

    public static String toJson(Object obj) {
        return getJson().toJson(obj);
    }

    public static String toPrettyJson(Object obj) {
        return getJson().toPrettyJson(obj);
    }

    public static List<?> getList(Map<String, ?> obj, String key) {
        return getJson().getList(obj, key);
    }

    public static List<Map<String, ?>> getListOfObjects(Map<String, ?> obj, String key) {
        return getJson().getListOfObjects(obj, key);
    }

    public static List<String> getListOfStrings(Map<String, ?> obj, String key) {
        return getJson().getListOfStrings(obj, key);
    }

    public static Map<String, ?> getObject(Map<String, ?> obj, String key) {
        return getJson().getObject(obj, key);
    }

    public static Object convertObject(Object obj, Type targetType) {
        return getJson().convertObject(obj, targetType);
    }

    public static Object convertObject(Object obj, Class<?> targetType) {
        return getJson().convertObject(obj, targetType);
    }

    public static Double getNumberAsDouble(Map<String, ?> obj, String key) {
        return getJson().getNumberAsDouble(obj, key);
    }

    public static Integer getNumberAsInteger(Map<String, ?> obj, String key) {
        return getJson().getNumberAsInteger(obj, key);
    }

    public static Long getNumberAsLong(Map<String, ?> obj, String key) {
        return getJson().getNumberAsLong(obj, key);
    }

    public static String getString(Map<String, ?> obj, String key) {
        return getJson().getString(obj, key);
    }

    public static List<Map<String, ?>> checkObjectList(List<?> rawList) {
        return getJson().checkObjectList(rawList);
    }

    public static List<String> checkStringList(List<?> rawList) {
        return getJson().checkStringList(rawList);
    }

    public static boolean checkJson(String json) {
        return getJson().isJson(json);
    }
}
