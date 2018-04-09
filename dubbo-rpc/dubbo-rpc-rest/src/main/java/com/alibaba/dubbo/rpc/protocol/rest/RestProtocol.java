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
package com.alibaba.dubbo.rpc.protocol.rest;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.servlet.BootstrapListener;
import com.alibaba.dubbo.remoting.http.servlet.ServletManager;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.ServiceClassHolder;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jboss.resteasy.util.GetRestful;

import javax.servlet.ServletContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RestProtocol extends AbstractProxyProtocol {

    /**
     * 服务器默认端口
     */
    private static final int DEFAULT_PORT = 80;

    /**
     * 服务器集合
     *
     * key：ip:port
     */
    private final Map<String, RestServer> servers = new ConcurrentHashMap<String, RestServer>();
    /**
     * 服务器工厂，负责创建服务器
     */
    private final RestServerFactory serverFactory = new RestServerFactory();
    /**
     * 客户端数组
     */
    // TODO in the future maybe we can just use a single rest client and connection manager
    private final List<ResteasyClient> clients = Collections.synchronizedList(new LinkedList<ResteasyClient>());
    /**
     * 连接监控器
     */
    private volatile ConnectionMonitor connectionMonitor;

    public RestProtocol() {
        super(WebApplicationException.class, ProcessingException.class);
    }

    public void setHttpBinder(HttpBinder httpBinder) {
        serverFactory.setHttpBinder(httpBinder);
    }

    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        // 获得服务器地址
        String addr = getAddr(url);
        // 获得服务的真实类名，例如 DemoServiceImpl
        Class implClass = ServiceClassHolder.getInstance().popServiceClass();
        // 获得 RestServer 对象。若不存在，进行创建。
        RestServer server = servers.get(addr);
        if (server == null) {
            server = serverFactory.createServer(url.getParameter(Constants.SERVER_KEY, "jetty"));
            server.start(url); // 启动
            servers.put(addr, server);
        }

        // 获得 ContextPath 路径。
        String contextPath = getContextPath(url);
        if ("servlet".equalsIgnoreCase(url.getParameter(Constants.SERVER_KEY, "jetty"))) { // 外部的容器，需要从 ServletContext 中获得。
            ServletContext servletContext = ServletManager.getInstance().getServletContext(ServletManager.EXTERNAL_SERVER_PORT);
            if (servletContext == null) {
                throw new RpcException("No servlet context found. Since you are using server='servlet', " +
                        "make sure that you've configured " + BootstrapListener.class.getName() + " in web.xml");
            }
            String webappPath = servletContext.getContextPath();
            if (StringUtils.isNotEmpty(webappPath)) {
                // 去掉 `/` 起始
                webappPath = webappPath.substring(1);
                // 校验 URL 中配置的 `contextPath` 是外部容器的 `contextPath` 起始。
                if (!contextPath.startsWith(webappPath)) {
                    throw new RpcException("Since you are using server='servlet', " +
                            "make sure that the 'contextpath' property starts with the path of external webapp");
                }
                // 截取掉起始部分
                contextPath = contextPath.substring(webappPath.length());
                // 去掉 `/` 起始
                if (contextPath.startsWith("/")) {
                    contextPath = contextPath.substring(1);
                }
            }
        }
        // 获得以 `@Path` 为注解的基础类，一般情况下，我们直接在 `implClass` 上添加了该注解，即就是 `implClass` 类。
        final Class resourceDef = GetRestful.getRootResourceClass(implClass) != null ? implClass : type;
        // 部署到服务器上
        server.deploy(resourceDef, impl, contextPath);
        // 返回取消暴露的回调 Runnable
        final RestServer s = server;
        return new Runnable() {
            public void run() {
                // TODO due to dubbo's current architecture,
                // it will be called from registry protocol in the shutdown process and won't appear in logs
                s.undeploy(resourceDef);
            }
        };
    }

    @Override
    protected <T> T doRefer(Class<T> serviceType, URL url) throws RpcException {
        // 创建 ConnectionMonitor 对象。
        if (connectionMonitor == null) {
            connectionMonitor = new ConnectionMonitor();
        }

        // 创建 HttpClient 连接池管理器
        // TODO more configs to add
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        // 20 is the default maxTotal of current PoolingClientConnectionManager
        connectionManager.setMaxTotal(url.getParameter(Constants.CONNECTIONS_KEY, 20)); // 最大请求数
        connectionManager.setDefaultMaxPerRoute(url.getParameter(Constants.CONNECTIONS_KEY, 20)); // 每个路由，最大请求数
        // 添加到 ConnectionMonitor 中。
        connectionMonitor.addConnectionManager(connectionManager);

        // 创建 RequestConfig 对象
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT)) // 连接超时
                .setSocketTimeout(url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT)) // 请求超时
                .build();
        // 创建 SocketConfig 对象
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoKeepAlive(true) // 保持连接
                .setTcpNoDelay(true)
                .build();
        // 创建 HttpClient 对象 【Apache】
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setKeepAliveStrategy(new ConnectionKeepAliveStrategy() { // 优先以服务器返回的为准，缺省为 30 秒。
                    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                        HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                        while (it.hasNext()) {
                            HeaderElement he = it.nextElement();
                            String param = he.getName();
                            String value = he.getValue();
                            if (value != null && param.equalsIgnoreCase("timeout")) {
                                return Long.parseLong(value) * 1000;
                            }
                        }
                        // TODO constant
                        return 30 * 1000;
                    }
                })
                .setDefaultRequestConfig(requestConfig)
                .setDefaultSocketConfig(socketConfig)
                .build();

        // 创建 ApacheHttpClient4Engine 对象 【Resteasy】
        ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient/*, localContext*/);
        // 创建 ResteasyClient 对象 【Resteasy】
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        // 添加到客户端集合
        clients.add(client);

        // 设置 RpcContextFilter 过滤器
        client.register(RpcContextFilter.class);
        // 从 `extension` 配置项，设置对应的组件（过滤器 Filter 、拦截器 Interceptor 、异常匹配器 ExceptionMapper 等等）
        for (String clazz : Constants.COMMA_SPLIT_PATTERN.split(url.getParameter(Constants.EXTENSION_KEY, ""))) {
            if (!StringUtils.isEmpty(clazz)) {
                try {
                    client.register(Thread.currentThread().getContextClassLoader().loadClass(clazz.trim()));
                } catch (ClassNotFoundException e) {
                    throw new RpcException("Error loading JAX-RS extension class: " + clazz.trim(), e);
                }
            }
        }

        // 创建 Service Proxy 对象。
        // TODO protocol
        ResteasyWebTarget target = client.target("http://" + url.getHost() + ":" + url.getPort() + "/" + getContextPath(url));
        return target.proxy(serviceType);
    }

    @Override
    protected int getErrorCode(Throwable e) {
        // TODO
        return super.getErrorCode(e);
    }

    @Override
    public void destroy() {
        // 父类销毁
        super.destroy();

        // 关闭 ConnectionMonitor
        if (connectionMonitor != null) {
            connectionMonitor.shutdown();
        }

        // 关闭服务器
        for (Map.Entry<String, RestServer> entry : servers.entrySet()) {
            try {
                if (logger.isInfoEnabled()) {
                    logger.info("Closing the rest server at " + entry.getKey());
                }
                entry.getValue().stop();
            } catch (Throwable t) {
                logger.warn("Error closing rest server", t);
            }
        }
        servers.clear();

        // 关闭客户端
        if (logger.isInfoEnabled()) {
            logger.info("Closing rest clients");
        }
        for (ResteasyClient client : clients) {
            try {
                client.close();
            } catch (Throwable t) {
                logger.warn("Error closing rest client", t);
            }
        }
        clients.clear();
    }

    protected String getContextPath(URL url) {
        int pos = url.getPath().lastIndexOf("/"); // 尾部 / 之后
        return pos > 0 ? url.getPath().substring(0, pos) : "";
    }

    protected class ConnectionMonitor extends Thread {

        /**
         * 是否关闭
         */
        private volatile boolean shutdown;
        /**
         * HttpClient 连接池管理器集合
         */
        private final List<PoolingHttpClientConnectionManager> connectionManagers = Collections.synchronizedList(new LinkedList<PoolingHttpClientConnectionManager>());

        public void addConnectionManager(PoolingHttpClientConnectionManager connectionManager) {
            connectionManagers.add(connectionManager);
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        // 等待 1000 ms
                        wait(1000); // 可被下面 shutdown() 方法，强行关闭
                        for (PoolingHttpClientConnectionManager connectionManager : connectionManagers) {
                            connectionManager.closeExpiredConnections();
                            // TODO constant
                            connectionManager.closeIdleConnections(30, TimeUnit.SECONDS);
                        }
                    }
                }
            } catch (InterruptedException ex) {
                shutdown();
            }
        }

        public void shutdown() {
            // 标记关闭
            shutdown = true;
            // 清除管理器集合
            connectionManagers.clear();
            // 唤醒等待线程
            synchronized (this) {
                notifyAll();
            }
        }

    }

}