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
package org.apache.dubbo.remoting.handler;

import org.apache.dubbo.remoting.ExecutionException;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.transport.dispatcher.connection.ConnectionOrderedChannelHandler;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;


public class ConnectChannelHandlerTest extends WrappedChannelHandlerTest {

    @BeforeEach
    public void setUp() throws Exception {
        url = url.setScopeModel(ApplicationModel.defaultModel());
        handler = new ConnectionOrderedChannelHandler(new BizChannelHandler(true), url);
    }

    @Test
    public void testConnectBlocked() throws RemotingException {
        handler = new ConnectionOrderedChannelHandler(new BizChannelHandler(false), url);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) getField(handler, "connectionExecutor", 1);
        Assertions.assertEquals(1, executor.getMaximumPoolSize());

        int runs = 20;
        int taskCount = runs * 2;
        for (int i = 0; i < runs; i++) {
            handler.connected(new MockedChannel());
            handler.disconnected(new MockedChannel());
            Assertions.assertTrue(executor.getActiveCount() <= 1, executor.getActiveCount() + " must <=1");
        }
        //queue.size
        Assertions.assertEquals(taskCount - 1, executor.getQueue().size());

        for (int i = 0; i < taskCount; i++) {
            if (executor.getCompletedTaskCount() < taskCount) {
                sleep(100);
            }
        }
        Assertions.assertEquals(taskCount, executor.getCompletedTaskCount());
    }

    @Test //biz error should not throw and affect biz thread.
    public void testConnectBizError() throws RemotingException {
        handler = new ConnectionOrderedChannelHandler(new BizChannelHandler(true), url);
        handler.connected(new MockedChannel());
    }

    @Test //biz error should not throw and affect biz thread.
    public void testDisconnectBizError() throws RemotingException {
        handler = new ConnectionOrderedChannelHandler(new BizChannelHandler(true), url);
        handler.disconnected(new MockedChannel());
    }

    @Test
    public void testConnectExecuteError() throws RemotingException {
        Assertions.assertThrows(ExecutionException.class, () -> {
            handler = new ConnectionOrderedChannelHandler(new BizChannelHandler(false), url);
            ThreadPoolExecutor executor = (ThreadPoolExecutor) getField(handler, "connectionExecutor", 1);
            executor.shutdown();
            handler.connected(new MockedChannel());
        });
    }

    @Test
    public void testDisconnectExecuteError() throws RemotingException {
        Assertions.assertThrows(ExecutionException.class, () -> {
            handler = new ConnectionOrderedChannelHandler(new BizChannelHandler(false), url);
            ThreadPoolExecutor executor = (ThreadPoolExecutor) getField(handler, "connectionExecutor", 1);
            executor.shutdown();
            handler.disconnected(new MockedChannel());
        });
    }

    //throw  ChannelEventRunnable.runtimeExeception(int logger) not in execute exception
    @Test//(expected = RemotingException.class)
    public void testMessageReceivedBizError() throws RemotingException {
        handler.received(new MockedChannel(), "");
    }

    //throw  ChannelEventRunnable.runtimeExeception(int logger) not in execute exception
    @Test
    public void testCaughtBizError() throws RemotingException {
        handler.caught(new MockedChannel(), new BizException());
    }

    @Test
    @Disabled("FIXME")
    public void testReceivedInvokeInExecutor() throws RemotingException {
        Assertions.assertThrows(ExecutionException.class, () -> {
            handler = new ConnectionOrderedChannelHandler(new BizChannelHandler(false), url);
            ThreadPoolExecutor executor = (ThreadPoolExecutor) getField(handler, "SHARED_EXECUTOR", 1);
            executor.shutdown();
            executor = (ThreadPoolExecutor) getField(handler, "executor", 1);
            executor.shutdown();
            handler.received(new MockedChannel(), "");
        });
    }

    /**
     * Events do not pass through the thread pool and execute directly on the IO
     */
    @SuppressWarnings("deprecation")
    @Disabled("Heartbeat is processed in HeartbeatHandler not WrappedChannelHandler.")
    @Test
    public void testReceivedEventInvokeDirect() throws RemotingException {
        handler = new ConnectionOrderedChannelHandler(new BizChannelHandler(false), url);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) getField(handler, "SHARED_EXECUTOR", 1);
        executor.shutdown();
        executor = (ThreadPoolExecutor) getField(handler, "executor", 1);
        executor.shutdown();
        Request req = new Request();
        req.setHeartbeat(true);
        final AtomicInteger count = new AtomicInteger(0);
        handler.received(new MockedChannel() {
            @Override
            public void send(Object message) throws RemotingException {
                Assertions.assertTrue(((Response) message).isHeartbeat(), "response.heartbeat");
                count.incrementAndGet();
            }
        }, req);
        Assertions.assertEquals(1, count.get(), "channel.send must be invoke");
    }
}
