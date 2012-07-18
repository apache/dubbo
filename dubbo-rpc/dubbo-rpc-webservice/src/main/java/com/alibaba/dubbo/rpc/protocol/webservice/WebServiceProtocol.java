/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.webservice;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.http.HttpDestinationFactory;
import org.apache.cxf.transport.servlet.ServletController;
import org.apache.cxf.transport.servlet.ServletDestinationFactory;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.remoting.http.servlet.DispatcherServlet;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;

/**
 * WebServiceProtocol.
 * 
 * @author netcomm
 */
public class WebServiceProtocol extends AbstractProxyProtocol {
    
    public static final int DEFAULT_PORT = 80;

    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<String, HttpServer>();
    
    private final ExtensionManagerBus bus = new ExtensionManagerBus();

    private final HTTPTransportFactory transportFactory = new HTTPTransportFactory(bus);
	
    private HttpBinder httpBinder;
    
    public WebServiceProtocol() {
        super(Fault.class);
        bus.setExtension(new ServletDestinationFactory(), HttpDestinationFactory.class);
    }

    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }

    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    private class WebServiceHandler implements HttpHandler {

    	private volatile ServletController servletController;

        public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        	if (servletController == null) {
        		HttpServlet httpServlet = DispatcherServlet.getInstance();
    			if (httpServlet == null) {
    				response.sendError(500, "No such DispatcherServlet instance.");
    				return;
    			}
        		synchronized (this) {
        			if (servletController == null) {
            			servletController = new ServletController(transportFactory.getRegistry(), httpServlet.getServletConfig(), httpServlet);
        			}
				}
        	}
            RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());
            servletController.invoke(request, response);
        }

    }

    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
    	String addr = url.getIp() + ":" + url.getPort();
        HttpServer httpServer = serverMap.get(addr);
        if (httpServer == null) {
            httpServer = httpBinder.bind(url, new WebServiceHandler());
            serverMap.put(addr, httpServer);
        }
        final ServerFactoryBean serverFactoryBean = new ServerFactoryBean();
        serverFactoryBean.setAddress(url.getAbsolutePath());
    	serverFactoryBean.setServiceClass(type);
    	serverFactoryBean.setServiceBean(impl);
    	serverFactoryBean.setBus(bus);
        serverFactoryBean.setDestinationFactory(transportFactory);
    	serverFactoryBean.create();
        return new Runnable() {
            public void run() {
            	serverFactoryBean.destroy();
            }
        };
    }

    @SuppressWarnings("unchecked")
    protected <T> T doRefer(final Class<T> serviceType, final URL url) throws RpcException {
    	ClientProxyFactoryBean proxyFactoryBean = new ClientProxyFactoryBean();
    	proxyFactoryBean.setAddress(url.setProtocol("http").toIdentityString());
    	proxyFactoryBean.setServiceClass(serviceType);
    	proxyFactoryBean.setBus(bus);
    	T ref = (T) proxyFactoryBean.create();
    	Client proxy = ClientProxy.getClient(ref);  
		HTTPConduit conduit = (HTTPConduit) proxy.getConduit();
		HTTPClientPolicy policy = new HTTPClientPolicy();
		policy.setConnectionTimeout(url.getParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT));
		policy.setReceiveTimeout(url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
		conduit.setClient(policy);
        return ref;
    }

    protected int getErrorCode(Throwable e) {
    	if (e instanceof Fault) {
            e = e.getCause();
        }
        if (e instanceof SocketTimeoutException) {
            return RpcException.TIMEOUT_EXCEPTION;
        } else if (e instanceof IOException) {
            return RpcException.NETWORK_EXCEPTION;
        }
        return super.getErrorCode(e);
    }

}