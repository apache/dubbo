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
package org.apache.dubbo.rpc.protocol.tri;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadlocal.NamedInternalThreadFactory;
import org.apache.dubbo.common.timer.HashedWheelTimer;
import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.timer.TimerTask;
import org.apache.dubbo.remoting.utils.UrlUtils;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public class HealthCheckHandler extends ChannelDuplexHandler implements TimerTask {

    private final String KEY_READ_TIMESTAMP = "READ_TIMESTAMP";
    private Timer checkTimer;
    private Channel channel;
    private URL url;
    private int checkIntervalSeconds = 5;

    public HealthCheckHandler(URL url) {
        this.url = url;
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
        ChannelPromise promise) throws Exception {
        super.connect(ctx, remoteAddress, localAddress, promise);
        setReadTimestamp(ctx.channel());
        this.channel = ctx.channel();
        checkTimer = new HashedWheelTimer(new NamedInternalThreadFactory("tri-health-check", true), 1, TimeUnit.SECONDS);
        checkTimer.newTimeout(this, checkIntervalSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        setReadTimestamp(ctx.channel());
        ctx.read();
    }

    private void setReadTimestamp(Channel channel) {
        channel.attr(AttributeKey.valueOf(KEY_READ_TIMESTAMP)).set(System.currentTimeMillis());
    }

    private long getReadTimestamp(Channel channel) {
        return (long)channel.attr(AttributeKey.valueOf(KEY_READ_TIMESTAMP)).get();
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        int idleTimeout = UrlUtils.getIdleTimeout(url);
        int readInterval = Math.toIntExact(System.currentTimeMillis() - getReadTimestamp(channel));
        if (readInterval > idleTimeout) {
            channel.close();
        } else {
            checkTimer.newTimeout(this, checkIntervalSeconds, TimeUnit.SECONDS);
        }
    }
}
