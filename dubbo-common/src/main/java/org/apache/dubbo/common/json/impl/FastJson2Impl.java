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

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONValidator;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.JSONWriter.Context;
import com.alibaba.fastjson2.JSONWriter.Feature;
import com.alibaba.fastjson2.util.TypeUtils;

@Activate(order = 100, onClass = "com.alibaba.fastjson2.JSON")
public class FastJson2Impl extends CustomizableJsonUtil<JSONReader.Context, JSONWriter.Context> {

    @Override
    public boolean isJson(String json) {
        return JSONValidator.from(json).validate();
    }

    @Override
    public <T> T toJavaObject(String json, Type type) {
        if (hasCustomizer()) {
            return JSON.parseObject(json, type, createReader());
        }

        return JSON.parseObject(json, type);
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> clazz) {
        if (hasCustomizer()) {
            return parseArray(json, clazz, createReader());
        }

        return JSON.parseArray(json, clazz);
    }

    @Override
    public String toJson(Object obj) {
        if (hasCustomizer()) {
            Context writer = createWriter();
            writer.config(Feature.WriteEnumsUsingName);
            return JSON.toJSONString(obj, writer);
        }

        return JSON.toJSONString(obj, Feature.WriteEnumsUsingName);
    }

    @Override
    public String toPrettyJson(Object obj) {
        if (hasCustomizer()) {
            Context writer = createWriter();
            writer.config(Feature.WriteEnumsUsingName, Feature.PrettyFormat);
            return JSON.toJSONString(obj, writer);
        }

        return JSON.toJSONString(obj, Feature.WriteEnumsUsingName, Feature.PrettyFormat);
    }

    @Override
    public Object convertObject(Object obj, Type type) {
        if (hasCustomizer()) {
            return TypeUtils.cast(obj, type, getReader().getProvider());
        }

        return TypeUtils.cast(obj, type);
    }

    @Override
    public Object convertObject(Object obj, Class<?> clazz) {
        if (hasCustomizer()) {
            return TypeUtils.cast(obj, clazz, getReader().getProvider());
        }

        return TypeUtils.cast(obj, clazz);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> parseArray(String text, Class<T> type, JSONReader.Context context) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        try (JSONReader reader = JSONReader.of(text, context)) {
            List<T> list = reader.readArray(type);
            reader.handleResolveTasks(list);
            if (!reader.isEnd() && (context.getFeatures() & JSONReader.Feature.IgnoreCheckClose.mask) == 0) {
                throw new JSONException(reader.info("input not end"));
            }
            return list;
        }
    }

    @Override
    protected JSONReader.Context newReader() {
        return new JSONReader.Context();
    }

    @Override
    protected Context newWriter() {
        return new JSONWriter.Context();
    }
}
