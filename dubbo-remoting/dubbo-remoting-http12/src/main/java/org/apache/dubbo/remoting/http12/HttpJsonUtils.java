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
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.json.JsonUtil;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HttpJsonUtils {

    private final JsonUtil jsonUtil;

    public HttpJsonUtils(FrameworkModel frameworkModel) {
        Configuration configuration = ConfigurationUtils.getGlobalConfiguration(frameworkModel.defaultApplication());
        JsonUtil jsonUtil;
        String name = configuration.getString(Constants.H2_SETTINGS_JSON_FRAMEWORK_NAME, null);
        if (name == null) {
            jsonUtil = CollectionUtils.first(frameworkModel.getActivateExtensions(JsonUtil.class));
        } else {
            try {
                jsonUtil = frameworkModel.getExtension(JsonUtil.class, name);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load json framework: " + name, e);
            }
        }

        this.jsonUtil = Objects.requireNonNull(jsonUtil, "Dubbo unable to find out any json framework");
    }

    public <T> T toJavaObject(String json, Type type) {
        return jsonUtil.toJavaObject(json, type);
    }

    public <T> List<T> toJavaList(String json, Class<T> clazz) {
        return jsonUtil.toJavaList(json, clazz);
    }

    public String toJson(Object obj) {
        return jsonUtil.toJson(obj);
    }

    public String toPrettyJson(Object obj) {
        return jsonUtil.toPrettyJson(obj);
    }

    public Object convertObject(Object obj, Type targetType) {
        return jsonUtil.convertObject(obj, targetType);
    }

    public Object convertObject(Object obj, Class<?> targetType) {
        return jsonUtil.convertObject(obj, targetType);
    }

    public String getString(Map<String, ?> obj, String key) {
        return jsonUtil.getString(obj, key);
    }
}
