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
package org.apache.dubbo.monitor.logstash;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.monitor.MonitorService;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LogstashMonitorTest {

    private EventLoopGroup group = new NioEventLoopGroup();
    private ChannelFuture connect;
    private BlockingQueue<String> messageReceived = new LinkedBlockingQueue<>();

    @Before
    public void setup() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(group).channel(NioServerSocketChannel.class).localAddress("127.0.0.1", 6666)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LineBasedFrameDecoder(1024, true, true));
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                if (msg instanceof ByteBuf) {
                                    ByteBuf packet = (ByteBuf) msg;
                                    messageReceived.offer(packet.toString(StandardCharsets.UTF_8));
                                }
                            }
                        });
                    }
                });
        connect = bootstrap.bind().sync();
    }

    @Test
    public void testCount() throws Exception {
        LogstashMonitor monitor = new LogstashMonitor(URL.valueOf("logstash://127.0.0.1:6666"));
        URL statistics = new URL("dubbo", "10.20.153.10", 0)
                .addParameter(MonitorService.APPLICATION, "morgan")
                .addParameter(MonitorService.INTERFACE, "MemberService")
                .addParameter(MonitorService.METHOD, "findPerson")
                .addParameter(MonitorService.CONSUMER, "10.20.153.11")
                .addParameter(MonitorService.SUCCESS, 1)
                .addParameter(MonitorService.FAILURE, 0)
                .addParameter(MonitorService.ELAPSED, 3)
                .addParameter(MonitorService.MAX_ELAPSED, 3)
                .addParameter(MonitorService.CONCURRENT, 1)
                .addParameter(MonitorService.MAX_CONCURRENT, 1);
        monitor.collect(statistics);
        monitor.send();

        String received = messageReceived.poll(3, TimeUnit.SECONDS);
        assertNotNull("monitor data not received", received);
        MonitorData data = JSON.parseObject(received, MonitorData.class);

        assertEquals(data.application, "morgan");
        assertEquals(data.method, "findPerson");
        assertEquals(data.service, "MemberService");
        assertEquals(data.client, "10.20.153.11");
        assertEquals(data.server, "10.20.153.10");
        assertEquals(data.success, 1);
        assertEquals(data.failure, 0);
        assertEquals(data.elapsed, 3);
        assertEquals(data.maxElapsed, 3);
        assertEquals(data.concurrent, 1);
        assertEquals(data.maxConcurrent, 1);
        monitor.destroy();
    }

    @After
    public void teardown() {
        connect.channel().closeFuture();
        group.shutdownGracefully();
    }
}
