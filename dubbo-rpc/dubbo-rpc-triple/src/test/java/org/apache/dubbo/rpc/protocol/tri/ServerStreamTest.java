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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.filter.TokenHeaderFilter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.QueuedCommand;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeter;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeterImpl;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.rpc.protocol.tri.Compressor.DEFAULT_COMPRESSOR;

/**
 * {@link ServerStream}
 * {@link UnaryServerStream}
 * {@link AbstractServerStream}
 */
public class ServerStreamTest {
    private URL url;
    private ProviderModel providerModel;
    private Invoker invoker;
    private IGreeter serviceImpl;
    private int timeout = 10000;
    private AtomicInteger writeMethodCalledTimes = new AtomicInteger(0);
    private final String REQUEST_MSG = "TEST_DATA";

    @BeforeEach
    public void init() {
        serviceImpl = new IGreeterImpl();
        url = URL.valueOf("tri://127.0.0.1:9103/" + IGreeter.class.getName());
        ModuleServiceRepository serviceRepository = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();
        ServiceDescriptor serviceDescriptor = serviceRepository.registerService(IGreeter.class);
        providerModel = new ProviderModel(
            url.getServiceKey(),
            serviceImpl,
            serviceDescriptor,
            null,
            new ServiceMetadata());
        serviceRepository.registerProvider(providerModel);
        url = url.setServiceModel(providerModel);

        ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        invoker = proxy.getInvoker(serviceImpl, IGreeter.class, url);

        writeMethodCalledTimes.set(0);
    }

    @Test
    public void testNewServerStream() {
        AbstractServerStream unaryServerStream = AbstractServerStream.newServerStream(invoker.getUrl(), true);
        Assertions.assertTrue(unaryServerStream instanceof UnaryServerStream);

        AbstractServerStream serverStream = AbstractServerStream.newServerStream(invoker.getUrl(), false);
        Assertions.assertTrue(serverStream instanceof ServerStream);

        Assertions.assertTrue(unaryServerStream.getHeaderFilters().get(0) instanceof TokenHeaderFilter);
        Assertions.assertEquals(unaryServerStream.getSerializeType(), Constants.DEFAULT_REMOTING_SERIALIZATION);
    }


    @Test
    public void testUnaryServerStream() throws Exception {

        AbstractServerStream stream = AbstractServerStream.newServerStream(invoker.getUrl(), true);
        Http2StreamChannel channel = getHttp2StreamChannel(stream);
        WriteQueue writeQueue = new WriteQueue(channel);
        ServerOutboundTransportObserver outboundTransportObserver = new ServerOutboundTransportObserver(writeQueue);
        Method echoMethod = IGreeter.class.getDeclaredMethod("echo", new Class[]{String.class});
        String methodName = echoMethod.getName();
        stream.service(providerModel.getServiceModel())
            .invoker(invoker)
            .methodName(methodName)
            .setDeCompressor(Compressor.NONE)
            .method(providerModel.getServiceModel().getMethod(methodName, new Class[]{String.class}))
            .subscribe(outboundTransportObserver);

        Http2Headers headers = getHttp2Headers(methodName);

        final TransportObserver inboundTransportObserver = stream.inboundTransportObserver();
        inboundTransportObserver.onMetadata(new Http2HeaderMeta(headers), false); // 1
        byte[] data = getPackedData(stream, new Object[]{REQUEST_MSG}, new Class[]{String.class});
        inboundTransportObserver.onData(data, false); // 2
        inboundTransportObserver.onComplete(); // 3

        // Wait for the asynchronous operation to complete
        while (writeMethodCalledTimes.get() != 3) {
            Thread.sleep(50);
        }
        ArgumentCaptor<QueuedCommand> commandArgumentCaptor = ArgumentCaptor.forClass(QueuedCommand.class);
        ArgumentCaptor<ChannelPromise> promiseArgumentCaptor = ArgumentCaptor.forClass(ChannelPromise.class);
        Mockito.verify(channel, Mockito.times(3)).write(commandArgumentCaptor.capture(), promiseArgumentCaptor.capture());
        List<QueuedCommand> queuedCommands = commandArgumentCaptor.getAllValues();

        HeaderQueueCommand headerQueueCommand1 = (HeaderQueueCommand) queuedCommands.get(0);
        Http2Headers headers1 = headerQueueCommand1.getHeaders();
        Assertions.assertEquals(headers1.get(Http2Headers.PseudoHeaderName.STATUS.value()), HttpResponseStatus.OK.codeAsText());
        Assertions.assertEquals(headers1.get(HttpHeaderNames.CONTENT_TYPE), TripleConstant.CONTENT_PROTO);
        Assertions.assertEquals(headers1.get(TripleHeaderEnum.GRPC_ENCODING.getHeader()), stream.getCompressor().getMessageEncoding());
        Assertions.assertEquals(headers1.get(TripleHeaderEnum.GRPC_ACCEPT_ENCODING.getHeader()), stream.getAcceptEncoding());

        DataQueueCommand dataQueueCommand = (DataQueueCommand) queuedCommands.get(1);
        Assertions.assertTrue(dataQueueCommand.getData().length > 0);
        Assertions.assertFalse(dataQueueCommand.isEndStream());
        Assertions.assertFalse(dataQueueCommand.isClient());
        TripleWrapper.TripleResponseWrapper responseWrapper = stream.unpack(dataQueueCommand.getData(), TripleWrapper.TripleResponseWrapper.class);
        ByteArrayInputStream bais = new ByteArrayInputStream(responseWrapper.getData().toByteArray());
        Object ret = stream.getMultipleSerialization().deserialize(url, stream.getSerializeType(), responseWrapper.getType(), bais);
        bais.close();
        Assertions.assertEquals(ret.toString(), REQUEST_MSG);

        HeaderQueueCommand headerQueueCommand2 = (HeaderQueueCommand) queuedCommands.get(2);
        Assertions.assertTrue(headerQueueCommand2.isEndStream());
        Http2Headers headers2 = headerQueueCommand2.getHeaders();
        Assertions.assertEquals(headers2.get(TripleHeaderEnum.MESSAGE_KEY.getHeader()), TripleConstant.SUCCESS_RESPONSE_MESSAGE);
        Assertions.assertEquals(headers2.get(TripleHeaderEnum.STATUS_KEY.getHeader()), TripleConstant.SUCCESS_RESPONSE_STATUS);
    }


    @Test
    public void testServerStream() throws Exception {
        AbstractServerStream stream = AbstractServerStream.newServerStream(invoker.getUrl(), false);
        Http2StreamChannel channel = getHttp2StreamChannel(stream);
        WriteQueue writeQueue = new WriteQueue(channel);
        ServerOutboundTransportObserver outboundTransportObserver = new ServerOutboundTransportObserver(writeQueue);
        Method serverStreamMethod = IGreeter.class.getDeclaredMethod("serverStream", new Class[]{String.class, StreamObserver.class});
        String methodName = serverStreamMethod.getName();
        stream.service(providerModel.getServiceModel())
            .invoker(invoker)
            .methodName(methodName)
            .setDeCompressor(Compressor.NONE)
            .method(providerModel.getServiceModel().getMethod(methodName, new Class[]{String.class, StreamObserver.class}))
            .subscribe(outboundTransportObserver);


        final TransportObserver inboundTransportObserver = stream.inboundTransportObserver();
        Http2Headers headers = getHttp2Headers(methodName);
        inboundTransportObserver.onMetadata(new Http2HeaderMeta(headers), false); // 1
        byte[] data = getPackedData(stream, new Object[]{REQUEST_MSG}, new Class[]{String.class});
        inboundTransportObserver.onData(data, false); // 2
        inboundTransportObserver.onComplete(); // 3

        // Wait for the asynchronous operation to complete
        while (writeMethodCalledTimes.get() != 3) {
            Thread.sleep(50);
        }
        // NOTE: Send two header frames and one data frames. The previous test case has been verified, so I won’t go into details here.
        Mockito.verify(channel, Mockito.times(3)).write(Mockito.any(), Mockito.any());
    }

    @Test
    public void testServerStream_biDirectional() throws Exception {
        AbstractServerStream stream = AbstractServerStream.newServerStream(invoker.getUrl(), false);
        Http2StreamChannel channel = getHttp2StreamChannel(stream);
        WriteQueue writeQueue = new WriteQueue(channel);
        ServerOutboundTransportObserver outboundTransportObserver = new ServerOutboundTransportObserver(writeQueue);
        Method bidirectionalStreamMethod = IGreeter.class.getDeclaredMethod("bidirectionalStream", new Class[]{StreamObserver.class});
        String methodName = bidirectionalStreamMethod.getName();
        stream.service(providerModel.getServiceModel())
            .invoker(invoker)
            .methodName(methodName)
            .setDeCompressor(Compressor.NONE)
            .method(providerModel.getServiceModel().getMethod(methodName, new Class[]{String.class}))
            .subscribe(outboundTransportObserver);

        final TransportObserver inboundTransportObserver = stream.inboundTransportObserver();
        Http2Headers headers = getHttp2Headers(methodName);
        inboundTransportObserver.onMetadata(new Http2HeaderMeta(headers), false); // 1
        byte[] data = getPackedData(stream, new Object[]{REQUEST_MSG}, new Class[]{String.class});
        inboundTransportObserver.onData(data, false); // 2
        inboundTransportObserver.onComplete(); // 3

        // Wait for the asynchronous operation to complete
        while (writeMethodCalledTimes.get() != 3) {
            Thread.sleep(50);
        }
        // NOTE: Send two header frames and one data frames. The previous test case has been verified, so I won’t go into details here.
        Mockito.verify(channel, Mockito.times(3)).write(Mockito.any(), Mockito.any());

        MockStreamObserver serverOutboundMessageSubscriber = (MockStreamObserver) ((IGreeterImpl) serviceImpl).getMockStreamObserver();
        serverOutboundMessageSubscriber.getLatch().await(1000, TimeUnit.MILLISECONDS);
        Assertions.assertEquals(serverOutboundMessageSubscriber.getOnNextData(), REQUEST_MSG);
        Assertions.assertTrue(serverOutboundMessageSubscriber.isOnCompleted());
    }

    private Http2Headers getHttp2Headers(String methodName) {
        Http2Headers headers = new DefaultHttp2Headers();
        headers.set(Http2Headers.PseudoHeaderName.PATH.value(), "/" + url.getPath() + "/" + methodName);
        headers.set(Http2Headers.PseudoHeaderName.METHOD.value(), HttpMethod.POST.asciiName());
        headers.set(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);
        headers.set(TripleHeaderEnum.GRPC_ENCODING.getHeader(), DEFAULT_COMPRESSOR);
        headers.set(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(), TripleHeaderEnum.CONTENT_PROTO.getHeader());
        headers.set(TripleHeaderEnum.TIMEOUT.getHeader(), timeout + "m");
        return headers;
    }

    private Http2StreamChannel getHttp2StreamChannel(AbstractServerStream stream) {
        ChannelFuture channelFuture = Mockito.mock(ChannelFuture.class);
        Http2StreamChannel streamChannel = Mockito.mock(Http2StreamChannel.class);
        Attribute<AbstractServerStream> attribute = Mockito.mock(Attribute.class);
        ChannelPromise promise = Mockito.mock(ChannelPromise.class);
        EventLoop eventLoop = new DefaultEventLoop();

        Mockito.when(attribute.get()).thenReturn(stream);
        Mockito.when(streamChannel.writeAndFlush(Mockito.any())).thenReturn(channelFuture);
        Mockito.when(streamChannel.alloc()).thenReturn(ByteBufAllocator.DEFAULT);
        Mockito.when(streamChannel.attr(TripleConstant.SERVER_STREAM_KEY)).thenReturn(attribute);
        Mockito.when(streamChannel.eventLoop()).thenReturn(eventLoop);
        Mockito.when(streamChannel.newPromise()).thenReturn(promise);

        Mockito.when(streamChannel.write(Mockito.any(), Mockito.any())).thenAnswer(
            (Answer<ChannelPromise>) invocationOnMock -> {
                writeMethodCalledTimes.incrementAndGet();
                return promise;
            });
        return streamChannel;
    }

    private byte[] getPackedData(AbstractServerStream stream, Object[] args, Class[] argsTypes) throws IOException {
        String serializationName = TripleConstant.HESSIAN2;
        final TripleWrapper.TripleRequestWrapper.Builder builder = TripleWrapper.TripleRequestWrapper.newBuilder()
            .setSerializeType(TripleConstant.HESSIAN4);
        MultipleSerialization serialization = stream.getMultipleSerialization();
        for (int i = 0; i < args.length; i++) {
            final String clz = argsTypes[i].getName();
            builder.addArgTypes(clz);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            serialization.serialize(url, serializationName, clz, args[i], bos);
            builder.addArgs(ByteString.copyFrom(bos.toByteArray()));
        }
        TripleWrapper.TripleRequestWrapper requestWrapper = builder.build();
        byte[] bytes = stream.pack(requestWrapper);
        return bytes;
    }
}
