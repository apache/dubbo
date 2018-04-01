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
package com.alibaba.dubbo.remoting.exchange.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.TimeoutException;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.exchange.ResponseCallback;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DefaultFuture.
 *
 * 默认响应 Future 实现类
 */
public class DefaultFuture implements ResponseFuture {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFuture.class);

    /**
     * 通道集合
     *
     * key：请求编号
     */
    private static final Map<Long, Channel> CHANNELS = new ConcurrentHashMap<Long, Channel>();
    /**
     * Future 集合
     *
     * key：请求编号
     */
    private static final Map<Long, DefaultFuture> FUTURES = new ConcurrentHashMap<Long, DefaultFuture>();

    static {
        Thread th = new Thread(new RemotingInvocationTimeoutScan(), "DubboResponseTimeoutScanTimer");
        th.setDaemon(true);
        th.start();
    }

    /**
     * 请求编号
     */
    // invoke id.
    private final long id;
    /**
     * 通道
     */
    private final Channel channel;
    /**
     * 请求
     */
    private final Request request;
    /**
     * 超时
     */
    private final int timeout;
    /**
     * 锁
     */
    private final Lock lock = new ReentrantLock();
    /**
     * 完成 Condition
     */
    private final Condition done = lock.newCondition();
    /**
     * 创建开始时间
     */
    private final long start = System.currentTimeMillis();
    /**
     * 发送请求时间
     */
    private volatile long sent;
    /**
     * 响应
     */
    private volatile Response response;
    /**
     * 回调
     */
    private volatile ResponseCallback callback;

    public DefaultFuture(Channel channel, Request request, int timeout) {
        this.channel = channel;
        this.request = request;
        this.id = request.getId();
        this.timeout = timeout > 0 ? timeout : channel.getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        // put into waiting map.
        FUTURES.put(id, this);
        CHANNELS.put(id, channel);
    }

    public static DefaultFuture getFuture(long id) {
        return FUTURES.get(id);
    }

    /**
     * @param channel 通道
     * @return 通道是否有未结束的请求
     */
    public static boolean hasFuture(Channel channel) {
        return CHANNELS.containsValue(channel);
    }

    /**
     * 标记已发送
     *
     * @param channel 通道
     * @param request 请求
     */
    public static void sent(Channel channel, Request request) {
        DefaultFuture future = FUTURES.get(request.getId());
        if (future != null) {
            future.doSent();
        }
    }

    /**
     * 接收响应( Response )
     *
     * @param channel 通道
     * @param response 响应
     */
    public static void received(Channel channel, Response response) {
        try {
            // 移除 FUTURES
            DefaultFuture future = FUTURES.remove(response.getId());
            // 接收结果
            if (future != null) {
                future.doReceived(response);
            } else {
                logger.warn("The timeout response finally returned at "
                        + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
                        + ", response " + response
                        + (channel == null ? "" : ", channel: " + channel.getLocalAddress()
                        + " -> " + channel.getRemoteAddress()));
            }
        // 移除 CHANNELS
        } finally {
            CHANNELS.remove(response.getId());
        }
    }

    @Override
    public Object get() throws RemotingException {
        return get(timeout);
    }

    @Override
    public Object get(int timeout) throws RemotingException {
        if (timeout <= 0) {
            timeout = Constants.DEFAULT_TIMEOUT;
        }
        // 若未完成，等待
        if (!isDone()) {
            long start = System.currentTimeMillis();
            lock.lock();
            try {
                // 等待完成或超时
                while (!isDone()) {
                    done.await(timeout, TimeUnit.MILLISECONDS);
                    if (isDone() || System.currentTimeMillis() - start > timeout) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
            // 未完成，抛出超时异常 TimeoutException
            if (!isDone()) {
                throw new TimeoutException(sent > 0, channel, getTimeoutMessage(false));
            }
        }
        // 返回响应
        return returnFromResponse();
    }

    public void cancel() {
        Response errorResult = new Response(id);
        errorResult.setErrorMessage("request future has been canceled.");
        response = errorResult;
        FUTURES.remove(id);
        CHANNELS.remove(id);
    }

    @Override
    public boolean isDone() {
        return response != null;
    }

    @Override
    public void setCallback(ResponseCallback callback) {
        // 已完成，调用回调
        if (isDone()) {
            invokeCallback(callback);
        } else {
            boolean isdone = false;
            // 获得锁
            lock.lock();
            try {
                // 未完成，设置回调
                if (!isDone()) {
                    this.callback = callback;
                } else {
                    isdone = true;
                }
            // 释放锁
            } finally {
                lock.unlock();
            }
            // 已完成，调用回调
            if (isdone) {
                invokeCallback(callback);
            }
        }
    }

    private void invokeCallback(ResponseCallback c) {
        ResponseCallback callbackCopy = c;
        if (callbackCopy == null) {
            throw new NullPointerException("callback cannot be null.");
        }
        Response res = response;
        if (res == null) {
            throw new IllegalStateException("response cannot be null. url:" + channel.getUrl());
        }

        // 正常，处理结果
        if (res.getStatus() == Response.OK) {
            try {
                callbackCopy.done(res.getResult());
            } catch (Exception e) {
                logger.error("callback invoke error .reasult:" + res.getResult() + ",url:" + channel.getUrl(), e);
            }
        // 超时，处理 TimeoutException 异常
        } else if (res.getStatus() == Response.CLIENT_TIMEOUT || res.getStatus() == Response.SERVER_TIMEOUT) {
            try {
                TimeoutException te = new TimeoutException(res.getStatus() == Response.SERVER_TIMEOUT, channel, res.getErrorMessage());
                callbackCopy.caught(te);
            } catch (Exception e) {
                logger.error("callback invoke error ,url:" + channel.getUrl(), e);
            }
        // 其他，处理 RemotingException 异常
        } else {
            try {
                RuntimeException re = new RuntimeException(res.getErrorMessage());
                callbackCopy.caught(re);
            } catch (Exception e) {
                logger.error("callback invoke error ,url:" + channel.getUrl(), e);
            }
        }
    }

    /**
     * 返回结果
     *
     * @return 结果
     * @throws RemotingException 当发生异常
     */
    private Object returnFromResponse() throws RemotingException {
        Response res = response;
        if (res == null) {
            throw new IllegalStateException("response cannot be null");
        }
        // 正常，返回结果
        if (res.getStatus() == Response.OK) {
            return res.getResult();
        }
        // 超时，抛出 TimeoutException 异常
        if (res.getStatus() == Response.CLIENT_TIMEOUT || res.getStatus() == Response.SERVER_TIMEOUT) {
            throw new TimeoutException(res.getStatus() == Response.SERVER_TIMEOUT, channel, res.getErrorMessage());
        }
        // 其他，抛出 RemotingException 异常
        throw new RemotingException(channel, res.getErrorMessage());
    }

    private long getId() {
        return id;
    }

    private Channel getChannel() {
        return channel;
    }

    private boolean isSent() {
        return sent > 0;
    }

    public Request getRequest() {
        return request;
    }

    private int getTimeout() {
        return timeout;
    }

    private long getStartTimestamp() {
        return start;
    }

    /**
     * 标记 `sent`
     */
    private void doSent() {
        sent = System.currentTimeMillis();
    }

    private void doReceived(Response res) {
        // 锁定
        lock.lock();
        try {
            // 设置结果
            response = res;
            // 通知，唤醒等待
            if (done != null) {
                done.signal();
            }
        } finally {
            // 释放锁定
            lock.unlock();
        }
        // 调用回调
        if (callback != null) {
            invokeCallback(callback);
        }
    }

    /**
     * 获得异常信息提示
     *
     * @param scan 是否 {@link RemotingInvocationTimeoutScan} 扫描触发
     * @return 信息提示
     */
    private String getTimeoutMessage(boolean scan) {
        long nowTimestamp = System.currentTimeMillis();
                // 阶段
        return (sent > 0 ? "Waiting server-side response timeout" : "Sending request timeout in client-side")
                // 触发
                + (scan ? " by scan timer" : "") + ". start time: "
                + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(start))) + ", end time: "
                + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())) + ","
                // 剩余时间
                + (sent > 0 ? " client elapsed: " + (sent - start)
                + " ms, server elapsed: " + (nowTimestamp - sent)
                : " elapsed: " + (nowTimestamp - start)) + " ms, timeout: "
                + timeout + " ms, request: " + request + ", " +
                // 连接的服务器
                "channel: " + channel.getLocalAddress() + " -> " + channel.getRemoteAddress();
    }

    /**
     * 后台扫描调用超时任务
     */
    private static class RemotingInvocationTimeoutScan implements Runnable {

        public void run() {
            while (true) {
                try {
                    for (DefaultFuture future : FUTURES.values()) {
                        // 已完成，跳过
                        if (future == null || future.isDone()) {
                            continue;
                        }
                        // 超时
                        if (System.currentTimeMillis() - future.getStartTimestamp() > future.getTimeout()) {
                            // 创建超时 Response
                            // create exception response.
                            Response timeoutResponse = new Response(future.getId());
                            // set timeout status.
                            timeoutResponse.setStatus(future.isSent() ? Response.SERVER_TIMEOUT : Response.CLIENT_TIMEOUT);
                            timeoutResponse.setErrorMessage(future.getTimeoutMessage(true));
                            // 响应结果
                            // handle response.
                            DefaultFuture.received(future.getChannel(), timeoutResponse);
                        }
                    }
                    // 30 ms
                    Thread.sleep(30);
                } catch (Throwable e) {
                    logger.error("Exception when scan the timeout invocation of remoting.", e);
                }
            }
        }
    }

}