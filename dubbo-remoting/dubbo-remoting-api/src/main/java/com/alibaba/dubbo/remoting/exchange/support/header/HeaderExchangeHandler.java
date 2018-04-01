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
package com.alibaba.dubbo.remoting.exchange.support.header;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.ExecutionException;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.exchange.support.DefaultFuture;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerDelegate;

import java.net.InetSocketAddress;

/**
 * ExchangeReceiver
 *
 * 基于消息头部( Header )的信息交换处理器实现类
 */
public class HeaderExchangeHandler implements ChannelHandlerDelegate {

    protected static final Logger logger = LoggerFactory.getLogger(HeaderExchangeHandler.class);

    public static String KEY_READ_TIMESTAMP = HeartbeatHandler.KEY_READ_TIMESTAMP;

    public static String KEY_WRITE_TIMESTAMP = HeartbeatHandler.KEY_WRITE_TIMESTAMP;

    private final ExchangeHandler handler;

    public HeaderExchangeHandler(ExchangeHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.handler = handler;
    }

    /**
     * 处理响应
     *
     * @param channel 通道
     * @param response 响应
     */
    static void handleResponse(Channel channel, Response response) {
        if (response != null && !response.isHeartbeat()) {
            DefaultFuture.received(channel, response);
        }
    }

    private static boolean isClientSide(Channel channel) {
        InetSocketAddress address = channel.getRemoteAddress();
        URL url = channel.getUrl();
        return url.getPort() == address.getPort() &&
                NetUtils.filterLocalHost(url.getIp())
                        .equals(NetUtils.filterLocalHost(address.getAddress().getHostAddress()));
    }

    /**
     * 处理事件请求
     *
     * @param channel 通道
     * @param req 请求
     */
    void handlerEvent(Channel channel, Request req) {
        // 客户端接收到 READONLY_EVENT 事件请求，进行记录到通道。后续，不再向该服务器，发送新的请求。
        if (req.getData() != null && req.getData().equals(Request.READONLY_EVENT)) {
            channel.setAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY, Boolean.TRUE);
        }
    }

    /**
     * 处理普通请求( Request)
     *
     * @param channel 通道
     * @param req 请求
     * @return 响应( Response)
     */
    Response handleRequest(ExchangeChannel channel, Request req) {
        Response res = new Response(req.getId(), req.getVersion());
        // 请求无法解析，返回 BAD_REQUEST 响应
        if (req.isBroken()) {
            Object data = req.getData();
            String msg; // 请求数据，转成 msg
            if (data == null) {
                msg = null;
            } else if (data instanceof Throwable) {
                msg = StringUtils.toString((Throwable) data);
            } else {
                msg = data.toString();
            }
            res.setErrorMessage("Fail to decode request due to: " + msg);
            res.setStatus(Response.BAD_REQUEST);
            return res;
        }
        // 使用 ExchangeHandler 处理，并返回响应
        // find handler by message class.
        Object msg = req.getData();
        try {
            // handle data.
            Object result = handler.reply(channel, msg);
            res.setStatus(Response.OK);
            res.setResult(result);
        } catch (Throwable e) {
            res.setStatus(Response.SERVICE_ERROR);
            res.setErrorMessage(StringUtils.toString(e));
        }
        return res;
    }

    @Override
    public void connected(Channel channel) throws RemotingException {
        // 设置最后的读和写时间
        channel.setAttribute(KEY_READ_TIMESTAMP, System.currentTimeMillis());
        channel.setAttribute(KEY_WRITE_TIMESTAMP, System.currentTimeMillis());
        // 创建 ExchangeChannel 对象
        ExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
        try {
            // 提交给装饰的 `handler`，继续处理
            handler.connected(exchangeChannel);
        } finally {
            // 移除 ExchangeChannel 对象，若已断开
            HeaderExchangeChannel.removeChannelIfDisconnected(channel);
        }
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        // 设置最后的读和写时间
        channel.setAttribute(KEY_READ_TIMESTAMP, System.currentTimeMillis());
        channel.setAttribute(KEY_WRITE_TIMESTAMP, System.currentTimeMillis());
        // 创建 ExchangeChannel 对象
        ExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
        try {
            // 提交给装饰的 `handler`，继续处理
            handler.disconnected(exchangeChannel);
        } finally {
            // 移除 ExchangeChannel 对象，若已断开
            HeaderExchangeChannel.removeChannelIfDisconnected(channel);
        }
    }

    @Override
    public void sent(Channel channel, Object message) throws RemotingException {
        Throwable exception = null;
        try {
            // 设置最后的写时间
            channel.setAttribute(KEY_WRITE_TIMESTAMP, System.currentTimeMillis());
            // 创建 ExchangeChannel 对象
            ExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
            try {
                // 提交给装饰的 `handler`，继续处理
                handler.sent(exchangeChannel, message);
            } finally {
                // 移除 ExchangeChannel 对象，若已断开
                HeaderExchangeChannel.removeChannelIfDisconnected(channel);
            }
        } catch (Throwable t) {
            exception = t; // 记录异常，等下面在抛出。其实，这里可以写成 finally 的方式。
        }
        // 若是请求，标记已发送
        if (message instanceof Request) {
            Request request = (Request) message;
            DefaultFuture.sent(channel, request);
        }
        // 若发生异常，抛出异常
        if (exception != null) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else if (exception instanceof RemotingException) {
                throw (RemotingException) exception;
            } else {
                throw new RemotingException(channel.getLocalAddress(), channel.getRemoteAddress(),
                        exception.getMessage(), exception);
            }
        }
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        // 设置最后的读时间
        channel.setAttribute(KEY_READ_TIMESTAMP, System.currentTimeMillis());
        // 创建 ExchangeChannel 对象
        ExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
        try {
            // 处理请求( Request )
            if (message instanceof Request) {
                // handle request.
                Request request = (Request) message;
                // 处理事件请求
                if (request.isEvent()) {
                    handlerEvent(channel, request);
                } else {
                    // 处理普通请求
                    if (request.isTwoWay()) {
                        Response response = handleRequest(exchangeChannel, request);
                        channel.send(response);
                    // 提交给装饰的 `handler`，继续处理
                    } else {
                        handler.received(exchangeChannel, request.getData());
                    }
                }
            // 处理响应( Response )
            } else if (message instanceof Response) {
                handleResponse(channel, (Response) message);
            // 处理 String
            } else if (message instanceof String) {
                // 客户端侧，不支持 String
                if (isClientSide(channel)) {
                    Exception e = new Exception("Dubbo client can not supported string message: " + message + " in channel: " + channel + ", url: " + channel.getUrl());
                    logger.error(e.getMessage(), e);
                // 服务端侧，目前是 telnet 命令
                } else {
                    String echo = handler.telnet(channel, (String) message);
                    if (echo != null && echo.length() > 0) {
                        channel.send(echo);
                    }
                }
                // 提交给装饰的 `handler`，继续处理
            } else {
                handler.received(exchangeChannel, message);
            }
        } finally {
            // 移除 ExchangeChannel 对象，若已断开
            HeaderExchangeChannel.removeChannelIfDisconnected(channel);
        }
    }

    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {
        // 当发生 ExecutionException 异常，返回异常响应( Response )
        if (exception instanceof ExecutionException) {
            ExecutionException e = (ExecutionException) exception;
            Object msg = e.getRequest();
            if (msg instanceof Request) {
                Request req = (Request) msg;
                if (req.isTwoWay() && !req.isHeartbeat()) { // 需要响应，并且非心跳时间
                    Response res = new Response(req.getId(), req.getVersion());
                    res.setStatus(Response.SERVER_ERROR);
                    res.setErrorMessage(StringUtils.toString(e));
                    channel.send(res);
                    return;
                }
            }
        }
        // 创建 ExchangeChannel 对象
        ExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
        try {
            // 提交给装饰的 `handler`，继续处理
            handler.caught(exchangeChannel, exception);
        } finally {
            // 移除 ExchangeChannel 对象，若已断开
            HeaderExchangeChannel.removeChannelIfDisconnected(channel);
        }
    }

    @Override
    public ChannelHandler getHandler() {
        if (handler instanceof ChannelHandlerDelegate) {
            return ((ChannelHandlerDelegate) handler).getHandler();
        } else {
            return handler;
        }
    }

}