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
package org.apache.dubbo.common.serialize.jackson.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collection;

/**
 * The jackson utils used by dubbo
 */
public abstract class JacksonUtils {

    protected static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        OBJECT_MAPPER.activateDefaultTypingAsProperty(OBJECT_MAPPER.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@c");
    }

    public static <T> T readValue(String content, Class<T> valueType)
            throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(content, valueType);
    }

    public static String writeValueAsString(Object value) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(value);
    }

    public static boolean isArray(Object obj) {
        return obj != null && obj.getClass().isArray() || obj instanceof Collection;
    }

}
