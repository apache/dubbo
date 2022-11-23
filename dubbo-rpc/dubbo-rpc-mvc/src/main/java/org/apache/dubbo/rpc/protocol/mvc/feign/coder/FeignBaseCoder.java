package org.apache.dubbo.rpc.protocol.mvc.feign.coder;

import org.apache.dubbo.rpc.protocol.mvc.servlet.MvcConfigurationSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;

import java.util.*;

public class FeignBaseCoder {
    protected static List<HttpMessageConverter> httpMessageConverters;

    public FeignBaseCoder() {
        httpMessageConverters = getHttpMessageConverters();
    }


    // TODO add  WebMvcConfigurationSupport , fastjson , extend
    public static List<HttpMessageConverter> getHttpMessageConverters() {

        return new ArrayList<>(MvcConfigurationSupport.getDefaultHttpMessageConverters());
//        List<HttpMessageConverter> converters = new ArrayList();
//        if (ClassUtils.isPresent("org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport", (ClassLoader) null)) {
//            converters.addAll((new WebMvcConfigurationSupport() {
//                public List<HttpMessageConverter<?>> defaultMessageConverters() {
//                    return super.getMessageConverters();
//                }
//            }).defaultMessageConverters());
//        } else {
//            converters.addAll((new RestTemplate()).getMessageConverters());
//        }


//        converters.add(new ByteArrayHttpMessageConverter());
//        converters.add(new StringHttpMessageConverter());
//        converters.add(new ResourceHttpMessageConverter());
//        converters.add(new SourceHttpMessageConverter());
//        converters.add(new AllEncompassingFormHttpMessageConverter());
//        converters.add(new MappingJackson2HttpMessageConverter());
//        converters.add(new Jaxb2RootElementHttpMessageConverter());
//
//        return converters;
    }

    protected static HttpHeaders getHttpHeaders(Map<String, Collection<String>> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
            httpHeaders.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return httpHeaders;
    }

    protected static Map<String, Collection<String>> getHeaders(HttpHeaders httpHeaders) {
        LinkedHashMap<String, Collection<String>> headers = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : httpHeaders.entrySet()) {
            headers.put(entry.getKey(), entry.getValue());
        }

        return headers;
    }
}
