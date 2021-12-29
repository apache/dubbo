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
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http.HttpBinder;
import org.apache.dubbo.remoting.http.servlet.BootstrapListener;
import org.apache.dubbo.remoting.http.servlet.ServletManager;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractProxyProtocol;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
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
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.remoting.Constants.CONNECTIONS_KEY;
import static org.apache.dubbo.remoting.Constants.CONNECT_TIMEOUT_KEY;
import static org.apache.dubbo.remoting.Constants.DEFAULT_CONNECT_TIMEOUT;
import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.apache.dubbo.rpc.protocol.rest.Constants.EXTENSION_KEY;

public class RestProtocol extends AbstractProxyProtocol {

    private static final int DEFAULT_PORT = 80;
    private static final String DEFAULT_SERVER = "jetty";

    private static final int HTTPCLIENTCONNECTIONMANAGER_MAXPERROUTE = 20;
    private static final int HTTPCLIENTCONNECTIONMANAGER_MAXTOTAL = 20;
    private static final int HTTPCLIENT_KEEPALIVEDURATION = 30 * 1000;
    private static final int HTTPCLIENTCONNECTIONMANAGER_CLOSEWAITTIME_MS = 1000;
    private static final int HTTPCLIENTCONNECTIONMANAGER_CLOSEIDLETIME_S = 30;

    private final RestServerFactory serverFactory = new RestServerFactory();

    // TODO in the future maybe we can just use a single rest client and connection manager
    private final List<ResteasyClient> clients = Collections.synchronizedList(new LinkedList<>());

    private volatile ConnectionMonitor connectionMonitor;

    public RestProtocol() {
        super(WebApplicationException.class, ProcessingException.class);
    }

    public void setHttpBinder(HttpBinder httpBinder) {
        serverFactory.setHttpBinder(httpBinder);
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        String addr = getAddr(url);
        Class implClass = url.getServiceModel().getProxyObject().getClass();
        RestProtocolServer server = (RestProtocolServer) serverMap.computeIfAbsent(addr, restServer -> {
            RestProtocolServer s = serverFactory.createServer(url.getParameter(SERVER_KEY, DEFAULT_SERVER));
            s.setAddress(url.getAddress());
            s.start(url);
            return s;
        });

        String contextPath = getContextPath(url);
        if ("servlet".equalsIgnoreCase(url.getParameter(SERVER_KEY, DEFAULT_SERVER))) {
            ServletContext servletContext = ServletManager.getInstance().getServletContext(ServletManager.EXTERNAL_SERVER_PORT);
            if (servletContext == null) {
                throw new RpcException("No servlet context found. Since you are using server='servlet', " +
                        "make sure that you've configured " + BootstrapListener.class.getName() + " in web.xml");
            }
            String webappPath = servletContext.getContextPath();
            if (StringUtils.isNotEmpty(webappPath)) {
                webappPath = webappPath.substring(1);
                if (!contextPath.startsWith(webappPath)) {
                    throw new RpcException("Since you are using server='servlet', " +
                            "make sure that the 'contextpath' property starts with the path of external webapp");
                }
                contextPath = contextPath.substring(webappPath.length());
                if (contextPath.startsWith("/")) {
                    contextPath = contextPath.substring(1);
                }
            }
        }

        final Class resourceDef = GetRestful.getRootResourceClass(implClass) != null ? implClass : type;

        server.deploy(resourceDef, impl, contextPath);

        final RestProtocolServer s = server;
        return () -> {
            // TODO due to dubbo's current architecture,
            // it will be called from registry protocol in the shutdown process and won't appear in logs
            s.undeploy(resourceDef);
        };
    }

    @Override
    protected <T> T doRefer(Class<T> serviceType, URL url) throws RpcException {

        // TODO more configs to add
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        // 20 is the default maxTotal of current PoolingClientConnectionManager
        connectionManager.setMaxTotal(url.getParameter(CONNECTIONS_KEY, HTTPCLIENTCONNECTIONMANAGER_MAXTOTAL));
        connectionManager.setDefaultMaxPerRoute(url.getParameter(CONNECTIONS_KEY, HTTPCLIENTCONNECTIONMANAGER_MAXPERROUTE));

        if (connectionMonitor == null) {
            connectionMonitor = new ConnectionMonitor();
            connectionMonitor.start();
        }
        connectionMonitor.addConnectionManager(connectionManager);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(url.getParameter(CONNECT_TIMEOUT_KEY, DEFAULT_CONNECT_TIMEOUT))
                .setSocketTimeout(url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT))
                .build();

        SocketConfig socketConfig = SocketConfig.custom()
                .setSoKeepAlive(true)
                .setTcpNoDelay(true)
                .build();

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setKeepAliveStrategy((response, context) -> {
                    HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                    while (it.hasNext()) {
                        HeaderElement he = it.nextElement();
                        String param = he.getName();
                        String value = he.getValue();
                        if (value != null && param.equalsIgnoreCase(TIMEOUT_KEY)) {
                            return Long.parseLong(value) * 1000;
                        }
                    }
                    return HTTPCLIENT_KEEPALIVEDURATION;
                })
                .setDefaultRequestConfig(requestConfig)
                .setDefaultSocketConfig(socketConfig)
                .build();

        ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient/*, localContext*/);

        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        clients.add(client);

        client.register(RpcContextFilter.class);
        for (String clazz : COMMA_SPLIT_PATTERN.split(url.getParameter(EXTENSION_KEY, ""))) {
            if (!StringUtils.isEmpty(clazz)) {
                try {
                    client.register(Thread.currentThread().getContextClassLoader().loadClass(clazz.trim()));
                } catch (ClassNotFoundException e) {
                    throw new RpcException("Error loading JAX-RS extension class: " + clazz.trim(), e);
                }
            }
        }

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
        if (logger.isInfoEnabled()) {
            logger.info("Destroying protocol [" + this.getClass().getSimpleName() + "] ...");
        }
        super.destroy();

        if (connectionMonitor != null) {
            connectionMonitor.shutdown();
        }

        for (Map.Entry<String, ProtocolServer> entry : serverMap.entrySet()) {
            try {
                if (logger.isInfoEnabled()) {
                    logger.info("Closing the rest server at " + entry.getKey());
                }
                entry.getValue().close();
            } catch (Throwable t) {
                logger.warn("Error closing rest server", t);
            }
        }
        serverMap.clear();

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

    /**
     *  getPath() will return: [contextpath + "/" +] path
     *  1. contextpath is empty if user does not set through ProtocolConfig or ProviderConfig
     *  2. path will never be empty, it's default value is the interface name.
     *
     * @return return path only if user has explicitly gave then a value.
     */
    protected String getContextPath(URL url) {
        String contextPath = url.getPath();
        if (contextPath != null) {
            if (contextPath.equalsIgnoreCase(url.getParameter(INTERFACE_KEY))) {
                return "";
            }
            if (contextPath.endsWith(url.getParameter(INTERFACE_KEY))) {
                contextPath = contextPath.substring(0, contextPath.lastIndexOf(url.getParameter(INTERFACE_KEY)));
            }
            return contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath;
        } else {
            return "";
        }
    }

    protected class ConnectionMonitor extends Thread {
        private volatile boolean shutdown;
        private final List<PoolingHttpClientConnectionManager> connectionManagers = Collections.synchronizedList(new LinkedList<>());

        public void addConnectionManager(PoolingHttpClientConnectionManager connectionManager) {
            connectionManagers.add(connectionManager);
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(HTTPCLIENTCONNECTIONMANAGER_CLOSEWAITTIME_MS);
                        for (PoolingHttpClientConnectionManager connectionManager : connectionManagers) {
                            connectionManager.closeExpiredConnections();
                            connectionManager.closeIdleConnections(HTTPCLIENTCONNECTIONMANAGER_CLOSEIDLETIME_S, TimeUnit.SECONDS);
                        }
                    }
                }
            } catch (InterruptedException ex) {
                shutdown();
            }
        }

        public void shutdown() {
            shutdown = true;
            connectionManagers.clear();
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
