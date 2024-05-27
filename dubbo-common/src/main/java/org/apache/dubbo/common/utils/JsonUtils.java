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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.json.JsonUtil;
import org.apache.dubbo.common.json.impl.FastJson2Impl;
import org.apache.dubbo.common.json.impl.FastJsonImpl;
import org.apache.dubbo.common.json.impl.GsonImpl;
import org.apache.dubbo.common.json.impl.JacksonImpl;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JsonUtils {
    private static volatile JsonUtil jsonUtil;

    protected static JsonUtil getJson() {
        if (jsonUtil == null) {
            synchronized (JsonUtils.class) {
                if (jsonUtil == null) {
                    String preferJsonFrameworkName = SystemPropertyConfigUtils.getSystemProperty(
                            CommonConstants.DubboProperty.DUBBO_PREFER_JSON_FRAMEWORK_NAME);
                    if (StringUtils.isNotEmpty(preferJsonFrameworkName)) {
                        try {
                            JsonUtil instance = null;
                            switch (preferJsonFrameworkName) {
                                case "fastjson2":
                                    instance = new FastJson2Impl();
                                    break;
                                case "fastjson":
                                    instance = new FastJsonImpl();
                                    break;
                                case "gson":
                                    instance = new GsonImpl();
                                    break;
                                case "jackson":
                                    instance = new JacksonImpl();
                                    break;
                                default:
                            }
                            if (instance != null && instance.isSupport()) {
                                jsonUtil = instance;
                            }
                        } catch (Throwable ignore) {

                        }
                    }
                    if (jsonUtil == null) {
                        List<Class<? extends JsonUtil>> jsonClasses = Arrays.asList(
                                FastJson2Impl.class, FastJsonImpl.class, GsonImpl.class, JacksonImpl.class);
                        for (Class<? extends JsonUtil> jsonClass : jsonClasses) {
                            try {
                                JsonUtil instance = jsonClass.getConstructor().newInstance();
                                if (instance.isSupport()) {
                                    jsonUtil = instance;
                                    break;
                                }
                            } catch (Throwable ignore) {

                            }
                        }
                    }
                    if (jsonUtil == null) {
                        throw new IllegalStateException(
                                "Dubbo unable to find out any json framework (e.g. fastjson2, fastjson, gson, jackson) from jvm env. "
                                        + "Please import at least one json framework.");
                    }
                }
            }
        }
        return jsonUtil;
    }

    /**
     * @deprecated for uts only
     */
    @Deprecated
    protected static void setJson(JsonUtil json) {
        JsonUtils.jsonUtil = json;
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
