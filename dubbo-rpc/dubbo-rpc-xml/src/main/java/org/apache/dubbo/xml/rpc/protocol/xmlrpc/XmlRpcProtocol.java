/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.xml.rpc.protocol.xmlrpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.http.HttpBinder;
import org.apache.dubbo.remoting.http.HttpHandler;
import org.apache.dubbo.remoting.http.HttpServer;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractProxyProtocol;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;
import org.springframework.remoting.RemoteAccessException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XmlRpcProtocol extends AbstractProxyProtocol {
    
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER = "Access-Control-Allow-Headers";

    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<>();

    private final Map<String, XmlRpcServletServer> skeletonMap = new ConcurrentHashMap<>();

    private HttpBinder httpBinder;

    public XmlRpcProtocol() {
        super(XmlRpcException.class);
    }

    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }

    @Override
    public int getDefaultPort() {
        return 80;
    }

    private class InternalHandler implements HttpHandler {
    	
        private boolean cors;

        public InternalHandler(boolean cors) {
            this.cors = cors;
        }

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            String uri = request.getRequestURI();
            XmlRpcServletServer xmlrpc = skeletonMap.get(uri);
            if (cors) {
                response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
                response.setHeader(ACCESS_CONTROL_ALLOW_METHODS_HEADER, "POST");
                response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS_HEADER, "*");
            }
            if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
                response.setStatus(200);
            } else if (request.getMethod().equalsIgnoreCase("POST")) {

                RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());
                try {
                    xmlrpc.execute (request,response);
                } catch (Throwable e) {
                    throw new ServletException(e);
                }
            } else {
                response.setStatus(500);
            }
        }

    }

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        final URL httpUrl = url.setProtocol("http");
        String addr = httpUrl.getIp() + ":" + httpUrl.getPort();
        HttpServer server = serverMap.get(addr);
        if (server == null) {
            server = httpBinder.bind(httpUrl, new InternalHandler(httpUrl.getParameter("cors", false)));
            serverMap.put(addr, server);
        }
        final String path = httpUrl.getAbsolutePath();

        XmlRpcServletServer xmlRpcServer = new XmlRpcServletServer();

        PropertyHandlerMapping propertyHandlerMapping = new PropertyHandlerMapping();
        try {

            propertyHandlerMapping.setRequestProcessorFactoryFactory(new RequestProcessorFactoryFactory(){
                @Override
                public RequestProcessorFactory getRequestProcessorFactory(Class pClass) throws XmlRpcException{
                    return new RequestProcessorFactory(){
                        @Override
                        public Object getRequestProcessor(XmlRpcRequest pRequest) throws XmlRpcException{
                            return impl;
                        }
                    };
                }
            });

            propertyHandlerMapping.addHandler(XmlRpcProxyFactoryBean.replace(type.getName()),type);

        } catch (Exception e) {
            throw new RpcException(e);
        }
        xmlRpcServer.setHandlerMapping(propertyHandlerMapping);

        XmlRpcServerConfigImpl xmlRpcServerConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
        xmlRpcServerConfig.setEnabledForExceptions(true);
        xmlRpcServerConfig.setContentLengthOptional(false);

        skeletonMap.put(path, xmlRpcServer);
        return new Runnable() {
            @Override
            public void run() {
                skeletonMap.remove(path);
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    @Override
    protected <T> T doRefer(final Class<T> serviceType, URL url) throws RpcException {
        XmlRpcProxyFactoryBean xmlRpcProxyFactoryBean = new XmlRpcProxyFactoryBean();
        xmlRpcProxyFactoryBean.setServiceUrl(url.setProtocol("http").toIdentityString());
        xmlRpcProxyFactoryBean.setServiceInterface(serviceType);
        xmlRpcProxyFactoryBean.afterPropertiesSet();
        return (T) xmlRpcProxyFactoryBean.getObject();
    }

    @Override
    protected int getErrorCode(Throwable e) {
        if (e instanceof RemoteAccessException) {
            e = e.getCause();
        }
        if (e != null) {
            Class<?> cls = e.getClass();
            if (SocketTimeoutException.class.equals(cls)) {
                return RpcException.TIMEOUT_EXCEPTION;
            } else if (IOException.class.isAssignableFrom(cls)) {
                return RpcException.NETWORK_EXCEPTION;
            } else if (ClassNotFoundException.class.isAssignableFrom(cls)) {
                return RpcException.SERIALIZATION_EXCEPTION;
            }
        }
        return super.getErrorCode(e);
    }

    @Override
    public void destroy() {
        super.destroy();
        for (String key : new ArrayList<>(serverMap.keySet())) {
            HttpServer server = serverMap.remove(key);
            if (server != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close xml server " + server.getUrl());
                    }
                    server.close();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
    }
}