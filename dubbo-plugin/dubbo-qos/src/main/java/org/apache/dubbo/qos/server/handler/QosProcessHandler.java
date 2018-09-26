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
package org.apache.dubbo.qos.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class QosProcessHandler extends ByteToMessageDecoder {

    private ScheduledFuture<?> welcomeFuture;

    private String welcome;
    // true means to accept foreign IP
    private boolean acceptForeignIp;

    public static String prompt = "dubbo>";

    public QosProcessHandler(String welcome, boolean acceptForeignIp) {
        this.welcome = welcome;
        this.acceptForeignIp = acceptForeignIp;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        welcomeFuture = ctx.executor().schedule(new Runnable() {

            @Override
            public void run() {
                if (welcome != null) {
                    ctx.write(Unpooled.wrappedBuffer(welcome.getBytes()));
                    ctx.writeAndFlush(Unpooled.wrappedBuffer(prompt.getBytes()));
                }
            }

        }, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 1) {
            return;
        }

        // read one byte to guess protocol
        final int magic = in.getByte(in.readerIndex());

        ChannelPipeline p = ctx.pipeline();
        p.addLast(new LocalHostPermitHandler(acceptForeignIp));
        if (isHttp(magic)) {
            // no welcome output for http protocol
            if (welcomeFuture != null && welcomeFuture.isCancellable()) {
                welcomeFuture.cancel(false);
            }
            p.addLast(new HttpServerCodec());
            p.addLast(new HttpObjectAggregator(1048576));
            p.addLast(new HttpProcessHandler());
            p.remove(this);
        } else {
            p.addLast(new LineBasedFrameDecoder(2048));
            p.addLast(new StringDecoder(CharsetUtil.UTF_8));
            p.addLast(new StringEncoder(CharsetUtil.UTF_8));
            p.addLast(new IdleStateHandler(0, 0, 5 * 60));
            p.addLast(new TelnetProcessHandler());
            p.remove(this);
        }
    }

    // G for GET, and P for POST
    private static boolean isHttp(int magic) {
        return magic == 'G' || magic == 'P';
    }
}
