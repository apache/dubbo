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

import org.apache.dubbo.common.extension.Activate;

import java.lang.reflect.Type;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.TypeUtils;

@Activate(order = 200, onClass = "com.alibaba.fastjson.JSON")
public class FastJsonImpl extends AbstractJsonUtilImpl {

    @Override
    public String getName() {
        return "fastjson";
    }

    @Override
    public boolean isJson(String json) {
        try {
            Object obj = JSON.parse(json);
            return obj instanceof JSONObject || obj instanceof JSONArray;
        } catch (JSONException e) {
            return false;
        }
    }

    @Override
    public <T> T toJavaObject(String json, Type type) {
        return JSON.parseObject(json, type);
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> clazz) {
        return JSON.parseArray(json, clazz);
    }

    @Override
    public String toJson(Object obj) {
        return JSON.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect);
    }

    @Override
    public String toPrettyJson(Object obj) {
        return JSON.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat);
    }

    @Override
    public Object convertObject(Object obj, Type type) {
        return TypeUtils.cast(obj, type, ParserConfig.getGlobalInstance());
    }

    @Override
    public Object convertObject(Object obj, Class<?> clazz) {
        return TypeUtils.cast(obj, clazz, ParserConfig.getGlobalInstance());
    }
}
