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
package com.alibaba.dubbo.rpc.protocol.hessian;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import com.alibaba.dubbo.rpc.protocol.hessian.serialization.Hessian2FactoryUtil;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.dubbo.rpc.support.ProtocolUtils;
import com.caucho.hessian.HessianException;
import com.caucho.hessian.client.HessianConnectionException;
import com.caucho.hessian.client.HessianConnectionFactory;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.io.HessianMethodSerializationException;
import com.caucho.hessian.server.HessianSkeleton;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * http rpc support.
 */
public class HessianProtocol extends AbstractProxyProtocol {

    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<String, HttpServer>();

    private final Map<String, HessianSkeleton> skeletonMap = new ConcurrentHashMap<String, HessianSkeleton>();

    private HttpBinder httpBinder;

    public HessianProtocol() {
        super(HessianException.class);
    }

    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }

    @Override
    public int getDefaultPort() {
        return 80;
    }

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        String addr = getAddr(url);
        HttpServer server = serverMap.get(addr);
        if (server == null) {
            server = httpBinder.bind(url, new HessianHandler());
            serverMap.put(addr, server);
        }
        final String path = url.getAbsolutePath();
        final HessianSkeleton skeleton = new HessianSkeleton(impl, type);
        skeletonMap.put(path, skeleton);

        final String genericPath = path + "/" + Constants.GENERIC_KEY;
        skeletonMap.put(genericPath, new HessianSkeleton(impl, GenericService.class));

        return new Runnable() {
            @Override
            public void run() {
                skeletonMap.remove(path);
                skeletonMap.remove(genericPath);
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRefer(Class<T> serviceType, URL url) throws RpcException {
        String generic = url.getParameter(Constants.GENERIC_KEY);
        boolean isGeneric = ProtocolUtils.isGeneric(generic) || serviceType.equals(GenericService.class);
        if (isGeneric) {
            RpcContext.getContext().setAttachment(Constants.GENERIC_KEY, generic);
            url = url.setPath(url.getPath() + "/" + Constants.GENERIC_KEY);
        }

        HessianProxyFactory hessianProxyFactory = new HessianProxyFactory();
        boolean isHessian2Request = url.getParameter(Constants.HESSIAN2_REQUEST_KEY, Constants.DEFAULT_HESSIAN2_REQUEST);
        hessianProxyFactory.setHessian2Request(isHessian2Request);
        boolean isOverloadEnabled = url.getParameter(Constants.HESSIAN_OVERLOAD_METHOD_KEY, Constants.DEFAULT_HESSIAN_OVERLOAD_METHOD);
        hessianProxyFactory.setOverloadEnabled(isOverloadEnabled);
        String client = url.getParameter(Constants.CLIENT_KEY, Constants.DEFAULT_HTTP_CLIENT);
        if ("httpclient".equals(client)) {
            HessianConnectionFactory factory = new HttpClientConnectionFactory();
            factory.setHessianProxyFactory(hessianProxyFactory);
            hessianProxyFactory.setConnectionFactory(factory);
        } else if (client != null && client.length() > 0 && !Constants.DEFAULT_HTTP_CLIENT.equals(client)) {
            throw new IllegalStateException("Unsupported http protocol client=\"" + client + "\"!");
        } else {
            HessianConnectionFactory factory = new DubboHessianURLConnectionFactory();
            factory.setHessianProxyFactory(hessianProxyFactory);
            hessianProxyFactory.setConnectionFactory(factory);
        }
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        hessianProxyFactory.setConnectTimeout(timeout);
        hessianProxyFactory.setReadTimeout(timeout);
        hessianProxyFactory.setSerializerFactory(Hessian2FactoryUtil.getInstance().getSerializerFactory());
        return (T) hessianProxyFactory.create(serviceType, url.setProtocol("http").toJavaURL(), Thread.currentThread().getContextClassLoader());
    }

    @Override
    protected int getErrorCode(Throwable e) {
        if (e instanceof HessianConnectionException) {
            if (e.getCause() != null) {
                Class<?> cls = e.getCause().getClass();
                if (SocketTimeoutException.class.equals(cls)) {
                    return RpcException.TIMEOUT_EXCEPTION;
                }
            }
            return RpcException.NETWORK_EXCEPTION;
        } else if (e instanceof HessianMethodSerializationException) {
            return RpcException.SERIALIZATION_EXCEPTION;
        }
        return super.getErrorCode(e);
    }

    @Override
    public void destroy() {
        super.destroy();
        for (String key : new ArrayList<String>(serverMap.keySet())) {
            HttpServer server = serverMap.remove(key);
            if (server != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close hessian server " + server.getUrl());
                    }
                    server.close();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
    }

    private class HessianHandler implements HttpHandler {

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            String uri = request.getRequestURI();
            HessianSkeleton skeleton = skeletonMap.get(uri);
            if (!request.getMethod().equalsIgnoreCase("POST")) {
                response.setStatus(500);
            } else {
                RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());

                Enumeration<String> enumeration = request.getHeaderNames();
                while (enumeration.hasMoreElements()) {
                    String key = enumeration.nextElement();
                    if (key.startsWith(Constants.DEFAULT_EXCHANGER)) {
                        RpcContext.getContext().setAttachment(key.substring(Constants.DEFAULT_EXCHANGER.length()),
                                request.getHeader(key));
                    }
                }

                try {
                    skeleton.invoke(request.getInputStream(), response.getOutputStream(), Hessian2FactoryUtil.getInstance().getSerializerFactory());
                } catch (Throwable e) {
                    throw new ServletException(e);
                }
            }
        }

    }

}
