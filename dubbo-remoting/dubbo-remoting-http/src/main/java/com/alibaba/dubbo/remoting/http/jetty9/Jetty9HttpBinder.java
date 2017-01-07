package com.alibaba.dubbo.remoting.http.jetty9;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;

/**
 * JettyHttpTransporter
 * 
 * @author william.liangf
 */
public class Jetty9HttpBinder implements HttpBinder {

	public HttpServer bind(URL url, HttpHandler handler) {
		return new Jetty9HttpServer(url, handler);
	}

}