package org.apache.dubbo.common.serialize.jackson.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Writer;

/**
 * The jackson utils used by dubbo
 *
 * @author Johnson.Jia
 */
public abstract class JacksonUtils {

    /**
     * jackson 序列化
     */
    protected static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        // 过滤掉 null值
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 在遇到未知属性时防止异常
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        OBJECT_MAPPER.activateDefaultTypingAsProperty(OBJECT_MAPPER.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@c");
    }

    /**
     * Method to deserialize JSON content from given JSON content String.
     *
     * @throws JsonParseException
     *             if underlying input contains invalid content of type {@link JsonParser} supports (JSON for default
     *             case)
     * @throws JsonMappingException
     *             if the input JSON structure does not match structure expected for result type (or has other mismatch
     *             issues)
     */
    public static <T> T readValue(String content, Class<T> valueType)
        throws JsonProcessingException, JsonMappingException {
        return OBJECT_MAPPER.readValue(content, valueType);
    }

    /**
     * Method that can be used to serialize any Java value as a String. Functionally equivalent to calling
     * {@link ObjectMapper#writeValue(Writer,Object)} with {@link java.io.StringWriter} and constructing String, but
     * more efficient.
     * <p>
     * Note: prior to version 2.1, throws clause included {@link IOException}; 2.1 removed it.
     */
    public static String writeValueAsString(Object value) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(value);
    }

}
