package org.apache.dubbo.rpc.protocol.mvc.servlet;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.ArrayList;
import java.util.HashSet;

public class MvcRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {


    public MvcRequestMappingHandlerAdapter() {
        afterPropertiesSet();
    }

    protected boolean isContextRequired() {
        return false;
    }


    @Override
    public void afterPropertiesSet() {

        HashSet<HttpMessageConverter> httpMessageConverters = new HashSet<>();
        MvcConfigurationSupport.addDefaultHttpMessageConverters(httpMessageConverters);
        setMessageConverters(new ArrayList<>(httpMessageConverters));
        super.afterPropertiesSet();
    }
}
