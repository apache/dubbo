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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.threadpool.ThreadlessExecutor;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.api.connection.ConnectionManager;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ReflectionMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.call.ClientCall;
import org.apache.dubbo.rpc.protocol.tri.call.TripleClientCall;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeter;

import io.netty.channel.Channel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TripleInvokerTest {

    @Test
    void testNewCall() throws NoSuchMethodException {
        URL url = URL.valueOf("tri://127.0.0.1:9103/" + IGreeter.class.getName());
        Channel channel = Mockito.mock(Channel.class);
        AbstractConnectionClient connectionClient = Mockito.mock(AbstractConnectionClient.class);
        ConnectionManager connectionManager = Mockito.mock(ConnectionManager.class);
        when(connectionManager.connect(any(URL.class), any(ChannelHandler.class)))
                .thenReturn(connectionClient);
        when(connectionClient.getChannel(true))
                .thenReturn(channel);
        when(connectionClient.isConnected()).thenReturn(true);

        ExecutorService executorService = ExecutorRepository.getInstance(url.getOrDefaultApplicationModel())
                .createExecutorIfAbsent(url);
        TripleClientCall call = Mockito.mock(TripleClientCall.class);
        StreamObserver streamObserver = Mockito.mock(StreamObserver.class);
        when(call.start(any(RequestMetadata.class), any(ClientCall.Listener.class)))
                .thenReturn(streamObserver);
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("test");
        invocation.setArguments(new Object[]{streamObserver, streamObserver});
        TripleInvoker<IGreeter> invoker = new TripleInvoker<>(IGreeter.class, url,
                Identity.MESSAGE_ENCODING, connectionClient, new HashSet<>(), executorService);
        MethodDescriptor echoMethod = new ReflectionMethodDescriptor(
                IGreeter.class.getDeclaredMethod("echo", String.class));
        Assertions.assertTrue(invoker.isAvailable());
        invoker.invokeUnary(echoMethod, invocation, call, new ThreadlessExecutor());
        invoker.destroy();
        Assertions.assertFalse(invoker.isAvailable());
    }

}
