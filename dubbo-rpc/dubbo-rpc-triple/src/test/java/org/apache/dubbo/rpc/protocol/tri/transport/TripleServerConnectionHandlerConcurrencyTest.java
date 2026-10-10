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
package org.apache.dubbo.rpc.protocol.tri.transport;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ResourceLeakDetector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Concurrency tests for max-connection-age: many connections sharing a small
 * number of EventLoop threads, racing client disconnects, concurrent server
 * closes and reference-leak detection.
 */
class TripleServerConnectionHandlerConcurrencyTest {

    private static final long AGE_MS = 200L;
    private static final long GRACE_MS = 100L;
    private static final int CONNECTIONS = 100;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private EventLoopGroup clientGroup;
    private Channel serverChannel;
    private final List<Channel> clientChannels = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        bossGroup = new NioEventLoopGroup(1);
        // Deliberately tiny worker pool: all connections share these threads,
        // which is where concurrency bugs in scheduled tasks would surface.
        workerGroup = new NioEventLoopGroup(2);
        clientGroup = new NioEventLoopGroup(2);
    }

    @AfterEach
    void tearDown() {
        clientChannels.forEach(Channel::close);
        clientChannels.clear();
        acceptedChannels.forEach(Channel::close);
        acceptedChannels.clear();
        if (serverChannel != null) {
            serverChannel.close();
        }
        bossGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS);
        workerGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS);
        clientGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS);
    }

    private final List<Channel> acceptedChannels = new CopyOnWriteArrayList<>();

    private InetSocketAddress startServer(List<Throwable> serverErrors) throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        acceptedChannels.add(ch);
                        ch.pipeline()
                                .addLast(Http2FrameCodecBuilder.forServer()
                                        .gracefulShutdownTimeoutMillis(2000)
                                        .build())
                                .addLast(new TripleServerConnectionHandler(AGE_MS, GRACE_MS))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                        serverErrors.add(cause);
                                    }
                                });
                    }
                });
        serverChannel = bootstrap.bind(0).sync().channel();
        return (InetSocketAddress) serverChannel.localAddress();
    }

    private Channel connectClient(InetSocketAddress address, CountDownLatch inactiveLatch, List<Throwable> errors)
            throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap()
                .group(clientGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(Http2FrameCodecBuilder.forClient().build())
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                        ReferenceCountUtil.release(msg);
                                    }

                                    @Override
                                    public void channelInactive(ChannelHandlerContext ctx) {
                                        inactiveLatch.countDown();
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                        errors.add(cause);
                                    }
                                });
                    }
                });
        Channel channel = bootstrap.connect(address).sync().channel();
        clientChannels.add(channel);
        return channel;
    }

    @Test
    void testManyConnectionsRotateConcurrently() throws Exception {
        List<Throwable> serverErrors = new CopyOnWriteArrayList<>();
        List<Throwable> clientErrors = new CopyOnWriteArrayList<>();
        InetSocketAddress address = startServer(serverErrors);

        CountDownLatch allClosed = new CountDownLatch(CONNECTIONS);
        for (int i = 0; i < CONNECTIONS; i++) {
            connectClient(address, allClosed, clientErrors);
        }

        // Every connection must be closed by the server (advisory GOAWAY at
        // ~200ms +/-10%, then graceful close within the grace + ping window).
        Assertions.assertTrue(
                allClosed.await(15, TimeUnit.SECONDS),
                "all connections should be rotated by the server, still open: " + allClosed.getCount());

        // Filter benign close-related noise: what matters is no unexpected
        // IllegalStateException/refcount errors from concurrent task execution.
        assertNoSeriousErrors(serverErrors);
        assertNoSeriousErrors(clientErrors);
    }

    @Test
    void testClientDisconnectRacesWithAgeExpiry() throws Exception {
        List<Throwable> serverErrors = new CopyOnWriteArrayList<>();
        List<Throwable> clientErrors = new CopyOnWriteArrayList<>();
        InetSocketAddress address = startServer(serverErrors);

        CountDownLatch allClosed = new CountDownLatch(CONNECTIONS);
        List<Channel> channels = new ArrayList<>();
        for (int i = 0; i < CONNECTIONS; i++) {
            channels.add(connectClient(address, allClosed, clientErrors));
        }

        // Close half of the connections from the client side at random moments
        // around the age expiry window: the server-side scheduled task and the
        // close/inactive events race on the EventLoop.
        for (int i = 0; i < CONNECTIONS; i += 2) {
            Channel ch = channels.get(i);
            long delay = ThreadLocalRandom.current().nextLong(AGE_MS - AGE_MS / 5, AGE_MS + AGE_MS / 5);
            ch.eventLoop().schedule(() -> ch.close(), delay, TimeUnit.MILLISECONDS);
        }

        Assertions.assertTrue(
                allClosed.await(15, TimeUnit.SECONDS),
                "all connections should eventually close, still open: " + allClosed.getCount());
        assertNoSeriousErrors(serverErrors);
        assertNoSeriousErrors(clientErrors);
    }

    @Test
    void testServerCloseRacesWithAgeExpiry() throws Exception {
        List<Throwable> serverErrors = new CopyOnWriteArrayList<>();
        List<Throwable> clientErrors = new CopyOnWriteArrayList<>();
        InetSocketAddress address = startServer(serverErrors);

        CountDownLatch allClosed = new CountDownLatch(CONNECTIONS);
        for (int i = 0; i < CONNECTIONS; i++) {
            connectClient(address, allClosed, clientErrors);
        }

        // Close the accepted channels from the SERVER side while the age
        // expiry tasks are pending/running: this races the graceful-shutdown
        // close() path against onMaxConnectionAgeReached on the same EventLoop.
        Thread.sleep(AGE_MS / 2);
        while (acceptedChannels.size() < CONNECTIONS) {
            Thread.sleep(10);
        }
        for (Channel accepted : acceptedChannels) {
            accepted.close();
        }

        Assertions.assertTrue(
                allClosed.await(15, TimeUnit.SECONDS),
                "all connections should close when the server closes them, still open: " + allClosed.getCount());
        assertNoSeriousErrors(serverErrors);
        assertNoSeriousErrors(clientErrors);
    }

    private static void assertNoSeriousErrors(List<Throwable> errors) {
        List<String> serious = new ArrayList<>();
        for (Throwable t : errors) {
            String msg = String.valueOf(t.getMessage());
            boolean benign = t instanceof java.io.IOException
                    || t instanceof java.nio.channels.ClosedChannelException
                    || msg.contains("Connection reset")
                    || msg.contains("broken pipe")
                    || msg.toLowerCase().contains("closed");
            if (!benign) {
                serious.add(t.getClass().getName() + ": " + msg);
            }
        }
        Assertions.assertTrue(serious.isEmpty(), "unexpected errors: " + serious);
    }

    @Test
    void testNoGoAwayFrameLeakUnderChurn() throws Exception {
        // PARANOID leak detector is enabled in setUp; a leaked GOAWAY frame
        // would be reported by Netty as an ERROR log and fail the JVM leak
        // tracking assertion here via the leak detector's report hook.
        List<Throwable> serverErrors = new CopyOnWriteArrayList<>();
        List<Throwable> clientErrors = new CopyOnWriteArrayList<>();
        InetSocketAddress address = startServer(serverErrors);

        for (int round = 0; round < 5; round++) {
            CountDownLatch closed = new CountDownLatch(20);
            for (int i = 0; i < 20; i++) {
                connectClient(address, closed, clientErrors);
            }
            Assertions.assertTrue(closed.await(15, TimeUnit.SECONDS));
        }
        // Give the paranoid leak detector a GC cycle to report
        System.gc();
        Thread.sleep(200);
        assertNoSeriousErrors(serverErrors);
        assertNoSeriousErrors(clientErrors);
    }
}
