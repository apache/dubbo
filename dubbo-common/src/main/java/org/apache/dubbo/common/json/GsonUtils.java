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

import org.apache.dubbo.common.utils.ClassUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_GSON;

public class GsonUtils {
    // weak reference of com.google.gson.Gson, prevent throw exception when init
    private static volatile Object gsonCache = null;

    private static volatile Boolean supportGson;

    private static boolean isSupportGson() {
        if (supportGson == null) {
            synchronized (GsonUtils.class) {
                if (supportGson == null) {
                    try {
                        Class<?> aClass = ClassUtils.forName("com.google.gson.Gson");
                        supportGson = aClass != null;
                    } catch (Throwable t) {
                        supportGson = false;
                    }
                }
            }
        }
        return supportGson;
    }

    public static Object fromJson(String json, Type originType) throws RuntimeException {
        if (!isSupportGson()) {
            throw new RuntimeException("Gson is not supported. Please import Gson in JVM env.");
        }
        Type type = TypeToken.get(originType).getType();
        try {
            return getGson().fromJson(json, type);
        } catch (JsonSyntaxException ex) {
            throw new RuntimeException(String.format("Generic serialization [%s] Json syntax exception thrown when parsing (message:%s type:%s) error:%s", GENERIC_SERIALIZATION_GSON, json, type.toString(), ex.getMessage()));
        }
    }

    private static Gson getGson() {
        if (gsonCache == null || !(gsonCache instanceof Gson)) {
            synchronized (GsonUtils.class) {
                if (gsonCache == null || !(gsonCache instanceof Gson)) {
                    gsonCache = new Gson();
                }
            }
        }
        return (Gson) gsonCache;
    }

    /**
     * @deprecated for uts only
     */
    @Deprecated
    protected static void setSupportGson(Boolean supportGson) {
        GsonUtils.supportGson = supportGson;
    }
}
