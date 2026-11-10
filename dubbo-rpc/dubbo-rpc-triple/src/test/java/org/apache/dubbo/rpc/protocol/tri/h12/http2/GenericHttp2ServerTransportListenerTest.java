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
package org.apache.dubbo.rpc.protocol.tri.h12.http2;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.serial.SerializingExecutor;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessageFrame;
import org.apache.dubbo.remoting.http12.h2.Http2ServerChannelObserver;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.h12.HttpMessageListener;
import org.apache.dubbo.rpc.protocol.tri.h12.grpc.GrpcStreamingDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests ownership of DATA bodies across the server executor boundary.
 *
 * @date 2026-11-10
 */
class GenericHttp2ServerTransportListenerTest {
    private final FrameworkModel framework = new FrameworkModel();
    private TestListener listener;
    private CountingInput input;

    @AfterEach
    void tearDown() throws IOException {
        if (listener != null) {
            listener.close();
        }
        if (input != null && input.buffer.refCnt() != 0) {
            input.close();
        }
        framework.destroy();
    }

    @Test
    void releasesRejectedDataWithoutWaitingForClose() throws Exception {
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1));
        try {
            initialize(Runnable::run);
            pool.execute(() -> {
                running.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(running.await(5, TimeUnit.SECONDS));
            pool.execute(() -> {});
            listener.dataExecutor = new SerializingExecutor(pool);
            input = new CountingInput();
            try {
                listener.onData(new Http2InputMessageFrame(input, false));
            } catch (RejectedExecutionException expected) {
                // Rejection propagation is independent of body ownership.
            }
            assertReleasedOnce();
            listener.close();
            assertReleasedOnce();
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void releasesSilentlyDiscardedDataOnClose() throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1));
        initialize(Runnable::run);
        listener.dataExecutor = new SerializingExecutor(pool);
        pool.shutdown();
        input = new CountingInput();
        listener.onData(new Http2InputMessageFrame(input, false));
        listener.close();
        assertReleasedOnce();
    }

    @Test
    void releasesSilentlyDiscardedDataOnRemoteCancel() throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1));
        initialize(Runnable::run);
        listener.dataExecutor = new SerializingExecutor(pool);
        pool.shutdown();
        input = new CountingInput();
        listener.onData(new Http2InputMessageFrame(input, false));
        listener.cancelByRemote(8);
        assertReleasedOnce();
        listener.close();
        assertReleasedOnce();
    }

    @Test
    void preservesAcceptedPartialDataUntilStreamClose() {
        initialize(Runnable::run);
        input = new CountingInput();
        listener.onData(new Http2InputMessageFrame(input, false));
        assertEquals(1, input.buffer.refCnt());
        assertEquals(0, input.closes);
        listener.close();
        assertReleasedOnce();
        listener.close();
        assertReleasedOnce();
    }

    @Test
    void queuedDataIsReleasedOnCloseBeforeExecution() {
        Queue<Runnable> queue = new ArrayDeque<>();
        initialize(queue::add);
        queue.remove().run();
        input = new CountingInput();
        listener.onData(new Http2InputMessageFrame(input, false));
        assertEquals(1, input.buffer.refCnt());
        assertEquals(0, input.closes);
        listener.close();
        assertReleasedOnce();
        queue.remove().run();
        assertReleasedOnce();
    }

    @Test
    void decoderConsumesAcceptedDataExactlyOnce() {
        initialize(Runnable::run);
        input = new CountingInput(Unpooled.buffer().writeByte(0).writeInt(1).writeByte(42));
        listener.onData(new Http2InputMessageFrame(input, false));
        assertReleasedOnce();
        assertArrayEquals(new byte[] {42}, listener.decoded);
        listener.close();
        assertReleasedOnce();
    }

    @Test
    void dataAfterCloseIsReleasedWithoutEnteringDecoder() {
        initialize(Runnable::run);
        listener.close();
        input = new CountingInput();
        listener.onData(new Http2InputMessageFrame(input, false));
        assertReleasedOnce();
    }

    @Test
    void closeDuringDataCallbackDefersPendingCleanupUntilCallbackReturns() throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1));
        CountDownLatch resume = new CountDownLatch(1);
        try {
            initialize(Runnable::run);
            listener.dataExecutor = new SerializingExecutor(pool);
            listener.dataPrepared = new CountDownLatch(1);
            listener.resumeData = resume;
            input = new CountingInput();
            listener.onData(new Http2InputMessageFrame(input, false));
            assertTrue(listener.dataPrepared.await(5, TimeUnit.SECONDS));
            listener.close();
            // The callback has started but has not handed the input to the decoder yet.
            assertEquals(1, input.buffer.refCnt());
            assertEquals(0, input.closes);
            resume.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
            assertReleasedOnce();
        } finally {
            resume.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void remoteCancelDefersBufferedInputsWhileAnotherDataCallbackReadsThem() throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1));
        CountDownLatch resume = new CountDownLatch(1);
        CountingInput next =
                new CountingInput(Unpooled.buffer().writeShort(0).writeByte(1).writeByte(42));
        try {
            initialize(Runnable::run);
            input = new CountingInput();
            input.readStarted = new CountDownLatch(1);
            input.resumeRead = resume;
            listener.onData(new Http2InputMessageFrame(input, false));
            assertEquals(1, input.buffer.refCnt());
            listener.dataExecutor = new SerializingExecutor(pool);
            listener.onData(new Http2InputMessageFrame(next, false));
            assertTrue(input.readStarted.await(5, TimeUnit.SECONDS));
            listener.cancelByRemote(8);
            // The second callback is reading the first callback's buffered input.
            assertEquals(1, input.buffer.refCnt());
            assertEquals(0, input.closes);
            assertEquals(1, next.buffer.refCnt());
            resume.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
            assertReleasedOnce();
            assertEquals(0, next.buffer.refCnt());
            assertEquals(1, next.closes);
            assertArrayEquals(new byte[] {42}, listener.decoded);
        } finally {
            resume.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
            if (next.buffer.refCnt() != 0) {
                next.close();
            }
        }
    }

    @Test
    void remoteCancelLeavesAcceptedPartialInputToDecoder() {
        initialize(Runnable::run);
        input = new CountingInput();
        listener.onData(new Http2InputMessageFrame(input, false));
        listener.cancelByRemote(8);
        assertEquals(1, input.buffer.refCnt());
        assertEquals(0, input.closes);
        listener.close();
        assertReleasedOnce();
    }

    @Test
    void closeAfterHandoffDefersInputToClosedDecoder() throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1));
        CountDownLatch resume = new CountDownLatch(1);
        try {
            initialize(Runnable::run);
            listener.dataExecutor = new SerializingExecutor(pool);
            listener.deliveryStarted = new CountDownLatch(1);
            listener.resumeDelivery = resume;
            input = new CountingInput();
            listener.onData(new Http2InputMessageFrame(input, false));
            assertTrue(listener.deliveryStarted.await(5, TimeUnit.SECONDS));
            listener.close();
            assertEquals(1, input.buffer.refCnt());
            assertEquals(0, input.closes);
            resume.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
            assertReleasedOnce();
        } finally {
            resume.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void preservesFrameMetadataAndOriginalMessageClose() {
        initialize(Runnable::run);
        input = new CountingInput();
        AtomicInteger messageCloses = new AtomicInteger();
        listener.onData(new Http2InputMessage() {
            @Override
            public InputStream getBody() {
                return input;
            }

            @Override
            public long id() {
                return 37;
            }

            @Override
            public String name() {
                return "DATA";
            }

            @Override
            public boolean isEndStream() {
                return true;
            }

            @Override
            public void close() throws IOException {
                messageCloses.incrementAndGet();
                input.close();
            }
        });
        assertEquals(37, listener.frameId);
        assertTrue(listener.endStream);
        listener.close();
        assertReleasedOnce();
        assertEquals(1, messageCloses.get());
    }

    @Test
    void deliversWebSocketFinalFragmentWithoutClosingStream() {
        listener = new WebSocketTestListener(framework);
        listener.onMetadata(mock(Http2Header.class));
        input = new FragmentInput(true, new byte[] {42});
        listener.onData(new Http2InputMessageFrame(input, false));
        assertArrayEquals(new byte[] {42}, listener.decoded);
        assertReleasedOnce();
    }

    @Test
    void buffersWebSocketFragmentsUntilFinalAndKeepsStreamReusable() {
        listener = new WebSocketTestListener(framework);
        listener.onMetadata(mock(Http2Header.class));
        CountingInput first = new FragmentInput(false, new byte[] {1, 2});
        input = new FragmentInput(true, new byte[] {3});
        listener.onData(new Http2InputMessageFrame(first, false));
        assertNull(listener.decoded);
        listener.onData(new Http2InputMessageFrame(input, false));
        assertArrayEquals(new byte[] {1, 2, 3}, listener.decoded);
        assertEquals(0, first.buffer.refCnt());
        assertReleasedOnce();
        input = new FragmentInput(true, new byte[] {4});
        listener.onData(new Http2InputMessageFrame(input, false));
        assertArrayEquals(new byte[] {4}, listener.decoded);
        assertReleasedOnce();
    }

    private static final class FragmentInput extends CountingInput
            implements org.apache.dubbo.remoting.websocket.FinalFragment {
        private final boolean last;

        FragmentInput(boolean last, byte[] data) {
            super(Unpooled.wrappedBuffer(data));
            this.last = last;
        }

        @Override
        public boolean isFinalFragment() {
            return last;
        }
    }

    @Test
    void closesOriginalMessageOnceEvenWhenCloseThrows() {
        initialize(Runnable::run);
        listener.close();
        input = new CountingInput();
        AtomicInteger closes = new AtomicInteger();
        listener.onData(new Http2InputMessage() {
            @Override
            public InputStream getBody() {
                return input;
            }

            @Override
            public long id() {
                return 41;
            }

            @Override
            public String name() {
                return "DATA";
            }

            @Override
            public boolean isEndStream() {
                return false;
            }

            @Override
            public void close() throws IOException {
                closes.incrementAndGet();
                input.close();
                throw new IOException("test close failure after releasing input");
            }
        });
        listener.close();
        assertReleasedOnce();
        assertEquals(1, closes.get());
    }

    private void initialize(Executor executor) {
        listener = new TestListener(framework, executor);
        listener.onMetadata(mock(Http2Header.class));
    }

    private void assertReleasedOnce() {
        assertEquals(0, input.buffer.refCnt());
        assertEquals(1, input.closes);
    }

    private static class CountingInput extends ByteBufInputStream {
        private final ByteBuf buffer;
        private int closes;
        private CountDownLatch readStarted;
        private CountDownLatch resumeRead;

        CountingInput() {
            this(Unpooled.buffer().writeShort(0));
        }

        private CountingInput(ByteBuf buffer) {
            super(buffer, true);
            this.buffer = buffer;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (readStarted != null) {
                readStarted.countDown();
                try {
                    assertTrue(resumeRead.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
            return super.read(bytes, offset, length);
        }

        @Override
        public void close() throws IOException {
            closes++;
            super.close();
        }
    }

    private static final class WebSocketTestListener extends TestListener {
        WebSocketTestListener(FrameworkModel framework) {
            super(framework, Runnable::run);
        }

        @Override
        protected StreamingDecoder newStreamingDecoder() {
            return new org.apache.dubbo.remoting.websocket.FinalFragmentStreamingDecoder();
        }
    }

    private static class TestListener
            extends org.apache.dubbo.rpc.protocol.tri.websocket.DefaultWebSocketServerTransportListener {
        private Executor dataExecutor;
        private byte[] decoded;
        private CountDownLatch dataPrepared;
        private CountDownLatch resumeData;
        private CountDownLatch deliveryStarted;
        private CountDownLatch resumeDelivery;
        private long frameId;
        private boolean endStream;

        TestListener(FrameworkModel framework, Executor executor) {
            super(mock(H2StreamChannel.class), URL.valueOf("tri://127.0.0.1:50051"), framework);
            dataExecutor = executor;
        }

        @Override
        protected void onBeforeMetadata(Http2Header metadata) {}

        @Override
        protected void onPrepareMetadata(Http2Header metadata) {}

        @Override
        protected Executor initializeExecutor(URL url, Http2Header metadata) {
            return task -> dataExecutor.execute(task);
        }

        @Override
        protected void onPrepareData(Http2InputMessage message) {
            frameId = message.id();
            endStream = message.isEndStream();
            if (dataPrepared != null) {
                dataPrepared.countDown();
                try {
                    assertTrue(resumeData.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
        }

        @Override
        protected HttpMessageListener buildHttpMessageListener() {
            return message -> {
                if (deliveryStarted != null) {
                    deliveryStarted.countDown();
                    try {
                        assertTrue(resumeDelivery.await(5, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                }
                getStreamingDecoder().decode(message);
            };
        }

        @Override
        protected void onMetadataCompletion(Http2Header metadata) {
            getStreamingDecoder().setFragmentListener(new StreamingDecoder.FragmentListener() {
                @Override
                public void bytesRead(int count) {}

                @Override
                public void onFragmentMessage(InputStream message, int length) {
                    try {
                        decoded = new byte[length];
                        assertEquals(length, message.read(decoded));
                    } catch (IOException e) {
                        throw new AssertionError(e);
                    }
                }

                @Override
                public void onClose() {}
            });
            getStreamingDecoder().request(1);
        }

        @Override
        protected StreamingDecoder newStreamingDecoder() {
            return new GrpcStreamingDecoder();
        }

        @Override
        protected Http2ServerChannelObserver newResponseObserver(H2StreamChannel channel) {
            return new Http2ServerChannelObserver(channel);
        }

        @Override
        protected void onError(Throwable throwable) {}
    }
}
