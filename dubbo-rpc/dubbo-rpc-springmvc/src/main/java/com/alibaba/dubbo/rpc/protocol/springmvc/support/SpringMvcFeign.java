package com.alibaba.dubbo.rpc.protocol.springmvc.support;

import com.alibaba.dubbo.rpc.protocol.springmvc.annotation.Api;
import com.alibaba.dubbo.rpc.protocol.springmvc.message.MessageConverters;
import feign.Feign;
import feign.Retryer;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.converter.HttpMessageConverter;

import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by wuyu on 2016/6/18.
 */
public class SpringMvcFeign {

    private static final List<HttpMessageConverter<?>> converters = new MessageConverters().getMessageConverters();

    private static final int DEFAULT_MAX_CONN = 20;

    private static final int DEFAULT_TIMEOUT= 2000;

    private static final int DEFAULT_RETRY_COUNT = 5;

    private static final int DEFAULT_RETRY_INTERVAL_TIME = 100;

    public static <T> T target(Class<T> apiType) {
        Api api = apiType.getAnnotation(Api.class);
        if (api == null) {
            throw new IllegalArgumentException("Must carry the @Api annotation");
        }
        return target(apiType, api.value(), api.maxConnTotal(), api.timeout(), api.retry(), api.keepAlive());
    }


    public static <T> T target(Class<T> apiType, String url) {
        return target(apiType, url, DEFAULT_MAX_CONN, DEFAULT_TIMEOUT, DEFAULT_RETRY_COUNT, false);
    }

    public static <T> T target(Class<T> apiType, String url, int maxConn) {
        return target(apiType, url, maxConn, DEFAULT_TIMEOUT, DEFAULT_RETRY_COUNT, false);
    }

    public static <T> T target(Class<T> apiType, String url, int maxConn, int timeout) {
        return target(apiType, url, maxConn, timeout, DEFAULT_RETRY_COUNT, false);
    }

    public static <T> T target(Class<T> apiType, String url, int maxConn, int timeout, int retry) {
        return target(apiType, url, maxConn, timeout, retry, false);
    }

    public static <T> T target(Class<T> apiType, String url, int maxConn, int timeout, int retry, boolean keepAlive) {
        return builder().
                client(new ApacheHttpClient(
                        getDefaultHttpClientPool(
                                maxConn,
                                timeout,
                                0,
                                keepAlive
                        )))
                .retryer(new Retryer.Default(DEFAULT_RETRY_INTERVAL_TIME, SECONDS.toMillis(1), retry))
                .target(apiType, url);
    }

    public static <T> T target(Class<T> apiType, String url, HttpClient httpClient) {
        return builder().
                client(new ApacheHttpClient(httpClient))
                .target(apiType, url);
    }

    public static Feign.Builder builder() {
        return Feign.builder()
                .contract(new SpringMvcContract())
                .decoder(new SpringDecoder(converters))
                .encoder(new SpringEncoder(converters));
    }

    public static HttpClient getDefaultHttpClientPool(int maxTotal, int timeout, int retry, boolean keepAlive) {
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        HttpClientBuilder httpClient = HttpClientBuilder.create()
                .setConnectionManager(manager)
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnPerRoute(maxTotal)
                .setMaxConnTotal(maxTotal)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(retry, true));
        if (keepAlive) {
            httpClient.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());
        }
        return httpClient.build();
    }

    public static List<HttpMessageConverter<?>> getConverters() {
        return converters;
    }

}
