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
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.serialize.DefaultMultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.QueuedCommand;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeter;
import org.apache.dubbo.rpc.protocol.tri.support.MockStreamObserver;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.util.Attribute;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.rpc.protocol.tri.Compressor.DEFAULT_COMPRESSOR;
import static org.apache.dubbo.rpc.protocol.tri.TripleConstant.HTTP_SCHEME;
import static org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum.GRPC_ENCODING;

/**
 * {@link ClientStream}
 * {@link UnaryClientStream}
 * {@link AbstractClientStream}
 */
public class ClientStreamTest {

    private URL url;
    private ConsumerModel consumerModel;
    private Invoker<IGreeter> invoker;
    private Connection connection;
    private int timeout = 100000;
    private AtomicInteger writeMethodCalledTimes = new AtomicInteger(0);


    @BeforeEach
    public void init() {
        url = URL.valueOf("tri://127.0.0.1:9103/" + IGreeter.class.getName());
        ModuleServiceRepository serviceRepository = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();
        ServiceDescriptor serviceDescriptor = serviceRepository.registerService(IGreeter.class);
        consumerModel = new ConsumerModel(url.getServiceKey(), null, serviceDescriptor, null,
            new ServiceMetadata(), null);
        url = url.setServiceModel(consumerModel);

        invoker = Mockito.mock(Invoker.class);
        Mockito.when(invoker.getUrl()).thenReturn(url);

        connection = Mockito.mock(Connection.class);

        writeMethodCalledTimes.set(0);
    }

    @Test
    public void testNewClientStream() throws Exception {

        Method echoMethod = IGreeter.class.getDeclaredMethod("echo", String.class);
        RpcInvocation rpcInvocation = new RpcInvocation(consumerModel, echoMethod, IGreeter.class.getName(), url.getProtocolServiceKey(), new Object[]{"ECHO"});
        rpcInvocation.setInvoker(invoker);

        Request request = new Request(1);
        request.setData(rpcInvocation);

        // test UnaryClientStream
        AbstractClientStream stream = AbstractClientStream.newClientStream(request, connection);

        Assertions.assertTrue(stream instanceof UnaryClientStream);
        UnaryClientStream unaryClientStream = (UnaryClientStream) stream;
        Assertions.assertEquals(unaryClientStream.getConnection(), connection);
        Assertions.assertEquals(unaryClientStream.getConsumerModel(), consumerModel);
        Assertions.assertEquals(unaryClientStream.getMethodName(), echoMethod.getName());
        Assertions.assertEquals(unaryClientStream.getUrl(), url);
        Assertions.assertEquals(unaryClientStream.getRequestId(), request.getId());
        Assertions.assertEquals(unaryClientStream.getRpcInvocation(), rpcInvocation);
        Assertions.assertEquals(unaryClientStream.getCompressor(), Compressor.NONE);
        Assertions.assertEquals(unaryClientStream.getDeCompressor(), Compressor.NONE);
        Assertions.assertEquals(stream.getScheme(), HTTP_SCHEME);
        Assertions.assertEquals(stream.getAcceptEncoding(), "gzip");
        Assertions.assertFalse(stream.getCancellationContext().getListeners().isEmpty());
        Assertions.assertTrue(stream.getMultipleSerialization() instanceof DefaultMultipleSerialization);

        // test ClientStream
        Method serverStreamMethod = IGreeter.class.getDeclaredMethod("serverStream", String.class, StreamObserver.class);
        rpcInvocation = new RpcInvocation(consumerModel, serverStreamMethod, IGreeter.class.getName(), url.getProtocolServiceKey(), new Object[]{null, null});
        request.setData(rpcInvocation);
        rpcInvocation.setInvoker(invoker);
        stream = AbstractClientStream.newClientStream(request, connection);
        Assertions.assertTrue(stream instanceof ClientStream);
    }

    @Test
    public void testStartCall_UnaryClientStream() throws Throwable {
        //  1. test startCall
        Method echoMethod = IGreeter.class.getDeclaredMethod("echo", String.class);
        RpcInvocation rpcInvocation = new RpcInvocation(consumerModel, echoMethod, IGreeter.class.getName(),
            url.getProtocolServiceKey(), new Object[]{"ECHO"});
        rpcInvocation.setInvoker(invoker);
        rpcInvocation.setObjectAttachment(Constants.SERIALIZATION_KEY, TripleConstant.HESSIAN2);
        rpcInvocation.setObjectAttachment(CommonConstants.PATH_KEY, url.getPath());
        rpcInvocation.put(CommonConstants.TIMEOUT_KEY, timeout);
        Request request = new Request(1);
        request.setData(rpcInvocation);

        ExecutorService executor = Mockito.mock(ExecutorService.class);
        DefaultFuture2 future = DefaultFuture2.newFuture(connection, request, timeout, executor);

        AbstractClientStream stream = AbstractClientStream.newClientStream(request, connection);

        ChannelPromise promise = Mockito.mock(ChannelPromise.class);
        Http2StreamChannel streamChannel = getHttp2StreamChannel(stream);

        // startCall
        WriteQueue writeQueue = new WriteQueue(streamChannel);
        stream.startCall(writeQueue, promise);
        // Wait for the asynchronous operation to complete
        while (writeMethodCalledTimes.get() != 3) {
            Thread.sleep(50);
        }

        Assertions.assertNotNull(stream.outboundTransportObserver());
        ArgumentCaptor<QueuedCommand> commandArgumentCaptor = ArgumentCaptor.forClass(QueuedCommand.class);
        ArgumentCaptor<ChannelPromise> promiseArgumentCaptor = ArgumentCaptor.forClass(ChannelPromise.class);
        Mockito.verify(streamChannel, Mockito.times(3)).write(commandArgumentCaptor.capture(), promiseArgumentCaptor.capture());
        List<QueuedCommand> queuedCommands = commandArgumentCaptor.getAllValues();

        HeaderQueueCommand headerQueueCommand = (HeaderQueueCommand) queuedCommands.get(0);
        Http2Headers headers = headerQueueCommand.getHeaders();
        Assertions.assertEquals(headers.get(Http2Headers.PseudoHeaderName.SCHEME.value()), HTTP_SCHEME);
        Assertions.assertEquals(headers.get(Http2Headers.PseudoHeaderName.PATH.value()), "/" + url.getPath() + "/" + rpcInvocation.getMethodName());
        Assertions.assertEquals(headers.get(Http2Headers.PseudoHeaderName.AUTHORITY.value()), url.getAddress());
        Assertions.assertEquals(headers.get(Http2Headers.PseudoHeaderName.METHOD.value()), HttpMethod.POST.asciiName());
        Assertions.assertEquals(headers.get(HttpHeaderNames.TE), HttpHeaderValues.TRAILERS);
        Assertions.assertNull(headers.get(GRPC_ENCODING.getHeader()));
        Assertions.assertEquals(headers.get(TripleHeaderEnum.GRPC_ACCEPT_ENCODING.getHeader()), stream.getAcceptEncoding());
        Assertions.assertEquals(headers.get(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader()), TripleHeaderEnum.CONTENT_PROTO.getHeader());
        Assertions.assertEquals(headers.get(TripleHeaderEnum.TIMEOUT.getHeader()), timeout + "m");

        DataQueueCommand dataQueueCommand1 = (DataQueueCommand) queuedCommands.get(1);
        Assertions.assertTrue(dataQueueCommand1.getData().length > 0);
        Assertions.assertFalse(dataQueueCommand1.isEndStream());
        Assertions.assertTrue(dataQueueCommand1.isClient());
        TripleWrapper.TripleRequestWrapper requestWrapper = stream.unpack(dataQueueCommand1.getData(), TripleWrapper.TripleRequestWrapper.class);
        ByteArrayInputStream bais = new ByteArrayInputStream(requestWrapper.getArgs(0).toByteArray());
        Object ret = stream.getMultipleSerialization().deserialize(url, stream.getSerializeType(), requestWrapper.getArgTypes(0), bais);
        bais.close();
        Assertions.assertEquals(ret.toString(), "ECHO");

        DataQueueCommand dataQueueCommand2 = (DataQueueCommand) queuedCommands.get(2);
        Assertions.assertNull(dataQueueCommand2.getData());
        Assertions.assertTrue(dataQueueCommand2.isEndStream());
        // 2. Verify the data from the server()
        // NOTE: The onXX method of inboundTransportObserver is usually triggered when receiving server data,
        // here we manually call to simulate the behavior of the [server -> client]
        TransportObserver inboundTransportObserver = stream.inboundTransportObserver();
        headers = getHttp2Headers(stream);
        inboundTransportObserver.onMetadata(new Http2HeaderMeta(headers), false);
        Object resp = "RESPONSE";
        byte[] bytes = getPackedData(stream, resp);
        inboundTransportObserver.onData(bytes, false);
        inboundTransportObserver.onMetadata(TripleConstant.getSuccessResponseMeta(), false); // trailers
        inboundTransportObserver.onComplete();
        Object result = future.get();
        Assertions.assertEquals(((AppResponse) result).recreate(), resp);
        // TODO onError case
    }

    @Test
    public void testStartCall_ClientStream_ServerStream() throws Throwable {
        //  1. test startCall
        StreamObserver<String> outboundMessageSubscriber = new MockStreamObserver();
        Method serverStreamMethod = IGreeter.class.getDeclaredMethod("serverStream", String.class, StreamObserver.class);
        RpcInvocation rpcInvocation = new RpcInvocation(consumerModel, serverStreamMethod, IGreeter.class.getName(),
            url.getProtocolServiceKey(), new Object[]{"stringData", outboundMessageSubscriber});
        rpcInvocation.setInvoker(invoker);
        rpcInvocation.setObjectAttachment(Constants.SERIALIZATION_KEY, TripleConstant.HESSIAN2);
        Request request = new Request(1);
        request.setData(rpcInvocation);
        ExecutorService executor = Mockito.mock(ExecutorService.class);
        DefaultFuture2 future = DefaultFuture2.newFuture(connection, request, timeout, executor);
        AbstractClientStream stream = AbstractClientStream.newClientStream(request, connection);

        ChannelPromise promise = Mockito.mock(ChannelPromise.class);
        Http2StreamChannel streamChannel = getHttp2StreamChannel(stream);
        // startCall
        WriteQueue writeQueue = new WriteQueue(streamChannel);
        stream.startCall(writeQueue, promise);
        // Wait for the asynchronous operation to complete
        while (writeMethodCalledTimes.get() != 3) {
            Thread.sleep(50);
        }
        Assertions.assertNull(DefaultFuture2.getFuture(request.getId()));
        Assertions.assertNull(((AppResponse) future.get()).recreate());
        // NOTE: Send one header frame and two data frames. The previous test case has been verified, so I won’t go into details here.
        Mockito.verify(streamChannel, Mockito.times(3)).write(Mockito.any(), Mockito.any());

        // 2. Verify the data from the server()
        // NOTE: The onXX method of inboundTransportObserver is usually triggered when receiving server data,
        // here we manually call to simulate the behavior of the [server -> client]
        TransportObserver inboundTransportObserver = stream.inboundTransportObserver();
        DefaultHttp2Headers headers = getHttp2Headers(stream);
        inboundTransportObserver.onMetadata(new Http2HeaderMeta(headers), false);
        Object resp = "RESPONSE";
        byte[] bytes = getPackedData(stream, resp);
        inboundTransportObserver.onData(bytes, false);
        inboundTransportObserver.onMetadata(TripleConstant.SUCCESS_RESPONSE_META, false);
        inboundTransportObserver.onComplete();
        MockStreamObserver observer = (MockStreamObserver) outboundMessageSubscriber;
        observer.getLatch().await(1000, TimeUnit.MILLISECONDS); // Wait for the asynchronous operation to complete
        Assertions.assertEquals(observer.getOnNextData(), resp);
        Assertions.assertTrue(observer.isOnCompleted());

    }


    @Test
    public void testStartCall_ClientStream_BidirectionalStream() throws Throwable {
        //  1. test startCall
        StreamObserver<String> outboundMessageSubscriber = new MockStreamObserver();
        Method bidirectionalStreamMethod = IGreeter.class.getDeclaredMethod("bidirectionalStream", StreamObserver.class);
        RpcInvocation rpcInvocation = new RpcInvocation(consumerModel, bidirectionalStreamMethod, IGreeter.class.getName(),
            url.getProtocolServiceKey(), new Object[]{outboundMessageSubscriber});
        rpcInvocation.setInvoker(invoker);
        rpcInvocation.setObjectAttachment(Constants.SERIALIZATION_KEY, TripleConstant.HESSIAN2);
        Request request = new Request(1);
        request.setData(rpcInvocation);
        ExecutorService executor = Mockito.mock(ExecutorService.class);
        DefaultFuture2 future = DefaultFuture2.newFuture(connection, request, timeout, executor);
        AbstractClientStream stream = AbstractClientStream.newClientStream(request, connection);
        ChannelPromise promise = Mockito.mock(ChannelPromise.class);
        Http2StreamChannel streamChannel = getHttp2StreamChannel(stream);
        // startCall
        WriteQueue writeQueue = new WriteQueue(streamChannel);
        stream.startCall(writeQueue, promise);
        // Wait for the asynchronous operation to complete
        while (DefaultFuture2.getFuture(request.getId()) != null) {
            Thread.sleep(50);
        }
        Assertions.assertNull(DefaultFuture2.getFuture(request.getId()));
        Assertions.assertEquals(stream.outboundMessageSubscriber(), outboundMessageSubscriber);
        Assertions.assertEquals(((AppResponse) future.get()).recreate(), stream.inboundMessageObserver());
        Mockito.verify(streamChannel, Mockito.times(0)).write(Mockito.any(), Mockito.any());

        // 2. Verify the data from the server()
        // Simulate the client to initiate a call
        StreamObserver<Object> inboundMessageObserver = stream.inboundMessageObserver();
        inboundMessageObserver.onNext("TEST");
        inboundMessageObserver.onCompleted();
        while (writeMethodCalledTimes.get() != 3) {
            Thread.sleep(50);
        }
        Mockito.verify(streamChannel, Mockito.times(3)).write(Mockito.any(), Mockito.any());

        // NOTE: The onXX method of inboundTransportObserver is usually triggered when receiving server data,
        // here we manually call to simulate the behavior of the [server -> client]
        TransportObserver inboundTransportObserver = stream.inboundTransportObserver();
        DefaultHttp2Headers headers = getHttp2Headers(stream);
        inboundTransportObserver.onMetadata(new Http2HeaderMeta(headers), false);
        Object resp = "RESPONSE";
        byte[] bytes = getPackedData(stream, resp);
        inboundTransportObserver.onData(bytes, false);
        inboundTransportObserver.onMetadata(TripleConstant.SUCCESS_RESPONSE_META, false);
        inboundTransportObserver.onComplete();

        MockStreamObserver observer = (MockStreamObserver) outboundMessageSubscriber;
        observer.getLatch().await(1000, TimeUnit.MILLISECONDS);
        Assertions.assertEquals(observer.getOnNextData(), resp);
        Assertions.assertTrue(observer.isOnCompleted());
    }


    private Http2StreamChannel getHttp2StreamChannel(AbstractClientStream stream) {
        ChannelFuture channelFuture = Mockito.mock(ChannelFuture.class);
        Http2StreamChannel streamChannel = Mockito.mock(Http2StreamChannel.class);
        Attribute<AbstractClientStream> attribute = Mockito.mock(Attribute.class);
        ChannelPromise promise = Mockito.mock(ChannelPromise.class);
        EventLoop eventLoop = new DefaultEventLoop();

        Mockito.when(attribute.get()).thenReturn(stream);
        Mockito.when(streamChannel.writeAndFlush(Mockito.any())).thenReturn(channelFuture);
        Mockito.when(streamChannel.alloc()).thenReturn(ByteBufAllocator.DEFAULT);
        Mockito.when(streamChannel.attr(TripleConstant.CLIENT_STREAM_KEY)).thenReturn(attribute);
        Mockito.when(streamChannel.eventLoop()).thenReturn(eventLoop);
        Mockito.when(streamChannel.newPromise()).thenReturn(promise);

        Mockito.when(streamChannel.write(Mockito.any(), Mockito.any())).thenAnswer(
            (Answer<ChannelPromise>) invocationOnMock -> {
                writeMethodCalledTimes.incrementAndGet();
                return promise;
            });
        return streamChannel;
    }

    private DefaultHttp2Headers getHttp2Headers(AbstractClientStream stream) {
        DefaultHttp2Headers headers = new DefaultHttp2Headers(true);
        headers.set(Http2Headers.PseudoHeaderName.STATUS.value(), HttpResponseStatus.OK.codeAsText());
        headers.set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
        headers.set(TripleHeaderEnum.GRPC_ENCODING.getHeader(), stream.getCompressor().getMessageEncoding());
        headers.set(TripleHeaderEnum.GRPC_ACCEPT_ENCODING.getHeader(), stream.getAcceptEncoding());
        return headers;
    }

    private byte[] getPackedData(AbstractClientStream stream, Object resp) throws IOException {
        final TripleWrapper.TripleResponseWrapper.Builder builder = TripleWrapper.TripleResponseWrapper.newBuilder()
            .setType(stream.getMethodDescriptor().getReturnClass().getName())
            .setSerializeType(TripleConstant.HESSIAN4);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        stream.getMultipleSerialization().serialize(url, TripleConstant.HESSIAN2, stream.getMethodDescriptor().getReturnClass().getName(), resp, bos);
        builder.setData(ByteString.copyFrom(bos.toByteArray()));
        bos.close();
        TripleWrapper.TripleResponseWrapper responseWrapper = builder.build();
        byte[] bytes = stream.pack(responseWrapper);
        return bytes;
    }
}
