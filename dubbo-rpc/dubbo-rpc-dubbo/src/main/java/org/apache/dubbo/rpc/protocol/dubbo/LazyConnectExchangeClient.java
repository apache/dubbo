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
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.Parameters;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.Exchangers;
import org.apache.dubbo.rpc.RpcException;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.LAZY_CONNECT_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_REQUEST;
import static org.apache.dubbo.remoting.Constants.SEND_RECONNECT_KEY;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.DEFAULT_LAZY_REQUEST_WITH_WARNING;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.LAZY_REQUEST_WITH_WARNING_KEY;

/**
 * dubbo protocol support class.
 */
@SuppressWarnings("deprecation")
final class LazyConnectExchangeClient implements ExchangeClient {

    private final static ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(LazyConnectExchangeClient.class);
    private final boolean requestWithWarning;
    private final URL url;
    private final ExchangeHandler requestHandler;
    private final Lock connectLock = new ReentrantLock();
    private static final int warningPeriod = 5000;
    private final boolean needReconnect;
    private volatile ExchangeClient client;
    private final AtomicLong warningCount = new AtomicLong(0);

    public LazyConnectExchangeClient(URL url, ExchangeHandler requestHandler) {
        // lazy connect, need set send.reconnect = true, to avoid channel bad status.
        this.url = url.addParameter(LAZY_CONNECT_KEY, true);
        this.needReconnect = url.getParameter(SEND_RECONNECT_KEY, false);
        this.requestHandler = requestHandler;
        this.requestWithWarning = url.getParameter(LAZY_REQUEST_WITH_WARNING_KEY, DEFAULT_LAZY_REQUEST_WITH_WARNING);
    }

    private void initClient() throws RemotingException {
        if (client != null) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Lazy connect to " + url);
        }
        connectLock.lock();
        try {
            if (client != null) {
                return;
            }
            this.client = Exchangers.connect(url, requestHandler);
        } finally {
            connectLock.unlock();
        }
    }

    @Override
    public CompletableFuture<Object> request(Object request) throws RemotingException {
        warning();
        checkClient();
        return client.request(request);
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        if (client == null) {
            return InetSocketAddress.createUnresolved(url.getHost(), url.getPort());
        } else {
            return client.getRemoteAddress();
        }
    }

    @Override
    public CompletableFuture<Object> request(Object request, int timeout) throws RemotingException {
        warning();
        checkClient();
        return client.request(request, timeout);
    }

    @Override
    public CompletableFuture<Object> request(Object request, ExecutorService executor) throws RemotingException {
        warning();
        checkClient();
        return client.request(request, executor);
    }

    @Override
    public CompletableFuture<Object> request(Object request, int timeout, ExecutorService executor) throws RemotingException {
        warning();
        checkClient();
        return client.request(request, timeout, executor);
    }

    /**
     * If {@link Constants.LAZY_REQUEST_WITH_WARNING_KEY} is configured, then warn once every 5000 invocations.
     */
    private void warning() {
        if (requestWithWarning) {
            if (warningCount.get() % warningPeriod == 0) {
                logger.warn(PROTOCOL_FAILED_REQUEST, "", "", url.getAddress() + " " + url.getServiceKey() + " safe guard client , should not be called ,must have a bug.");
            }
            warningCount.incrementAndGet();
        }
    }

    @Override
    public ChannelHandler getChannelHandler() {
        checkClient();
        return client.getChannelHandler();
    }

    @Override
    public boolean isConnected() {
        if (client == null) {
            // Before the request arrives, LazyConnectExchangeClient always exists in a normal connection state
            // to prevent ReconnectTask from initiating a reconnection action.
            return true;
        } else {
            return client.isConnected();
        }
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        if (client == null) {
            return InetSocketAddress.createUnresolved(NetUtils.getLocalHost(), 0);
        } else {
            return client.getLocalAddress();
        }
    }

    @Override
    public ExchangeHandler getExchangeHandler() {
        return requestHandler;
    }

    @Override
    public void send(Object message) throws RemotingException {
        checkClient();
        client.send(message);
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        checkClient();
        client.send(message, sent);
    }

    @Override
    public boolean isClosed() {
        if (client != null) {
            return client.isClosed();
        } else {
            return false;
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Override
    public void close(int timeout) {
        if (client != null) {
            client.close(timeout);
            client = null;
        }
    }

    @Override
    public void startClose() {
        if (client != null) {
            client.startClose();
        }
    }

    @Override
    public void reset(URL url) {
        checkClient();
        client.reset(url);
    }

    @Override
    @Deprecated
    public void reset(Parameters parameters) {
        reset(getUrl().addParameters(parameters.getParameters()));
    }

    @Override
    public void reconnect() throws RemotingException {
        checkClient();
        client.reconnect();
    }

    @Override
    public Object getAttribute(String key) {
        if (client == null) {
            return null;
        } else {
            return client.getAttribute(key);
        }
    }

    @Override
    public void setAttribute(String key, Object value) {
        checkClient();
        client.setAttribute(key, value);
    }

    @Override
    public void removeAttribute(String key) {
        checkClient();
        client.removeAttribute(key);
    }

    @Override
    public boolean hasAttribute(String key) {
        if (client == null) {
            return false;
        } else {
            return client.hasAttribute(key);
        }
    }

    private void checkClient() {
        try {
            initClient();
        } catch (Exception e) {
            throw new RpcException("Fail to create remoting client for service(" + url + "): " + e.getMessage(), e);
        }

        if (!isConnected() && !needReconnect) {
            throw new IllegalStateException("LazyConnectExchangeClient is not connected normally, " +
                "and send.reconnect is configured as false, the request fails quickly" + url);
        }
    }
}
