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
package com.alibaba.dubbo.remoting.transport;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.store.DataStore;
import com.alibaba.dubbo.common.utils.ExecutorUtil;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.dispatcher.ChannelHandlers;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AbstractClient
 *
 * Client 抽象类，实现了重连逻辑。
 */
public abstract class AbstractClient extends AbstractEndpoint implements Client {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClient.class);

    protected static final String CLIENT_THREAD_POOL_NAME = "DubboClientHandler";

    @Deprecated // 因为，{@link #createExecutor()} 方法，未声明
    private static final AtomicInteger CLIENT_THREAD_POOL_ID = new AtomicInteger();

    /**
     * 重连定时任务执行器
     */
    private static final ScheduledThreadPoolExecutor reconnectExecutorService = new ScheduledThreadPoolExecutor(2, new NamedThreadFactory("DubboClientReconnectTimer", true));
    /**
     * 连接锁，用于实现发起连接和断开连接互斥，避免并发。
     */
    private final Lock connectLock = new ReentrantLock();
    /**
     * 发送消息时，若断开，是否重连
     */
    private final boolean send_reconnect;
    /**
     * 重连次数
     */
    private final AtomicInteger reconnect_count = new AtomicInteger(0);
    /**
     * 重连时，是否已经打印过错误日志。
     */
    // Reconnection error log has been called before?
    private final AtomicBoolean reconnect_error_log_flag = new AtomicBoolean(false);
    /**
     * 重连 warning 的间隔.(waring多少次之后，warning一次) //for test
     */
    // reconnect warning period. Reconnect warning interval (log warning after how many times) //for test
    private final int reconnect_warning_period;
    /**
     * 关闭超时时间
     */
    private final long shutdown_timeout;
    /**
     * 线程池
     *
     * 在调用 {@link #wrapChannelHandler(URL, ChannelHandler)} 时，会调用 {@link com.alibaba.dubbo.remoting.transport.dispatcher.WrappedChannelHandler} 创建
     */
    protected volatile ExecutorService executor;
    /**
     * 重连执行任务 Future
     */
    private volatile ScheduledFuture<?> reconnectExecutorFuture = null;
    /**
     * 最后成功连接时间
     */
    // the last successed connected time
    private long lastConnectedTime = System.currentTimeMillis();

    public AbstractClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
        // 从 URL 中，获得重连相关配置项
        send_reconnect = url.getParameter(Constants.SEND_RECONNECT_KEY, false);
        shutdown_timeout = url.getParameter(Constants.SHUTDOWN_TIMEOUT_KEY, Constants.DEFAULT_SHUTDOWN_TIMEOUT);
        // The default reconnection interval is 2s, 1800 means warning interval is 1 hour.
        reconnect_warning_period = url.getParameter("reconnect.waring.period", 1800);

        // 初始化客户端
        try {
            doOpen();
        } catch (Throwable t) {
            close(); // 失败，则关闭
            throw new RemotingException(url.toInetSocketAddress(), null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
        }

        // 连接服务器
        try {
            // connect.
            connect();
            if (logger.isInfoEnabled()) {
                logger.info("Start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress() + " connect to the server " + getRemoteAddress());
            }
        } catch (RemotingException t) {
            if (url.getParameter(Constants.CHECK_KEY, true)) {
                close(); // 失败，则关闭
                throw t;
            } else {
                logger.warn("Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                        + " connect to the server " + getRemoteAddress() + " (check == false, ignore and retry later!), cause: " + t.getMessage(), t);
            }
        } catch (Throwable t) {
            close(); // 失败，则关闭
            throw new RemotingException(url.toInetSocketAddress(), null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
        }

        // 获得线程池
        executor = (ExecutorService) ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension()
                .get(Constants.CONSUMER_SIDE, Integer.toString(url.getPort()));
        ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension()
                .remove(Constants.CONSUMER_SIDE, Integer.toString(url.getPort()));
    }

    /**
     * 包装通道处理器
     *
     * @param url URL
     * @param handler 被包装的通道处理器
     * @return 包装后的通道处理器
     */
    protected static ChannelHandler wrapChannelHandler(URL url, ChannelHandler handler) {
        // 设置线程名
        url = ExecutorUtil.setThreadName(url, CLIENT_THREAD_POOL_NAME);
        // 设置使用的线程池类型
        url = url.addParameterIfAbsent(Constants.THREADPOOL_KEY, Constants.DEFAULT_CLIENT_THREADPOOL);
        // 包装通道处理器
        return ChannelHandlers.wrap(handler, url);
    }

    /**
     * 从 URL 中，获得重连频率。单位：毫秒。
     *
     * @param url URL
     * @return 0-false
     */
    private static int getReconnectParam(URL url) {
        int reconnect;
        String param = url.getParameter(Constants.RECONNECT_KEY);
        if (param == null || param.length() == 0 || "true".equalsIgnoreCase(param)) {
            reconnect = Constants.DEFAULT_RECONNECT_PERIOD; // 默认 2000 毫秒
        } else if ("false".equalsIgnoreCase(param)) {
            reconnect = 0;
        } else {
            try {
                reconnect = Integer.parseInt(param);
            } catch (Exception e) {
                throw new IllegalArgumentException("reconnect param must be nonnegative integer or false/true. input is:" + param);
            }
            if (reconnect < 0) {
                throw new IllegalArgumentException("reconnect param must be nonnegative integer or false/true. input is:" + param);
            }
        }
        return reconnect;
    }

    /**
     * init reconnect thread
     *
     * 初始化重连线程
     */
    private synchronized void initConnectStatusCheckCommand() {
        //reconnect=false to close reconnect
        // 获得获得重连频率，默认开启。
        int reconnect = getReconnectParam(getUrl());
        // 若开启重连功能，创建重连线程
        if (reconnect > 0 && (reconnectExecutorFuture == null || reconnectExecutorFuture.isCancelled())) {
            // 创建 Runnable 对象
            Runnable connectStatusCheckCommand = new Runnable() {
                public void run() {
                    try {
                        // 未连接，重连
                        if (!isConnected()) {
                            connect();
                        // 已连接，记录最后连接时间
                        } else {
                            lastConnectedTime = System.currentTimeMillis();
                        }
                    } catch (Throwable t) {
                        // 超过一定时间未连接上，才打印异常日志。并且，仅打印一次。默认，15 分钟。
                        String errorMsg = "client reconnect to " + getUrl().getAddress() + " find error . url: " + getUrl();
                        // wait registry sync provider list
                        if (System.currentTimeMillis() - lastConnectedTime > shutdown_timeout) {
                            if (!reconnect_error_log_flag.get()) {
                                reconnect_error_log_flag.set(true);
                                logger.error(errorMsg, t);
                                return;
                            }
                        }
                        // 每一定次发现未重连，才打印告警日志。默认，1800 次，1 小时。
                        if (reconnect_count.getAndIncrement() % reconnect_warning_period == 0) {
                            logger.warn(errorMsg, t);
                        }
                    }
                }
            };
            // 发起定时任务
            reconnectExecutorFuture = reconnectExecutorService.scheduleWithFixedDelay(connectStatusCheckCommand, reconnect, reconnect, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 关闭重连线程
     */
    private synchronized void destroyConnectStatusCheckCommand() {
        try {
            // 关闭重连线程
            if (reconnectExecutorFuture != null && !reconnectExecutorFuture.isDone()) {
                // 强制取消
                reconnectExecutorFuture.cancel(true);
                // 清除已经取消的任务
                reconnectExecutorService.purge();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    // 未调用
    protected ExecutorService createExecutor() {
        return Executors.newCachedThreadPool(new NamedThreadFactory(CLIENT_THREAD_POOL_NAME + CLIENT_THREAD_POOL_ID.incrementAndGet() + "-" + getUrl().getAddress(), true));
    }

    public InetSocketAddress getConnectAddress() {
        return new InetSocketAddress(NetUtils.filterLocalHost(getUrl().getHost()), getUrl().getPort());
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        Channel channel = getChannel();
        if (channel == null)
            return getUrl().toInetSocketAddress();
        return channel.getRemoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        Channel channel = getChannel();
        if (channel == null)
            return InetSocketAddress.createUnresolved(NetUtils.getLocalHost(), 0);
        return channel.getLocalAddress();
    }

    @Override
    public boolean isConnected() {
        Channel channel = getChannel();
        return channel != null && channel.isConnected();
    }

    @Override
    public Object getAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null)
            return null;
        return channel.getAttribute(key);
    }

    @Override
    public void setAttribute(String key, Object value) {
        Channel channel = getChannel();
        if (channel == null)
            return;
        channel.setAttribute(key, value);
    }

    @Override
    public void removeAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null)
            return;
        channel.removeAttribute(key);
    }

    @Override
    public boolean hasAttribute(String key) {
        Channel channel = getChannel();
        return channel != null && channel.hasAttribute(key);
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        // 未连接时，开启重连功能，则先发起连接
        if (send_reconnect && !isConnected()) {
            connect();
        }
        // 发送消息
        Channel channel = getChannel();
        //TODO Can the value returned by getChannel() be null? need improvement.
        if (channel == null || !channel.isConnected()) {
            throw new RemotingException(this, "message can not send, because channel is closed . url:" + getUrl());
        }
        channel.send(message, sent);
    }

    protected void connect() throws RemotingException {
        // 获得锁
        connectLock.lock();
        try {
            // 已连接，
            if (isConnected()) {
                return;
            }
            // 初始化重连线程
            initConnectStatusCheckCommand();
            // 执行连接
            doConnect();
            // 连接失败，抛出异常
            if (!isConnected()) {
                throw new RemotingException(this, "Failed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                        + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                        + ", cause: Connect wait timeout: " + getTimeout() + "ms.");
            // 连接成功，打印日志
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("Successed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                            + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                            + ", channel is " + this.getChannel());
                }
            }
            // 设置重连次数归零
            reconnect_count.set(0);
            // 设置未打印过错误日志
            reconnect_error_log_flag.set(false);
        } catch (RemotingException e) {
            throw e;
        } catch (Throwable e) {
            throw new RemotingException(this, "Failed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                    + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                    + ", cause: " + e.getMessage(), e);
        } finally {
            // 释放锁
            connectLock.unlock();
        }
    }

    public void disconnect() {
        // 获得锁
        connectLock.lock();
        try {
            //
            destroyConnectStatusCheckCommand();
            // 关闭连接
            try {
                Channel channel = getChannel();
                if (channel != null) {
                    channel.close();
                }
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
            // 执行关闭
            try {
                doDisConnect();
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
        } finally {
            // 释放锁
            connectLock.unlock();
        }
    }

    @Override
    public void reconnect() throws RemotingException {
        // 断开连接
        disconnect();
        // 发起连接
        connect();
    }

    @Override
    public void close() {
        // 关闭线程池
        try {
            if (executor != null) {
                ExecutorUtil.shutdownNow(executor, 100);
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        // 标记关闭
        try {
            super.close();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        // 断开连接
        try {
            disconnect();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        // 执行子类关闭
        try {
            doClose();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public void close(int timeout) {
        ExecutorUtil.gracefulShutdown(executor, timeout);
        close();
    }

    @Override
    public String toString() {
        return getClass().getName() + " [" + getLocalAddress() + " -> " + getRemoteAddress() + "]";
    }

    /**
     * Open client.
     *
     * @throws Throwable
     */
    protected abstract void doOpen() throws Throwable;

    /**
     * Close client.
     *
     * @throws Throwable
     */
    protected abstract void doClose() throws Throwable;

    /**
     * Connect to server.
     *
     * @throws Throwable
     */
    protected abstract void doConnect() throws Throwable;

    /**
     * disConnect to server.
     *
     * @throws Throwable
     */
    protected abstract void doDisConnect() throws Throwable;

    /**
     * Get the connected channel.
     *
     * @return channel
     */
    protected abstract Channel getChannel();

}
