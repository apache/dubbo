package com.alibaba.dubbo.oauth2.support;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Created by wuyu on 2017/1/7.
 */
public class RestInstance {


    public static RestTemplate restTemplate(int timeout,int maxConnTotal) {
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();

        //超时时间
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        //最大并发数
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(manager)
                .setMaxConnPerRoute(maxConnTotal)
                .setMaxConnTotal(maxConnTotal)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getMessageConverters().add(new FastJsonHttpMessageConverter());
        return restTemplate;
    }

}
