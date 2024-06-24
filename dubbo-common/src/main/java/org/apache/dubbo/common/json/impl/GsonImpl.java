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

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class GsonImpl extends AbstractJSONImpl {
    // weak reference of com.google.gson.Gson, prevent throw exception when init
    private volatile Object gsonCache = null;

    @Override
    public boolean isJson(String json) {
        try {
            JsonElement jsonElement = JsonParser.parseString(json);
            return jsonElement.isJsonObject() || jsonElement.isJsonArray();
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    @Override
    public <T> T toJavaObject(String json, Type type) {
        return getGson().fromJson(json, type);
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> clazz) {
        return getGson()
                .fromJson(json, TypeToken.getParameterized(List.class, clazz).getType());
    }

    @Override
    public String toJson(Object obj) {
        return getGson().toJson(obj);
    }

    private Gson getGson() {
        if (gsonCache == null || !(gsonCache instanceof Gson)) {
            synchronized (this) {
                if (gsonCache == null || !(gsonCache instanceof Gson)) {
                    gsonCache = new Gson();
                }
            }
        }
        return (Gson) gsonCache;
    }
}
