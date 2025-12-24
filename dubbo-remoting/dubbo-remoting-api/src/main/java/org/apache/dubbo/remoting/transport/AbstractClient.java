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

public abstract class AbstractClient extends AbstractEndpoint implements Client {

    private Lock connectLock;

    private final boolean needReconnect;

    private final FrameworkModel frameworkModel;

    protected volatile ExecutorService executor;

    protected volatile ScheduledExecutorService connectivityExecutor;

    protected long reconnectDuration;

    public AbstractClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);

        connectLock = new ReentrantLock();
        needReconnect = url.getParameter(Constants.SEND_RECONNECT_KEY, true);
        frameworkModel = url.getOrDefaultFrameworkModel();

        initExecutor(url);
        reconnectDuration = getReconnectDuration(url);

        try {
            doOpen();
        } catch (Throwable t) {
            close();
            throw new RemotingException(url.toInetSocketAddress(), null,
                    "Failed to start client, cause: " + t.getMessage(), t);
        }

        try {
            connect();
        } catch (Throwable t) {
            if (url.getParameter(LAZY_CONNECT_KEY, false)) {
                return;
            }
            close();
            throw t;
        }
    }

    protected AbstractClient() {
        needReconnect = false;
        frameworkModel = null;
    }

    private void initExecutor(URL url) {
        ExecutorRepository executorRepository =
                ExecutorRepository.getInstance(url.getOrDefaultApplicationModel());

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

    @Override
    public boolean isConnected() {
        Channel channel = getChannel();
        return channel != null && channel.isConnected();
    }

    @Override
    public Object getAttribute(String key) {
        Channel channel = getChannel();
        return channel == null ? null : channel.getAttribute(key);
    }

    @Override
    public void setAttribute(String key, Object value) {
        Channel channel = getChannel();
        if (channel != null) {
            channel.setAttribute(key, value);
        }
    }

    @Override
    public void removeAttribute(String key) {
        Channel channel = getChannel();
        if (channel != null) {
            channel.removeAttribute(key);
        }
    }

    @Override
    public boolean hasAttribute(String key) {
        Channel channel = getChannel();
        return channel != null && channel.hasAttribute(key);
    }

    // ✅ FIXED SEND METHOD
    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        if (needReconnect && !isConnected() && !isClosed()) {
            connect();
        }
        Channel channel = getChannel();
        if (channel == null || !channel.isConnected()) {
            throw new RemotingException(this,
                    "message can not send, because channel is closed . url:" + getUrl());
        }
        channel.send(message, sent);
    }

    protected void connect() throws RemotingException {
        connectLock.lock();
        try {
            if (isConnected() || isClosed()) {
                return;
            }
            doConnect();
        } finally {
            connectLock.unlock();
        }
    }

    public void disconnect() {
        connectLock.lock();
        try {
            Channel channel = getChannel();
            if (channel != null) {
                channel.close();
            }
            doDisConnect();
        } catch (Throwable ignored) {
        } finally {
            connectLock.unlock();
        }
    }

    private long getReconnectDuration(URL url) {
        int idleTimeout = getIdleTimeout(url);
        long tick = Math.max(LEAST_HEARTBEAT_DURATION, idleTimeout / HEARTBEAT_CHECK_TICK);
        return Math.max(
                url.getParameter(LEAST_RECONNECT_DURATION_KEY, LEAST_RECONNECT_DURATION),
                tick);
    }

    @Override
    public void reconnect() throws RemotingException {
        disconnect();
        connect();
    }

    @Override
    public void close() {
        if (isClosed()) {
            return;
        }
        connectLock.lock();
        try {
            super.close();
            disconnect();
            doClose();
        } catch (Throwable ignored) {
        } finally {
            connectLock.unlock();
        }
    }

    protected abstract void doOpen() throws Throwable;

    protected abstract void doClose() throws Throwable;

    protected abstract void doConnect() throws Throwable;

    protected abstract void doDisConnect() throws Throwable;

    protected abstract Channel getChannel();
}
