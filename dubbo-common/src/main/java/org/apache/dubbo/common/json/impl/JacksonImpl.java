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
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.json.JsonMapper.Builder;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Activate(order = 400, onClass = "com.fasterxml.jackson.databind.json.JsonMapper")
public class JacksonImpl extends AbstractJsonUtilImpl {

    private volatile JsonMapper mapper;
    private final List<Module> customModules = new ArrayList<>();

    @Override
    public String getName() {
        return "jackson";
    }

    @Override
    public boolean isJson(String json) {
        try {
            JsonNode node = getMapper().readTree(json);
            return node.isObject() || node.isArray();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    @Override
    public <T> T toJavaObject(String json, Type type) {
        try {
            JsonMapper mapper = getMapper();
            return mapper.readValue(json, mapper.getTypeFactory().constructType(type));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> clazz) {
        try {
            JsonMapper mapper = getMapper();
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String toJson(Object obj) {
        try {
            return getMapper().writeValueAsString(obj);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String toPrettyJson(Object obj) {
        try {
            return getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Object convertObject(Object obj, Type type) {
        JsonMapper mapper = getMapper();
        return mapper.convertValue(obj, mapper.constructType(type));
    }

    @Override
    public Object convertObject(Object obj, Class<?> clazz) {
        return getMapper().convertValue(obj, clazz);
    }

    protected JsonMapper getMapper() {
        JsonMapper mapper = this.mapper;
        if (mapper == null) {
            synchronized (this) {
                mapper = this.mapper;
                if (mapper == null) {
                    this.mapper = mapper = createBuilder().build();
                }
            }
        }
        return mapper;
    }

    protected Builder createBuilder() {
        Builder builder = JsonMapper.builder()
                .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .serializationInclusion(Include.NON_NULL)
                .addModule(new JavaTimeModule());

        for (Module module : customModules) {
            builder.addModule(module);
        }

        return builder;
    }

    public void addModule(Module module) {
        synchronized (this) {
            customModules.add(module);
            // Invalidate the mapper to rebuild it
            this.mapper = null;
        }
    }
}
