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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Activate(order = 400, onClass = "com.fasterxml.jackson.databind.json.JsonMapper")
public class JacksonImpl extends CustomizableJsonUtil<Void, JsonMapper> {

    @Override
    public String getName() {
        return "jackson";
    }

    @Override
    public boolean isJson(String json) {
        try {
            JsonNode node = getWriter().readTree(json);
            return node.isObject() || node.isArray();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    @Override
    public <T> T toJavaObject(String json, Type type) {
        try {
            JsonMapper mapper = getWriter();
            return mapper.readValue(json, mapper.getTypeFactory().constructType(type));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> clazz) {
        try {
            JsonMapper mapper = getWriter();
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String toJson(Object obj) {
        try {
            return getWriter().writeValueAsString(obj);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String toPrettyJson(Object obj) {
        try {
            ObjectWriter prettyWriter = getWriter().writerWithDefaultPrettyPrinter();
            return prettyWriter.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Object convertObject(Object obj, Type type) {
        JsonMapper mapper = getWriter();
        return mapper.convertValue(obj, mapper.constructType(type));
    }

    @Override
    public Object convertObject(Object obj, Class<?> clazz) {
        return getWriter().convertValue(obj, clazz);
    }

    @Override
    protected JsonMapper newWriter() {
        return JsonMapper.builder()
                .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .serializationInclusion(Include.NON_NULL)
                .addModule(new JavaTimeModule())
                .build();
    }
}
