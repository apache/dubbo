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
package org.apache.dubbo.remoting.http12.netty4;

import org.apache.dubbo.config.nested.TripleConfig;
import org.apache.dubbo.remoting.http12.command.HttpChannelQueueCommand;
import org.apache.dubbo.remoting.http12.netty4.h2.NettyH2StreamChannel;

import java.util.ArrayList;
import java.util.List;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies connection-level batch flushing with real HTTP/2 child channels.
 *
 * @date 2026-11-10
 */
class HttpWriteQueueHandlerTest {

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void flushesEarlierStreamWhenLastStreamIsClosed(boolean closeLastStream, boolean consolidate) throws Exception {
        List<Channel> streams = new ArrayList<>();
        FlushCounter flushCounter = new FlushCounter();
        HttpWriteQueueHandler queueHandler = new HttpWriteQueueHandler();
        EmbeddedChannel server = new EmbeddedChannel(
                flushCounter,
                queueHandler,
                Http2FrameCodecBuilder.forServer().build(),
                new Http2MultiplexHandler(discardFrames(streams)));
        if (consolidate) {
            server.pipeline()
                    .addAfter(
                            server.pipeline().context(queueHandler).name(),
                            "consolidation",
                            new FlushConsolidationHandler(64, true));
        }
        EmbeddedChannel client = new EmbeddedChannel(
                Http2FrameCodecBuilder.forClient().build(), new Http2MultiplexHandler(discardFrames(null)));
        try {
            pump(client, server);
            for (int i = 0; i < 2; i++) {
                Http2StreamChannel stream = new Http2StreamChannelBootstrap(client)
                        .handler(discardFrames(null))
                        .open()
                        .sync()
                        .getNow();
                stream.writeAndFlush(new DefaultHttp2HeadersFrame(
                        new DefaultHttp2Headers()
                                .method("POST")
                                .scheme("http")
                                .authority("localhost")
                                .path("/test"),
                        false));
                pump(client, server);
            }
            Channel live = streams.get(0);
            Channel last = streams.get(1);
            for (Channel stream : streams) {
                stream.writeAndFlush(new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().status("200"), false));
            }
            pump(client, server);
            if (closeLastStream) {
                last.close().sync();
                pump(client, server);
                assertFalse(last.isOpen());
            }
            int beforeFlush = flushCounter.count;
            HttpChannelQueueCommand liveWrite = trailers(live);
            HttpChannelQueueCommand lastWrite = trailers(last);
            queueHandler.getWriteQueue().enqueue(liveWrite);
            queueHandler.getWriteQueue().enqueue(lastWrite);
            server.runPendingTasks();

            // Observe before pumping either peer: no explicit parent flush can rescue the batch.
            Object response = server.readOutbound();
            try {
                assertNotNull(response, "the earlier live stream must reach the connection outbound buffer");
                assertTrue(liveWrite.isDone(), "the earlier live write must complete at the batch boundary");
                assertFalse(liveWrite.isCompletedExceptionally());
                assertTrue(lastWrite.isDone());
                assertEquals(closeLastStream, lastWrite.isCompletedExceptionally());
                assertEquals(1, flushCounter.count - beforeFlush, "one connection flush per batch");
            } finally {
                ReferenceCountUtil.release(response);
            }
        } finally {
            client.finishAndReleaseAll();
            server.finishAndReleaseAll();
        }
    }

    private static HttpChannelQueueCommand trailers(Channel stream) {
        HttpChannelQueueCommand command = new HttpChannelQueueCommand() {
            @Override
            public void run() {
                stream.write(new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().set("grpc-status", "0"), true))
                        .addListener(future -> {
                            if (future.isSuccess()) {
                                complete(null);
                            } else {
                                completeExceptionally(future.cause());
                            }
                        });
            }
        };
        NettyH2StreamChannel channel = new NettyH2StreamChannel(
                (Http2StreamChannel) stream,
                new TripleConfig(),
                stream.parent().pipeline().get(Http2FrameCodec.class).connection());
        command.setHttpChannel(() -> channel);
        return command;
    }

    private static ChannelInitializer<Channel> discardFrames(List<Channel> streams) {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) {
                if (streams != null) {
                    streams.add(channel);
                }
                channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object message) {
                        ReferenceCountUtil.release(message);
                    }
                });
            }
        };
    }

    private static void pump(EmbeddedChannel client, EmbeddedChannel server) {
        for (int i = 0; i < 20; i++) {
            client.runPendingTasks();
            server.runPendingTasks();
            boolean moved = false;
            Object message;
            while ((message = client.readOutbound()) != null) {
                server.writeInbound(message);
                moved = true;
            }
            while ((message = server.readOutbound()) != null) {
                client.writeInbound(message);
                moved = true;
            }
            if (!moved) {
                return;
            }
        }
        fail("HTTP/2 handshake failed to settle");
    }

    private static class FlushCounter extends ChannelDuplexHandler {
        private int count;

        @Override
        public void flush(ChannelHandlerContext ctx) {
            count++;
            ctx.flush();
        }
    }
}
