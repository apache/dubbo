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
import org.apache.dubbo.common.json.JSON;
import org.apache.dubbo.common.json.impl.FastJsonImpl;
import org.apache.dubbo.common.json.impl.GsonImpl;

import java.util.Arrays;
import java.util.List;

public class JsonUtils {
    private static volatile JSON json;

    public static JSON getJson() {
        if (json == null) {
            synchronized (JsonUtils.class) {
                if (json == null) {
                    String preferJsonFrameworkName = System.getProperty(CommonConstants.PREFER_JSON_FRAMEWORK_NAME);
                    if (StringUtils.isNotEmpty(preferJsonFrameworkName)) {
                        try {
                            JSON instance = null;
                            switch (preferJsonFrameworkName) {
                                case "fastjson":
                                    instance = new FastJsonImpl();
                                    break;
                                case "gson":
                                    instance = new GsonImpl();
                                    break;
                            }
                            if (instance != null && instance.isSupport()) {
                                json = instance;
                            }
                        } catch (Throwable ignore) {

                        }
                    }
                    if (json == null) {
                        List<Class<? extends JSON>> jsonClasses = Arrays.asList(
                            FastJsonImpl.class,
                            GsonImpl.class);
                        for (Class<? extends JSON> jsonClass : jsonClasses) {
                            try {
                                JSON instance = jsonClass.getConstructor().newInstance();
                                if (instance.isSupport()) {
                                    json = instance;
                                    break;
                                }
                            } catch (Throwable ignore) {

                            }
                        }
                    }
                    if (json == null) {
                        throw new IllegalStateException("Dubbo unable to find out any json framework (e.g. fastjson, gson) from jvm env. " +
                            "Please import at least one json framework.");
                    }
                }
            }
        }
        return json;
    }

    /**
     * @deprecated for uts only
     */
    @Deprecated
    protected static void setJson(JSON json) {
        JsonUtils.json = json;
    }
}
