package com.alibaba.dubbo.common.json;

import com.alibaba.dubbo.common.extension.SPI;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by dylan on 11/12/14.
 */
@SPI("jackson")
public interface JacksonObjectMapperProvider {
    public ObjectMapper getObjectMapper();
}
