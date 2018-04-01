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

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.exchange.Request;

import java.util.Collection;

/**
 * 心跳任务
 */
final class HeartBeatTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(HeartBeatTask.class);

    private ChannelProvider channelProvider;
    /**
     * 心跳间隔，单位：毫秒
     */
    private int heartbeat;
    /**
     * 心跳超时时间，单位：毫秒
     */
    private int heartbeatTimeout;

    HeartBeatTask(ChannelProvider provider, int heartbeat, int heartbeatTimeout) {
        this.channelProvider = provider;
        this.heartbeat = heartbeat;
        this.heartbeatTimeout = heartbeatTimeout;
    }

    @Override
    public void run() {
        try {
            long now = System.currentTimeMillis();
            for (Channel channel : channelProvider.getChannels()) {
                if (channel.isClosed()) {
                    continue;
                }
                try {
                    Long lastRead = (Long) channel.getAttribute(HeaderExchangeHandler.KEY_READ_TIMESTAMP);
                    Long lastWrite = (Long) channel.getAttribute(HeaderExchangeHandler.KEY_WRITE_TIMESTAMP);
                    // 最后读写的时间，任一超过心跳间隔，发送心跳
                    if ((lastRead != null && now - lastRead > heartbeat)
                            || (lastWrite != null && now - lastWrite > heartbeat)) {
                        Request req = new Request();
                        req.setVersion("2.0.0");
                        req.setTwoWay(true); // 需要响应
                        req.setEvent(Request.HEARTBEAT_EVENT);
                        channel.send(req);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Send heartbeat to remote channel " + channel.getRemoteAddress()
                                    + ", cause: The channel has no data-transmission exceeds a heartbeat period: " + heartbeat + "ms");
                        }
                    }
                    // 最后读的时间，超过心跳超时时间
                    if (lastRead != null && now - lastRead > heartbeatTimeout) {
                        logger.warn("Close channel " + channel
                                + ", because heartbeat read idle time out: " + heartbeatTimeout + "ms");
                        // 客户端侧，重新连接服务端
                        if (channel instanceof Client) {
                            try {
                                ((Client) channel).reconnect();
                            } catch (Exception e) {
                                //do nothing
                            }
                        // 服务端侧，关闭客户端连接
                        } else {
                            channel.close();
                        }
                    }
                } catch (Throwable t) {
                    logger.warn("Exception when heartbeat to remote channel " + channel.getRemoteAddress(), t);
                }
            }
        } catch (Throwable t) {
            logger.warn("Unhandled exception when heartbeat, cause: " + t.getMessage(), t);
        }
    }

    interface ChannelProvider {
        Collection<Channel> getChannels();
    }

}

