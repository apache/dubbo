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

package org.apache.dubbo.remoting.api;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;

import java.util.concurrent.TimeUnit;

public class ConnectionListener implements ChannelFutureListener {
    private static final Logger log = LoggerFactory.getLogger(ConnectionListener.class);

    private final Connection connection;

    public ConnectionListener(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void operationComplete(ChannelFuture channelFuture) {
        if (!channelFuture.isSuccess()) {
            if (log.isInfoEnabled()) {
                log.info(String.format("Connection %s is reconnecting, attempt=%d", connection, 0));
            }
            final EventLoop loop = channelFuture.channel().eventLoop();
            loop.schedule((Runnable) connection::connect, 1L, TimeUnit.SECONDS);
        }
    }
}