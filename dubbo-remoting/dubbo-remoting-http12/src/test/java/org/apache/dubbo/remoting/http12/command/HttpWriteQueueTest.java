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
package org.apache.dubbo.remoting.http12.command;

import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpChannelHolder;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;

import org.junit.jupiter.api.Test;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link HttpWriteQueue} to verify HTTP write queue functionality.
 */
class HttpWriteQueueTest {

    @Test
    void testConstructorWithOnlyExecutor() {
        Executor executor = Runnable::run;
        HttpWriteQueue queue = new HttpWriteQueue(executor);

        assertNotNull(queue);
        // When flushCallback is null, flush() calls item.getHttpChannel().flush()
        HttpChannelQueueCommand cmd = new TestHttpChannelQueueCommand();
        queue.flush(cmd);
        // flushCallback is null, so item.getHttpChannel().flush() is called
        // Since testHttpChannel is not null, flush() is called on it
        assertTrue(cmd.isRunCalled());
        assertTrue(cmd.getHttpChannel().isFlushCalled());
    }

    @Test
    void testConstructorWithExecutorAndFlushCallback() {
        Executor executor = Runnable::run;
        Runnable flushCallback = () -> {};
        HttpWriteQueue queue = new HttpWriteQueue(executor, flushCallback);

        assertNotNull(queue);
    }

    @Test
    void testEnqueue() {
        Executor executor = Runnable::run;
        HttpWriteQueue queue = new HttpWriteQueue(executor);

        HttpChannelQueueCommand cmd = new TestHttpChannelQueueCommand();
        CompletableFuture<Void> result = queue.enqueue(cmd);

        assertSame(cmd, result);
    }

    @Test
    void testPrepare() {
        Executor executor = Runnable::run;
        HttpWriteQueue queue = new HttpWriteQueue(executor);

        TestHttpChannelQueueCommand cmd = new TestHttpChannelQueueCommand();
        queue.prepare(cmd);

        assertTrue(cmd.isRunCalled());
    }

    @Test
    void testFlushWithFlushCallback() {
        Executor executor = Runnable::run;
        boolean[] flushCallbackCalled = {false};
        Runnable flushCallback = () -> flushCallbackCalled[0] = true;
        HttpWriteQueue queue = new HttpWriteQueue(executor, flushCallback);

        TestHttpChannelQueueCommand cmd = new TestHttpChannelQueueCommand();
        queue.flush(cmd);

        assertTrue(cmd.isRunCalled());
        assertTrue(flushCallbackCalled[0]);
        // flushCallback was called, so item.getHttpChannel().flush() should NOT be called
        assertFalse(cmd.getHttpChannel().isFlushCalled());
    }

    @Test
    void testFlushWithoutFlushCallback() {
        Executor executor = Runnable::run;
        HttpWriteQueue queue = new HttpWriteQueue(executor);

        TestHttpChannelQueueCommand cmd = new TestHttpChannelQueueCommand();
        queue.flush(cmd);

        assertTrue(cmd.isRunCalled());
        // flushCallback is null, so item.getHttpChannel().flush() is called
        assertTrue(cmd.getHttpChannel().isFlushCalled());
    }

    /**
     * Test implementation of HttpChannelQueueCommand for testing purposes.
     */
    private static class TestHttpChannelQueueCommand extends HttpChannelQueueCommand {
        private boolean runCalled = false;
        private boolean flushCalled = false;

        @Override
        public void run() {
            runCalled = true;
        }

        public boolean isRunCalled() {
            return runCalled;
        }

        @Override
        public HttpChannel getHttpChannel() {
            return testHttpChannel;
        }

        public boolean isFlushCalled() {
            return flushCalled;
        }

        private final TestHttpChannel testHttpChannel = new TestHttpChannel();

        private static class TestHttpChannel implements HttpChannel {
            private boolean flushCalled = false;

            @Override
            public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
                return null;
            }

            @Override
            public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
                return null;
            }

            @Override
            public HttpOutputMessage newOutputMessage() {
                return null;
            }

            @Override
            public SocketAddress remoteAddress() {
                return null;
            }

            @Override
            public SocketAddress localAddress() {
                return null;
            }

            @Override
            public void flush() {
                flushCalled = true;
            }

            public boolean isFlushCalled() {
                return flushCalled;
            }
        }
    }
}
