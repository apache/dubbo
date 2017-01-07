package com.alibaba.dubbo.rpc.protocol.springmvc.message;

import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.ClassUtils;

import javax.xml.transform.Source;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wuyu on 2016/6/8.
 */
public class MessageConverters {
    private static boolean romePresent =
            ClassUtils.isPresent("com.sun.syndication.feed.WireFeed", MessageConverters.class.getClassLoader());

    private static final boolean jaxb2Present =
            ClassUtils.isPresent("javax.xml.bind.Binder", MessageConverters.class.getClassLoader());

    private static final boolean jackson2Present =
            ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", MessageConverters.class.getClassLoader()) &&
                    ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", MessageConverters.class.getClassLoader());

    private static final boolean jacksonPresent =
            ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", MessageConverters.class.getClassLoader()) &&
                    ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator", MessageConverters.class.getClassLoader());

    private static final boolean fastJson =
            ClassUtils.isPresent("com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter", MessageConverters.class.getClassLoader());

    private final List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

    public MessageConverters() {

        this.messageConverters.add(new ByteArrayHttpMessageConverter());
        this.messageConverters.add(new StringHttpMessageConverter());
        this.messageConverters.add(new ResourceHttpMessageConverter());
        this.messageConverters.add(new SourceHttpMessageConverter<Source>());
        this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
        this.messageConverters.add(new HessainHttpMessageConverter());

        if(fastJson){
            try {
                Class<?> fastJsonClass = ClassUtils.forName("com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter", MessageConverters.class.getClassLoader());
                this.messageConverters.add((HttpMessageConverter<?>) fastJsonClass.newInstance());
            } catch (Exception e) {
            }
        }
        if (romePresent) {
            this.messageConverters.add(new AtomFeedHttpMessageConverter());
            this.messageConverters.add(new RssChannelHttpMessageConverter());
        }
        if (jaxb2Present) {
            this.messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
        }
        if (jackson2Present) {
            this.messageConverters.add(new MappingJackson2HttpMessageConverter());
        }

    }

    /**
     * Return the message body converters.
     */
    public List<HttpMessageConverter<?>> getMessageConverters() {
        return this.messageConverters;
    }
}
