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
package org.apache.dubbo.remoting.transport;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Client;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelHandlers;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_CLIENT_THREADPOOL;
import static org.apache.dubbo.common.constants.CommonConstants.LAZY_CONNECT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CLOSE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CONNECT_PROVIDER;
import static org.apache.dubbo.config.Constants.CLIENT_THREAD_POOL_NAME;
import static org.apache.dubbo.remoting.Constants.HEARTBEAT_CHECK_TICK;
import static org.apache.dubbo.remoting.Constants.LEAST_HEARTBEAT_DURATION;
import static org.apache.dubbo.remoting.Constants.LEAST_RECONNECT_DURATION;
import static org.apache.dubbo.remoting.Constants.LEAST_RECONNECT_DURATION_KEY;
import static org.apache.dubbo.remoting.utils.UrlUtils.getIdleTimeout;

/**
 * AbstractClient
 */
public abstract class AbstractClient extends AbstractEndpoint implements Client {

    private final Lock connectLock = new ReentrantLock();

    private final boolean needReconnect;

    private final FrameworkModel frameworkModel;

    protected volatile ExecutorService executor;

    protected volatile ScheduledExecutorService connectivityExecutor;

    protected long reconnectDuration;

    public AbstractClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
        // set default needReconnect true when channel is not connected
        needReconnect = url.getParameter(Constants.SEND_RECONNECT_KEY, true);

        frameworkModel = url.getOrDefaultFrameworkModel();

        initExecutor(url);

        reconnectDuration = getReconnectDuration(url);

        try {
            doOpen();
        } catch (Throwable t) {
            close();
            throw new RemotingException(
                    url.toInetSocketAddress(),
                    null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(),
                    t);
        }

        try {
            // connect.
            connect();
            if (logger.isInfoEnabled()) {
                logger.info("Start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                        + " connect to the server " + getRemoteAddress());
            }
        } catch (RemotingException t) {
            // If lazy connect client fails to establish a connection, the client instance will still be created,
            // and the reconnection will be initiated by ReconnectTask, so there is no need to throw an exception
            if (url.getParameter(LAZY_CONNECT_KEY, false)) {
                logger.warn(
                        TRANSPORT_FAILED_CONNECT_PROVIDER,
                        "",
                        "",
                        "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                                + " connect to the server "
                                + getRemoteAddress()
                                + " (the connection request is initiated by lazy connect client, ignore and retry later!), cause: "
                                + t.getMessage(),
                        t);
                return;
            }

            if (url.getParameter(Constants.CHECK_KEY, true)) {
                close();
                throw t;
            } else {
                logger.warn(
                        TRANSPORT_FAILED_CONNECT_PROVIDER,
                        "",
                        "",
                        "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                                + " connect to the server " + getRemoteAddress()
                                + " (check == false, ignore and retry later!), cause: " + t.getMessage(),
                        t);
            }
        } catch (Throwable t) {
            close();
            throw new RemotingException(
                    url.toInetSocketAddress(),
                    null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(),
                    t);
        }
    }

    protected AbstractClient() {
        needReconnect = false;
        frameworkModel = null;
    }

    private void initExecutor(URL url) {
        ExecutorRepository executorRepository = ExecutorRepository.getInstance(url.getOrDefaultApplicationModel());

        /*
         * Consumer's executor is shared globally, provider ip doesn't need to be part of the thread name.
         *
         * Instance of url is InstanceAddressURL, so addParameter actually adds parameters into ServiceInstance,
         * which means params are shared among different services. Since client is shared among services this is currently not a problem.
         */
        url = url.addParameter(THREAD_NAME_KEY, CLIENT_THREAD_POOL_NAME)
                .addParameterIfAbsent(THREADPOOL_KEY, DEFAULT_CLIENT_THREADPOOL);
        executor = executorRepository.createExecutorIfAbsent(url);

        connectivityExecutor = frameworkModel
                .getBeanFactory()
                .getBean(FrameworkExecutorRepository.class)
                .getConnectivityScheduledExecutor();
    }

    protected static ChannelHandler wrapChannelHandler(URL url, ChannelHandler handler) {
        return ChannelHandlers.wrap(handler, url);
    }

    public InetSocketAddress getConnectAddress() {
        return new InetSocketAddress(NetUtils.filterLocalHost(getUrl().getHost()), getUrl().getPort());
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        Channel channel = getChannel();
        if (channel == null) {
            return getUrl().toInetSocketAddress();
        }
        return channel.getRemoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        Channel channel = getChannel();
        if (channel == null) {
            return InetSocketAddress.createUnresolved(NetUtils.getLocalHost(), 0);
        }
        return channel.getLocalAddress();
    }

    @Override
    public boolean isConnected() {
        Channel channel = getChannel();
        if (channel == null) {
            return false;
        }
        return channel.isConnected();
    }

    @Override
    public Object getAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null) {
            return null;
        }
        return channel.getAttribute(key);
    }

    @Override
    public void setAttribute(String key, Object value) {
        Channel channel = getChannel();
        if (channel == null) {
            return;
        }
        channel.setAttribute(key, value);
    }

    @Override
    public void removeAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null) {
            return;
        }
        channel.removeAttribute(key);
    }

    @Override
    public boolean hasAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null) {
            return false;
        }
        return channel.hasAttribute(key);
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        if (needReconnect && !isConnected()) {
            connect();
        }
        Channel channel = getChannel();
        // TODO Can the value returned by getChannel() be null? need improvement.
        if (channel == null || !channel.isConnected()) {
            throw new RemotingException(this, "message can not send, because channel is closed . url:" + getUrl());
        }
        channel.send(message, sent);
    }

    protected void connect() throws RemotingException {
        connectLock.lock();

        try {
            if (isConnected()) {
                return;
            }

            if (isClosed() || isClosing()) {
                logger.warn(
                        TRANSPORT_FAILED_CONNECT_PROVIDER,
                        "",
                        "",
                        "No need to connect to server " + getRemoteAddress() + " from "
                                + getClass().getSimpleName() + " " + NetUtils.getLocalHost() + " using dubbo version "
                                + Version.getVersion() + ", cause: client status is closed or closing.");
                return;
            }

            doConnect();

            if (!isConnected()) {
                throw new RemotingException(
                        this,
                        "Failed to connect to server " + getRemoteAddress() + " from "
                                + getClass().getSimpleName() + " "
                                + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                                + ", cause: Connect wait timeout: " + getConnectTimeout() + "ms.");

            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("Successfully connect to server " + getRemoteAddress() + " from "
                            + getClass().getSimpleName() + " "
                            + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                            + ", channel is " + this.getChannel());
                }
            }

        } catch (RemotingException e) {
            throw e;

        } catch (Throwable e) {
            throw new RemotingException(
                    this,
                    "Failed to connect to server " + getRemoteAddress() + " from "
                            + getClass().getSimpleName() + " "
                            + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                            + ", cause: " + e.getMessage(),
                    e);

        } finally {
            connectLock.unlock();
        }
    }

    public void disconnect() {
        connectLock.lock();
        try {
            try {
                Channel channel = getChannel();
                if (channel != null) {
                    channel.close();
                }
            } catch (Throwable e) {
                logger.warn(TRANSPORT_FAILED_CLOSE, "", "", e.getMessage(), e);
            }
            try {
                doDisConnect();
            } catch (Throwable e) {
                logger.warn(TRANSPORT_FAILED_CLOSE, "", "", e.getMessage(), e);
            }
        } finally {
            connectLock.unlock();
        }
    }

    private long getReconnectDuration(URL url) {
        int idleTimeout = getIdleTimeout(url);
        long heartbeatTimeoutTick = calculateLeastDuration(idleTimeout);
        return calculateReconnectDuration(url, heartbeatTimeoutTick);
    }

    private long calculateLeastDuration(int time) {
        if (time / HEARTBEAT_CHECK_TICK <= 0) {
            return LEAST_HEARTBEAT_DURATION;
        } else {
            return time / HEARTBEAT_CHECK_TICK;
        }
    }

    private long calculateReconnectDuration(URL url, long tick) {
        long leastReconnectDuration = url.getParameter(LEAST_RECONNECT_DURATION_KEY, LEAST_RECONNECT_DURATION);
        return Math.max(leastReconnectDuration, tick);
    }

    @Override
    public void reconnect() throws RemotingException {
        connectLock.lock();
        try {
            disconnect();
            connect();
        } finally {
            connectLock.unlock();
        }
    }

    @Override
    public void close() {
        if (isClosed()) {
            logger.warn(
                    TRANSPORT_FAILED_CONNECT_PROVIDER,
                    "",
                    "",
                    "No need to close connection to server " + getRemoteAddress() + " from "
                            + getClass().getSimpleName() + " " + NetUtils.getLocalHost() + " using dubbo version "
                            + Version.getVersion() + ", cause: the client status is closed.");
            return;
        }

        connectLock.lock();
        try {
            if (isClosed()) {
                logger.warn(
                        TRANSPORT_FAILED_CONNECT_PROVIDER,
                        "",
                        "",
                        "No need to close connection to server " + getRemoteAddress() + " from "
                                + getClass().getSimpleName() + " " + NetUtils.getLocalHost() + " using dubbo version "
                                + Version.getVersion() + ", cause: the client status is closed.");
                return;
            }

            try {
                super.close();
            } catch (Throwable e) {
                logger.warn(TRANSPORT_FAILED_CLOSE, "", "", e.getMessage(), e);
            }

            try {
                disconnect();
            } catch (Throwable e) {
                logger.warn(TRANSPORT_FAILED_CLOSE, "", "", e.getMessage(), e);
            }

            try {
                doClose();
            } catch (Throwable e) {
                logger.warn(TRANSPORT_FAILED_CLOSE, "", "", e.getMessage(), e);
            }

        } finally {
            connectLock.unlock();
        }
    }

    @Override
    public void close(int timeout) {
        close();
    }

    @Override
    public String toString() {
        return getClass().getName() + " [" + getLocalAddress() + " -> " + getRemoteAddress() + "]";
    }

    /**
     * Open client.
     */
    protected abstract void doOpen() throws Throwable;

    /**
     * Close client.
     */
    protected abstract void doClose() throws Throwable;

    /**
     * Connect to server.
     */
    protected abstract void doConnect() throws Throwable;

    /**
     * disConnect to server.
     */
    protected abstract void doDisConnect() throws Throwable;

    /**
     * Get the connected channel.
     */
    protected abstract Channel getChannel();
}
