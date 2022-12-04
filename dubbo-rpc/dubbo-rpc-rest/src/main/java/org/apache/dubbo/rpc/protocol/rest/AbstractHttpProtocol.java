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
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_CLIENT;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_SERVER;
import static org.apache.dubbo.remoting.Constants.*;
import static org.apache.dubbo.rpc.protocol.rest.Constants.EXTENSION_KEY;

public abstract class AbstractHttpProtocol extends AbstractProxyProtocol {


    private static final int HTTP_CLIENT_CONNECTION_MANAGER_MAX_PER_ROUTE = 20;
    private static final int HTTP_CLIENT_CONNECTION_MANAGER_MAX_TOTAL = 20;
    private static final int HTTP_CLIENT_KEEP_ALIVE_DURATION = 30 * 1000;
    private static final int HTTP_CLIENT_CONNECTION_MANAGER_CLOSE_WAIT_TIME_MS = 1000;
    private static final int HTTP_CLIENT_CONNECTION_MANAGER_CLOSE_IDLE_TIME_S = 30;

    protected volatile ConnectionMonitor connectionMonitor;

    protected final Map<String, ReferenceCountedClient> clients = new ConcurrentHashMap<>();

    public AbstractHttpProtocol() {
        super(WebApplicationException.class, ProcessingException.class);
    }



    protected ReferenceCountedClient getOrCreatePoolClient(URL url) {

        ReferenceCountedClient  referenceCountedClient;
        if ((referenceCountedClient = clients.get(url.getAddress())) != null) return referenceCountedClient;

        // create httpclient
        CloseableHttpClient httpClient = createPoolClient(url);

        ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient/*, localContext*/);

        // TODO
        ResteasyClient resteasyClient = new ResteasyClientBuilder().httpEngine(engine).build();
        resteasyClient.register(RpcContextFilter.class);


        referenceCountedClient = new ReferenceCountedClient(resteasyClient);
        referenceCountedClient.retain();

        for (String clazz : COMMA_SPLIT_PATTERN.split(url.getParameter(EXTENSION_KEY, ""))) {
            if (!StringUtils.isEmpty(clazz)) {
                try {
                    referenceCountedClient.getClient().register(Thread.currentThread().getContextClassLoader().loadClass(clazz.trim()));
                } catch (ClassNotFoundException e) {
                    throw new RpcException("Error loading JAX-RS extension class: " + clazz.trim(), e);
                }
            }
        }

        return referenceCountedClient;
    }


    protected CloseableHttpClient createPoolClient(URL url) {

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        // 20 is the default maxTotal of current PoolingClientConnectionManager
        connectionManager.setMaxTotal(url.getParameter(CONNECTIONS_KEY, HTTP_CLIENT_CONNECTION_MANAGER_MAX_TOTAL));
        connectionManager.setDefaultMaxPerRoute(url.getParameter(CONNECTIONS_KEY, HTTP_CLIENT_CONNECTION_MANAGER_MAX_PER_ROUTE));
        if (connectionMonitor == null) {
            connectionMonitor = new ConnectionMonitor();
            connectionMonitor.start();
        }
        connectionMonitor.addConnectionManager(url.getAddress(), connectionManager);

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(url.getParameter(CONNECT_TIMEOUT_KEY, DEFAULT_CONNECT_TIMEOUT))
            .setSocketTimeout(url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT))
            .build();

        SocketConfig socketConfig = SocketConfig.custom()
            .setSoKeepAlive(true)
            .setTcpNoDelay(true)
            .build();

        return HttpClientBuilder.create()
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
                return HTTP_CLIENT_KEEP_ALIVE_DURATION;
            })
            .setDefaultRequestConfig(requestConfig)
            .setDefaultSocketConfig(socketConfig)
            .build();
    }

    private static class ConnectionMonitor extends Thread {
        private volatile boolean shutdown;
        /**
         * The lifecycle of {@code PoolingHttpClientConnectionManager} instance is bond with ReferenceCountedClient
         */
        private final Map<String, PoolingHttpClientConnectionManager> connectionManagers = new ConcurrentHashMap<>();

        public void addConnectionManager(String address, PoolingHttpClientConnectionManager connectionManager) {
            connectionManagers.putIfAbsent(address, connectionManager);
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(HTTP_CLIENT_CONNECTION_MANAGER_CLOSE_WAIT_TIME_MS);
                        for (PoolingHttpClientConnectionManager connectionManager : connectionManagers.values()) {
                            connectionManager.closeExpiredConnections();
                            connectionManager.closeIdleConnections(HTTP_CLIENT_CONNECTION_MANAGER_CLOSE_IDLE_TIME_S, TimeUnit.SECONDS);
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

        // destroy the connection manager of a specific address when ReferenceCountedClient is destroyed.
        private void destroyManager(URL url) {
            PoolingHttpClientConnectionManager connectionManager = connectionManagers.remove(url.getAddress());
            if (connectionManager != null) {
                connectionManager.close();
            }
        }
    }

    @Override
    protected void destroyInternal(URL url) {
        try {
            ReferenceCountedClient referenceCountedClient = clients.get(url.getAddress());
            if (referenceCountedClient != null && referenceCountedClient.release()) {
                clients.remove(url.getAddress());
                connectionMonitor.destroyManager(url);
            }
        } catch (Exception e) {
            logger.warn(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", "Failed to close unused resources in rest protocol. interfaceName [" + url.getServiceInterface() + "]", e);
        }
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
                logger.warn(PROTOCOL_ERROR_CLOSE_SERVER, "", "", "Error closing rest server", t);
            }
        }
        serverMap.clear();

        if (logger.isInfoEnabled()) {
            logger.info("Closing rest clients");
        }
        for (ReferenceCountedClient client : clients.values()) {
            try {
                // destroy directly regardless of the current reference count.
                client.destroy();
            } catch (Throwable t) {
                logger.warn(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", "Error closing rest client", t);
            }
        }
        clients.clear();
    }
}
