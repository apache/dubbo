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
package com.alibaba.dubbo.rpc.protocol.hessian;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProtocol;

/**
 * http rpc support.
 * 
 * @author qianlei
 */
@Extension("hessian")
public class HessianProtocol extends AbstractProtocol {

    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<String, HttpServer>();

    private HttpBinder               httpTransporter;

    private ProxyFactory             proxyFactory;

    public void setHttpTransporter(HttpBinder httpTransporter) {
        this.httpTransporter = httpTransporter;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public int getDefaultPort() {
        return 80;
    }
    
    private class HessianHandler implements HttpHandler {
        
        public void handle(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            String uri = request.getRequestURI();
            HessianRpcExporter<?> exporter = (HessianRpcExporter<?>) exporterMap.get(uri);
            exporter.handle(request, response);
        }
        
    }

    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        final URL url = invoker.getUrl();
        final String uri = url.getAbsolutePath(); // service uri also exporter cache key.

        String addr = url.getIp() + ":" + url.getPort();
        HttpServer server = serverMap.get(addr);
        if (server == null) {
            server = httpTransporter.bind(url, new HessianHandler());
            serverMap.put(addr, server);
        }

        HessianRpcExporter<T> exporter = new HessianRpcExporter<T>(invoker, proxyFactory) {
            public void unexport() {
                super.unexport();
                exporterMap.remove(uri);
            }
        };
        exporterMap.put(uri, exporter);
        return exporter;
    }

    public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException {
        Invoker<T> invoker = new HessianRpcInvoker<T>(serviceType, url, proxyFactory);
        invokers.add(invoker);
        return invoker;
    }

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

}