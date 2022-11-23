package org.apache.dubbo.rpc.protocol.mvc.servlet;

import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import javax.xml.transform.Source;
import java.util.HashSet;
import java.util.Set;

public class MvcConfigurationSupport {
    private static boolean romePresent =
        ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", WebMvcConfigurationSupport.class.getClassLoader());

    private static final boolean jaxb2Present =
        ClassUtils.isPresent("javax.xml.bind.Binder", WebMvcConfigurationSupport.class.getClassLoader());

    private static final boolean jackson2Present =
        ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", WebMvcConfigurationSupport.class.getClassLoader()) &&
            ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", WebMvcConfigurationSupport.class.getClassLoader());

    private static final boolean jackson2XmlPresent =
        ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", WebMvcConfigurationSupport.class.getClassLoader());

    private static final boolean gsonPresent =
        ClassUtils.isPresent("com.google.gson.Gson", WebMvcConfigurationSupport.class.getClassLoader());


    public static void addDefaultHttpMessageConverters(Set<HttpMessageConverter> tmpMessageConverters) {
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
        stringConverter.setWriteAcceptCharset(false);

        tmpMessageConverters.add(new ByteArrayHttpMessageConverter());
        tmpMessageConverters.add(stringConverter);
        tmpMessageConverters.add(new ResourceHttpMessageConverter());
        tmpMessageConverters.add(new SourceHttpMessageConverter<Source>());
        tmpMessageConverters.add(new AllEncompassingFormHttpMessageConverter());

        if (romePresent) {
            tmpMessageConverters.add(new AtomFeedHttpMessageConverter());
            tmpMessageConverters.add(new RssChannelHttpMessageConverter());
        }

        if (jackson2XmlPresent) {
            tmpMessageConverters.add(new MappingJackson2XmlHttpMessageConverter());
        } else if (jaxb2Present) {
            tmpMessageConverters.add(new Jaxb2RootElementHttpMessageConverter());
        }

        if (jackson2Present) {
            tmpMessageConverters.add(new MappingJackson2HttpMessageConverter());
        } else if (gsonPresent) {
            tmpMessageConverters.add(new GsonHttpMessageConverter());
        }

    }


    public static Set<HttpMessageConverter> getDefaultHttpMessageConverters() {

        HashSet<HttpMessageConverter> httpMessageConverters = new HashSet<>();
        addDefaultHttpMessageConverters(httpMessageConverters);
        return httpMessageConverters;
    }
}
