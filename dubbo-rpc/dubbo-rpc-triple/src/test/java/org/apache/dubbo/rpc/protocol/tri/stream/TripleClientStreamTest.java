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

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http2.Http2StreamChannel;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.command.CancelQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.CreateStreamQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.EndStreamQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.QueuedCommand;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeter;
import org.apache.dubbo.rpc.protocol.tri.transport.H2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TripleClientStreamTest {

    @Test
    void progress() {
        final URL url = URL.valueOf("tri://127.0.0.1:8080/foo.bar.service");
        final ModuleServiceRepository repo = ApplicationModel.defaultModel().getDefaultModule()
            .getServiceRepository();
        repo.registerService(IGreeter.class);
        final ServiceDescriptor serviceDescriptor = repo.getService(IGreeter.class.getName());
        final MethodDescriptor methodDescriptor = serviceDescriptor.getMethod("echo",
            new Class<?>[]{String.class});

        MockClientStreamListener listener = new MockClientStreamListener();
        TripleWriteQueue writeQueue = mock(TripleWriteQueue.class);
        final EmbeddedChannel channel = new EmbeddedChannel();
        when(writeQueue.enqueueFuture(any(QueuedCommand.class), any(Executor.class))).thenReturn(channel.newPromise());
        Http2StreamChannel http2StreamChannel = mock(Http2StreamChannel.class);
        when(http2StreamChannel.isActive()).thenReturn(true);
        when(http2StreamChannel.newSucceededFuture()).thenReturn(channel.newSucceededFuture());
        when(http2StreamChannel.eventLoop()).thenReturn(new NioEventLoopGroup().next());
        when(http2StreamChannel.newPromise()).thenReturn(channel.newPromise());
        when(http2StreamChannel.parent()).thenReturn(channel);
        TripleClientStream stream = new TripleClientStream(url.getOrDefaultFrameworkModel(),
            ImmediateEventExecutor.INSTANCE, writeQueue, listener, http2StreamChannel);
        verify(writeQueue).enqueue(any(CreateStreamQueueCommand.class));

        final RequestMetadata requestMetadata = new RequestMetadata();
        requestMetadata.method = methodDescriptor;
        requestMetadata.scheme = TripleConstant.HTTP_SCHEME;
        requestMetadata.compressor = Compressor.NONE;
        requestMetadata.acceptEncoding = Compressor.NONE.getMessageEncoding();
        requestMetadata.address = url.getAddress();
        requestMetadata.service = url.getPath();
        requestMetadata.group = url.getGroup();
        requestMetadata.version = url.getVersion();
        stream.sendHeader(requestMetadata.toHeaders());
        verify(writeQueue).enqueueFuture(any(HeaderQueueCommand.class), any(Executor.class));
        // no other commands
        verify(writeQueue).enqueue(any(QueuedCommand.class));
        stream.sendMessage(new byte[0], 0, false);
        verify(writeQueue).enqueueFuture(any(DataQueueCommand.class), any(Executor.class));
        verify(writeQueue, times(2)).enqueueFuture(any(QueuedCommand.class), any(Executor.class));
        stream.halfClose();
        verify(writeQueue).enqueueFuture(any(EndStreamQueueCommand.class), any(Executor.class));
        verify(writeQueue, times(3)).enqueueFuture(any(QueuedCommand.class), any(Executor.class));

        stream.cancelByLocal(TriRpcStatus.CANCELLED);
        verify(writeQueue, times(1)).enqueue(any(CancelQueueCommand.class));
        verify(writeQueue, times(3)).enqueueFuture(any(QueuedCommand.class), any(Executor.class));

        H2TransportListener transportListener = stream.createTransportListener();
        DefaultHttp2Headers headers = new DefaultHttp2Headers();
        headers.scheme(HttpScheme.HTTP.name())
            .status(HttpResponseStatus.OK.codeAsText());
        headers.set(TripleHeaderEnum.STATUS_KEY.getHeader(), TriRpcStatus.OK.code.code + "");
        headers.set(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(),
            TripleHeaderEnum.CONTENT_PROTO.getHeader());
        transportListener.onHeader(headers, false);
        Assertions.assertTrue(listener.started);
        stream.request(2);
        byte[] data = new byte[]{0, 0, 0, 0, 1, 1};
        final ByteBuf buf = Unpooled.wrappedBuffer(data);
        transportListener.onData(buf, false);
        buf.release();
        Assertions.assertEquals(1, listener.message.length);
    }
}
