package com.alibaba.dubbo.common.json;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimeZone;

/**
 * Created by dylan on 11/12/14.
 */
public class Jackson {
    private static Logger logger = LoggerFactory.getLogger(Jackson.class);
    private static ObjectMapper objectMapper;

    private static JacksonObjectMapperProvider getJacksonProvider() {
        return ExtensionLoader.getExtensionLoader(JacksonObjectMapperProvider.class).getDefaultExtension();
    }

    /**
     * 获取object mapper
     * @return
     */
    public static ObjectMapper getObjectMapper(){
        ObjectMapper objectMapper = null;
        if(objectMapper == null){
            JacksonObjectMapperProvider jacksonObjectMapperProvider = getJacksonProvider();
            if(jacksonObjectMapperProvider != null){
                objectMapper = jacksonObjectMapperProvider.getObjectMapper();
            }
        }
        if(objectMapper == null){
            logger.warn("load objectMapper failed, use default config.");
            objectMapper = buildDefaultObjectMapper();
        }
        return objectMapper;
    }
    private static synchronized ObjectMapper buildDefaultObjectMapper(){
        if(objectMapper == null){
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
//            objectMapper.disable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            objectMapper.setTimeZone(TimeZone.getDefault());
            return objectMapper;
        }
        return null;
    }
}
