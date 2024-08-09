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
package org.apache.dubbo.remoting.http12;

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.json.CompositeJsonUtilCustomizer;
import org.apache.dubbo.common.json.JsonUtil;
import org.apache.dubbo.common.json.impl.CustomizableJsonUtil;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public final class HttpJsonUtils {

    private static JsonUtil jsonUtil;

    private HttpJsonUtils() {}

    public static void init(FrameworkModel frameworkModel, Configuration configuration) {
        JsonUtil jsonUtil;
        String frameworkName = configuration.getString(Constants.H2_SETTINGS_JSON_FRAMEWORK_NAME, null);
        if (frameworkName != null) {
            try {
                jsonUtil = frameworkModel.getExtension(JsonUtil.class, frameworkName);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load json framework: " + frameworkName, e);
            }
        } else {
            jsonUtil = CollectionUtils.first(frameworkModel.getActivateExtensions(JsonUtil.class));
        }

        if (jsonUtil == null) {
            return;
        }
        if (jsonUtil instanceof CustomizableJsonUtil) {
            CompositeJsonUtilCustomizer customizer = new CompositeJsonUtilCustomizer(frameworkModel);
            if (customizer.isAvailable()) {
                ((CustomizableJsonUtil<?, ?>) jsonUtil).setCustomizer(customizer);
            }
        }
        HttpJsonUtils.jsonUtil = jsonUtil;
    }

    private static JsonUtil getJson() {
        return jsonUtil == null ? JsonUtils.getJson() : jsonUtil;
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

    public static Object convertObject(Object obj, Type targetType) {
        return getJson().convertObject(obj, targetType);
    }

    public static Object convertObject(Object obj, Class<?> targetType) {
        return getJson().convertObject(obj, targetType);
    }

    public static String getString(Map<String, ?> obj, String key) {
        return getJson().getString(obj, key);
    }
}
