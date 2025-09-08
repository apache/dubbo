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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JacksonImpl extends AbstractJSONImpl {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile Object jacksonCache = null;

    @Override
    public boolean isJson(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.isObject() || node.isArray();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    @Override
    public <T> T toJavaObject(String json, Type type) {
        try {
            return getJackson().readValue(json, getJackson().getTypeFactory().constructType(type));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> clazz) {
        try {
            return getJackson()
                    .readValue(json, getJackson().getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String toJson(Object obj) {
        try {
            return getJackson().writeValueAsString(obj);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private JsonMapper getJackson() {
        if (jacksonCache == null || !(jacksonCache instanceof JsonMapper)) {
            synchronized (this) {
                if (jacksonCache == null || !(jacksonCache instanceof JsonMapper)) {
                    jacksonCache = JsonMapper.builder()
                            .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                            .serializationInclusion(Include.NON_NULL)
                            .addModule(new JavaTimeModule())
                            .build();
                }
            }
        }
        return (JsonMapper) jacksonCache;
    }
}
