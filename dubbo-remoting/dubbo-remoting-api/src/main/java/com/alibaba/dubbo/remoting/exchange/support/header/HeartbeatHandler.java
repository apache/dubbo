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
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.transport.AbstractChannelHandlerDelegate;

/**
 * 心跳处理器，处理心跳事件
 */
public class HeartbeatHandler extends AbstractChannelHandlerDelegate {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);

    public static String KEY_READ_TIMESTAMP = "READ_TIMESTAMP";

    public static String KEY_WRITE_TIMESTAMP = "WRITE_TIMESTAMP";

    public HeartbeatHandler(ChannelHandler handler) {
        super(handler);
    }

    @Override
    public void connected(Channel channel) throws RemotingException {
        // 设置最后的读和写时间
        setReadTimestamp(channel);
        setWriteTimestamp(channel);
        // 提交给装饰的 `handler`，继续处理
        handler.connected(channel);
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        // 清除最后的读和写时间
        clearReadTimestamp(channel);
        clearWriteTimestamp(channel);
        // 提交给装饰的 `handler`，继续处理
        handler.disconnected(channel);
    }

    @Override
    public void sent(Channel channel, Object message) throws RemotingException {
        // 设置最后的写时间
        setWriteTimestamp(channel);
        // 提交给装饰的 `handler`，继续处理
        handler.sent(channel, message);
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        // 设置最后的读时间
        setReadTimestamp(channel);
        // 如果是心跳事件请求，返回心跳事件的响应
        if (isHeartbeatRequest(message)) {
            Request req = (Request) message;
            if (req.isTwoWay()) {
                Response res = new Response(req.getId(), req.getVersion());
                res.setEvent(Response.HEARTBEAT_EVENT);
                channel.send(res);
                if (logger.isInfoEnabled()) {
                    int heartbeat = channel.getUrl().getParameter(Constants.HEARTBEAT_KEY, 0);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Received heartbeat from remote channel " + channel.getRemoteAddress()
                                + ", cause: The channel has no data-transmission exceeds a heartbeat period"
                                + (heartbeat > 0 ? ": " + heartbeat + "ms" : ""));
                    }
                }
            }
            return;
        }
        // 如果是心跳事件响应，返回
        if (isHeartbeatResponse(message)) {
            if (logger.isDebugEnabled()) {
                logger.debug(new StringBuilder(32).append("Receive heartbeat response in thread ").append(Thread.currentThread().getName()).toString());
            }
            return;
        }
        // 提交给装饰的 `handler`，继续处理
        handler.received(channel, message);
    }

    private void setReadTimestamp(Channel channel) {
        channel.setAttribute(KEY_READ_TIMESTAMP, System.currentTimeMillis());
    }

    private void setWriteTimestamp(Channel channel) {
        channel.setAttribute(KEY_WRITE_TIMESTAMP, System.currentTimeMillis());
    }

    private void clearReadTimestamp(Channel channel) {
        channel.removeAttribute(KEY_READ_TIMESTAMP);
    }

    private void clearWriteTimestamp(Channel channel) {
        channel.removeAttribute(KEY_WRITE_TIMESTAMP);
    }

    private boolean isHeartbeatRequest(Object message) {
        return message instanceof Request && ((Request) message).isHeartbeat();
    }

    private boolean isHeartbeatResponse(Object message) {
        return message instanceof Response && ((Response) message).isHeartbeat();
    }

}
