package com.alibaba.dubbo.remoting.http.none;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.support.AbstractHttpServer;

/**
 * Created by wuyu on 2016/6/9.
 */
public class NoneHttpServer extends AbstractHttpServer{

    public NoneHttpServer(URL url, HttpHandler handler) {
        super(url, handler);
    }
}
