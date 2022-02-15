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

package org.apache.dubbo.rpc.protocol.tri.stream;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.DefaultFuture2;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.RpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleClientHandler;
import org.apache.dubbo.rpc.protocol.tri.WriteQueue;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.QueuedCommand;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeter;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientStreamTest {
    @Test
    public void create() {
        final URL url = URL.valueOf("tri://127.0.0.1:8080/foo.bar.service");
        final Connection connection = mock(Connection.class);

        final ModuleServiceRepository repo = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();
        repo.registerService(IGreeter.class);
        final ServiceDescriptor serviceDescriptor = repo.getService(IGreeter.class.getName());
        final MethodDescriptor methodDescriptor = serviceDescriptor.getMethod("echo", new Class<?>[]{String.class});

        final EmbeddedChannel parentChannel = new EmbeddedChannel();
        final Http2FrameCodec codec = Http2FrameCodecBuilder.forClient().build();
        final Http2MultiplexHandler handler = new Http2MultiplexHandler(new TripleClientHandler(url.getOrDefaultFrameworkModel()));
        parentChannel.pipeline().addLast(codec)
            .addLast(handler);
        parentChannel.connect(new InetSocketAddress(0));
        when(connection.getChannel())
            .thenReturn(parentChannel);

        final RpcInvocation invocation = mock(RpcInvocation.class);
        int timeout = 1000;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        DefaultFuture2 future = DefaultFuture2.newFuture(connection, invocation, timeout, executorService);
        ClientStreamListener listener = new ClientStreamListener() {
            public RpcStatus status;
            byte[] message;

            @Override
            public void complete(RpcStatus rpcStatus, Map<String, Object> attachments, Map<String, String> excludeHeaders) {
                this.status = rpcStatus;
            }

            @Override
            public void onMessage(byte[] message) {
                this.message = message;
            }
        };
        WriteQueue writeQueue = mock(WriteQueue.class);
        when(writeQueue.enqueue(any())).thenReturn(parentChannel.newPromise());
        ClientStream stream = new ClientStream(url, future.requestId, writeQueue, listener);

        final RequestMetadata requestMetadata = StreamUtils.createRequest(url, methodDescriptor,
            invocation, future.requestId, Identity.IDENTITY,
            "identity", timeout, mock(GenericPack.class), mock(GenericUnpack.class));
        stream.startCall(requestMetadata);
        verify(writeQueue).enqueue(any(HeaderQueueCommand.class));
        // no other commands
        verify(writeQueue).enqueue(any(QueuedCommand.class));
    }

}