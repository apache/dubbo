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

import java.util.Arrays;
import java.util.List;

public class JsonUtils {
    private static volatile JsonUtil jsonUtil;

    public static JsonUtil getJson() {
        if (jsonUtil == null) {
            synchronized (JsonUtils.class) {
                if (jsonUtil == null) {
                    String preferJsonFrameworkName = System.getProperty(CommonConstants.PREFER_JSON_FRAMEWORK_NAME);
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
                            }
                            if (instance != null && instance.isSupport()) {
                                jsonUtil = instance;
                            }
                        } catch (Throwable ignore) {

                        }
                    }
                    if (jsonUtil == null) {
                        List<Class<? extends JsonUtil>> jsonClasses = Arrays.asList(
                            FastJson2Impl.class,
                            FastJsonImpl.class,
                            GsonImpl.class,
                            JacksonImpl.class);
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
                        throw new IllegalStateException("Dubbo unable to find out any json framework (e.g. fastjson2, fastjson, gson, jackson) from jvm env. " +
                            "Please import at least one json framework.");
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
}
