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

package org.apache.dubbo.rpc.protocol.rest.factory;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.rpc.RpcException;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * AbstractHttpClientFactory
 */
public abstract class AbstractHttpClientFactory implements RestClientFactory {

    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    protected static final int HTTP_CLIENT_CONNECTION_MANAGER_MAX_PER_ROUTE = 20;
    protected static final int HTTP_CLIENT_CONNECTION_MANAGER_MAX_TOTAL = 20;
    protected static final int HTTPCLIENT_KEEP_ALIVE_DURATION = 30 * 1000;
    protected static final int HTTP_CLIENT_CONNECTION_MANAGER_CLOSE_WAIT_TIME_MS = 1000;
    protected static final int HTTP_CLIENT_CONNECTION_MANAGER_CLOSE_IDLE_TIME_S = 30;

    protected volatile ConnectionMonitor connectionMonitor;



    //////////////////////////////////////// implements start ///////////////////////////////////////////////
    @Override
    public RestClient createRestClient(URL url) throws RpcException {

        beforeCreated(url);

        // create a raw client
        RestClient restClient = doCreateRestClient(url);

        // postprocessor
        afterCreated(restClient);

        return restClient;
    }

    @Override
    public void shutdown() {
        connectionMonitor.shutdown();
    }

    @Override
    public void destroy(URL url) throws Exception {
        connectionMonitor.destroyManager(url);
    }

    //////////////////////////////////////// implements end ///////////////////////////////////////////////



    ////////////////////////////////////////   inner methods  ///////////////////////////////////////////////

    protected void beforeCreated(URL url){}

    protected abstract RestClient doCreateRestClient(URL url) throws RpcException;

    protected void afterCreated(RestClient client){}

    ////////////////////////////////////////   inner methods  ///////////////////////////////////////////////



    public static class ConnectionMonitor extends Thread {
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


}
