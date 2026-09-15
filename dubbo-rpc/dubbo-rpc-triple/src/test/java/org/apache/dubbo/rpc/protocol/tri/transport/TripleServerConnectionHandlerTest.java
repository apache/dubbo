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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2PingFrame;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TripleServerConnectionHandlerTest {

    private static final long AGE_MS = 1000L;
    private static final long GRACE_MS = 500L;

    /** Intercepts outbound frames written by the handler before the codec encodes them. */
    private static class FrameCapture extends ChannelOutboundHandlerAdapter {
        final List<Object> frames = new ArrayList<>();

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            frames.add(msg);
            promise.setSuccess();
        }

        Http2GoAwayFrame pollGoAway() {
            for (int i = 0; i < frames.size(); i++) {
                if (frames.get(i) instanceof Http2GoAwayFrame) {
                    return (Http2GoAwayFrame) frames.remove(i);
                }
            }
            return null;
        }

        Http2PingFrame pollPing() {
            for (int i = 0; i < frames.size(); i++) {
                if (frames.get(i) instanceof Http2PingFrame) {
                    Http2PingFrame frame = (Http2PingFrame) frames.remove(i);
                    ReferenceCountUtil.release(frame);
                    return frame;
                }
            }
            return null;
        }

        void releaseAll() {
            frames.forEach(ReferenceCountUtil::release);
            frames.clear();
        }
    }

    private static EmbeddedChannel newChannel(TripleServerConnectionHandler handler, FrameCapture capture) {
        // Http2FrameCodec must precede the handler in the pipeline (mirrors the
        // production Triple server pipeline); the capture sits between them so
        // raw frame objects can be asserted without HTTP/2 encoding.
        Http2FrameCodec codec = Http2FrameCodecBuilder.forServer().build();
        return new EmbeddedChannel(codec, capture, handler);
    }

    private static void drainChannelOutbound(EmbeddedChannel channel) {
        Object msg;
        while ((msg = channel.readOutbound()) != null) {
            ReferenceCountUtil.release(msg);
        }
    }

    @Test
    void testGoAwaySentWhenMaxConnectionAgeReached() {
        FrameCapture capture = new FrameCapture();
        EmbeddedChannel channel = newChannel(new TripleServerConnectionHandler(AGE_MS, GRACE_MS), capture);
        Assertions.assertTrue(channel.isActive());
        drainChannelOutbound(channel);

        // No GOAWAY before the -10% jitter bound
        channel.advanceTimeBy(AGE_MS - AGE_MS / 10 - 1, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        Assertions.assertNull(capture.pollGoAway(), "GOAWAY must not fire before the jitter window");

        // Advance past the +10% jitter upper bound: advisory GOAWAY must be sent
        channel.advanceTimeBy(AGE_MS / 10 * 2 + 2, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        Http2GoAwayFrame goAwayFrame = capture.pollGoAway();
        Assertions.assertNotNull(goAwayFrame, "advisory GOAWAY frame should be sent after max connection age");
        Assertions.assertEquals(Http2Error.NO_ERROR.code(), goAwayFrame.errorCode());
        Assertions.assertEquals(Integer.MAX_VALUE, goAwayFrame.extraStreamIds());
        goAwayFrame.release();

        // Connection stays open during the grace period
        channel.advanceTimeBy(GRACE_MS - 1, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        Assertions.assertTrue(channel.isActive());

        // After the grace period close is initiated: the handler's close() path
        // runs the existing graceful shutdown sequence (final GOAWAY + PING).
        // The actual socket close afterwards depends on the PING ACK (or the
        // codec's own graceful-shutdown timeout), which is pre-existing Dubbo
        // behavior and out of scope here — we assert the sequence was started.
        channel.advanceTimeBy(2, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        Http2GoAwayFrame shutdownGoAway = capture.pollGoAway();
        Assertions.assertNotNull(shutdownGoAway, "graceful shutdown GOAWAY should be sent after grace period");
        shutdownGoAway.release();
        Assertions.assertNotNull(capture.pollPing(), "graceful shutdown PING should follow the GOAWAY");

        capture.releaseAll();
        channel.finishAndReleaseAll();
    }

    @Test
    void testNoGoAwayWhenDisabledByDefault() {
        FrameCapture capture = new FrameCapture();
        EmbeddedChannel channel = newChannel(new TripleServerConnectionHandler(), capture);
        drainChannelOutbound(channel);
        // Default constructor: maxConnectionAge = -1 (disabled)
        channel.advanceTimeBy(TimeUnit.HOURS.toMillis(24), TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        Assertions.assertNull(capture.pollGoAway());
        Assertions.assertTrue(channel.isActive());
        capture.releaseAll();
        channel.finishAndReleaseAll();
    }

    @Test
    void testTasksCancelledOnChannelInactive() {
        FrameCapture capture = new FrameCapture();
        EmbeddedChannel channel = newChannel(new TripleServerConnectionHandler(AGE_MS, GRACE_MS), capture);
        drainChannelOutbound(channel);
        channel.pipeline().fireChannelInactive();
        // Age tasks must have been cancelled: nothing should fire afterwards
        channel.advanceTimeBy(AGE_MS * 2, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        Assertions.assertNull(capture.pollGoAway());
        capture.releaseAll();
        channel.finishAndReleaseAll();
    }

    @Test
    void testJitterIsWithinTenPercent() {
        // Verify across many connections that GOAWAY never fires before the
        // -10% jitter bound and always fires by the +10% bound.
        for (int i = 0; i < 50; i++) {
            FrameCapture capture = new FrameCapture();
            EmbeddedChannel channel = newChannel(new TripleServerConnectionHandler(AGE_MS, GRACE_MS), capture);
            drainChannelOutbound(channel);
            channel.advanceTimeBy(AGE_MS - AGE_MS / 10 - 1, TimeUnit.MILLISECONDS);
            channel.runScheduledPendingTasks();
            Assertions.assertNull(capture.pollGoAway(), "GOAWAY must not fire before the -10% jitter bound");
            channel.advanceTimeBy(AGE_MS / 10 * 2 + 2, TimeUnit.MILLISECONDS);
            channel.runScheduledPendingTasks();
            Http2GoAwayFrame frame = capture.pollGoAway();
            Assertions.assertNotNull(frame, "GOAWAY must fire by the +10% jitter bound");
            frame.release();
            capture.releaseAll();
            channel.finishAndReleaseAll();
        }
    }
}
