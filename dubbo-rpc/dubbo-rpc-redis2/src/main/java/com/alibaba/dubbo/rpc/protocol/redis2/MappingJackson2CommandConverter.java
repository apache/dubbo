package com.alibaba.dubbo.rpc.protocol.redis2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wuyu on 2017/2/7.
 */
public class MappingJackson2CommandConverter {

    public static final String DEFAULT_ENCODING = "UTF-8";

    private ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private String encoding = DEFAULT_ENCODING;

    private Map<String, Class<?>> idClassMappings = new HashMap<String, Class<?>>();

    private Map<Class<?>, String> classIdMappings = new HashMap<Class<?>, String>();

    private ClassLoader beanClassLoader = MappingJackson2CommandConverter.class.getClassLoader();


    public Object fromMessage(byte[] bytes, String typeId) {
        try {
            JavaType targetJavaType = getJavaType(typeId);
            return convertFromBytes(bytes, targetJavaType);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to convert JSON message content", ex);
        }
    }


    public  Object convertFromBytes(byte[] bytes, JavaType targetJavaType)
            throws IOException {

        String encoding = this.encoding;
        try {
            String body = new String(bytes, encoding);
            return this.objectMapper.readValue(body, targetJavaType);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Cannot convert bytes to String", ex);
        }
    }


    public JavaType getJavaType(String typeId) {
        Class<?> mappedClass = this.idClassMappings.get(typeId);
        if (mappedClass != null) {
            return this.objectMapper.getTypeFactory().constructType(mappedClass);
        }
        try {
            Class<?> typeClass = ClassUtils.forName(typeId, this.beanClassLoader);
            return this.objectMapper.getTypeFactory().constructType(typeClass);
        } catch (Throwable ex) {
            throw new RuntimeException("Failed to resolve type id [" + typeId + "]", ex);
        }
    }

    public JavaType getJavaType(Type type) {
        try {
            return this.objectMapper.getTypeFactory().constructType(type);
        } catch (Throwable ex) {
            throw new RuntimeException("Failed to resolve type id [" + type + "]", ex);
        }
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        this.objectMapper = objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setTypeIdMappings(Map<String, Class<?>> typeIdMappings) {
        this.idClassMappings = new HashMap<String, Class<?>>();
        for (Map.Entry<String, Class<?>> entry : typeIdMappings.entrySet()) {
            String id = entry.getKey();
            Class<?> clazz = entry.getValue();
            this.idClassMappings.put(id, clazz);
            this.classIdMappings.put(clazz, id);
        }
    }

    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }


}
