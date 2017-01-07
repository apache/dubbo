package com.alibaba.dubbo.remoting.http.none;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;

/**
 * Created by wuyu on 2016/6/9.
 */
public class NoneBinder implements HttpBinder {
    @Override
    public HttpServer bind(URL url, HttpHandler handler) {
        return new NoneHttpServer(url, handler);
    }
}
