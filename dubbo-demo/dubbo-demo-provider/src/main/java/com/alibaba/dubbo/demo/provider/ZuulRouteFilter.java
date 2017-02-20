package com.alibaba.dubbo.demo.provider;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by wuyu on 2017/2/12.
 */
public class ZuulRouteFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return "route";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        try {

            ServletInputStream inputStream = request.getInputStream();
            CloseableHttpClient httpClient = getHttpClient();
            String requestURI = request.getRequestURI();
//            InputStreamEntity inputStreamEntity = new InputStreamEntity(inputStream);
            HttpGet httpGet = new HttpGet("http://localhost:8080/" + requestURI);
//            httpGet.setEntity(inputStreamEntity);
            CloseableHttpResponse execute = httpClient.execute(httpGet);
            InputStream content = execute.getEntity().getContent();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(content, out);
            byte[] bytes = out.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Hello";
    }

    public CloseableHttpClient getHttpClient() {
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(5000)
                .build();
        CloseableHttpClient httpclient = HttpClientBuilder.create()
                .setConnectionManager(manager)
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnPerRoute(50)
                .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
                .setMaxConnTotal(50)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(0, true))
                .build();
        return httpclient;
    }
}
