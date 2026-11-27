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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.h2.CancelStreamException;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessageFrame;
import org.apache.dubbo.remoting.http12.h2.Http2ServerChannelObserver;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.remoting.http12.message.DefaultListeningDecoder;
import org.apache.dubbo.remoting.http12.message.DefaultStreamingDecoder;
import org.apache.dubbo.remoting.http12.message.ListeningDecoder;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder.FragmentListener;
import org.apache.dubbo.remoting.http12.message.codec.JsonCodec;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.Http3Exchanger;
import org.apache.dubbo.rpc.protocol.tri.RpcInvocationBuildContext;
import org.apache.dubbo.rpc.protocol.tri.h12.AbstractServerTransportListener;
import org.apache.dubbo.rpc.protocol.tri.h12.BiStreamServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.HttpMessageListener;
import org.apache.dubbo.rpc.protocol.tri.h12.ServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.ServerStreamServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.UnaryServerCallListener;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_RESPONSE;

/**
 * Tracks DATA ownership across executor submission, including silently discarded tasks.
 *
 * <p>Each input moves from QUEUED to DELIVERING when its task starts, then to HANDED_OFF
 * before invoking the message listener. Transport termination may close only QUEUED inputs;
 * a started callback closes its own DELIVERING input in finally if handoff did not occur.
 * After handoff, buffered input belongs to the decoder, including while later callbacks read it.
 * Every close path claims CLOSED before closing the original message, so release occurs once.
 *
 * <p>The registry finds tasks that never run. The volatile terminal flag seals new registrations;
 * checking it after registration also covers DATA racing with the terminal drain. Per-input CAS
 * separates queued cleanup from an unrelated delivering callback, without a shared lifecycle lock.
 * A callback that passed its terminal check may race with closure before handoff. Handoff retains
 * the existing decoder lifecycle; it does not add synchronization with decoder termination.
 */
public class GenericHttp2ServerTransportListener extends AbstractServerTransportListener<Http2Header, Http2InputMessage>
        implements Http2TransportListener {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(GenericHttp2ServerTransportListener.class);

    private static final int QUEUED = 0;
    private static final int DELIVERING = 1;
    private static final int HANDED_OFF = 2;
    private static final int CLOSED = 3;

    private final H2StreamChannel h2StreamChannel;
    private final StreamingDecoder streamingDecoder;
    // Multiple HTTP/2 streams are initialized on one event-loop thread. Cancellation belongs
    // to the request, including when its response observer changes from unary to streaming.
    private final CancellationContext cancellationContext = new CancellationContext();
    private Http2ServerChannelObserver responseObserver;
    private ServerCallListener serverCallListener;

    private final Set<PendingInput> pendingInputs = ConcurrentHashMap.newKeySet();
    private volatile boolean inputClosed;

    public GenericHttp2ServerTransportListener(
            H2StreamChannel h2StreamChannel, URL url, FrameworkModel frameworkModel) {
        super(frameworkModel, url, h2StreamChannel);
        this.h2StreamChannel = h2StreamChannel;
        streamingDecoder = newStreamingDecoder();
        responseObserver = prepareResponseObserver(newResponseObserver(h2StreamChannel));
    }

    protected StreamingDecoder newStreamingDecoder() {
        return new DefaultStreamingDecoder();
    }

    protected Http2ServerChannelObserver newResponseObserver(H2StreamChannel h2StreamChannel) {
        return new Http2UnaryServerChannelObserver(getFrameworkModel(), h2StreamChannel);
    }

    protected Http2ServerChannelObserver newStreamResponseObserver(H2StreamChannel h2StreamChannel) {
        Http2ServerChannelObserver responseObserver =
                new Http2SseServerChannelObserver(getFrameworkModel(), h2StreamChannel);
        responseObserver.addHeadersCustomizer(
                (hs, t) -> hs.set(HttpHeaderNames.CONTENT_TYPE.getKey(), MediaType.TEXT_EVENT_STREAM.getName()));
        return responseObserver;
    }

    protected Http2ServerChannelObserver prepareResponseObserver(Http2ServerChannelObserver responseObserver) {
        responseObserver.setExceptionCustomizer(getExceptionCustomizer());
        RpcInvocationBuildContext context = getContext();
        responseObserver.setResponseEncoder(context == null ? JsonCodec.INSTANCE : context.getHttpMessageEncoder());
        responseObserver.setCancellationContext(cancellationContext);
        responseObserver.setStreamingDecoder(streamingDecoder);
        return responseObserver;
    }

    @Override
    protected HttpMessageListener buildHttpMessageListener() {
        RpcInvocationBuildContext context = getContext();
        RpcInvocation rpcInvocation = buildRpcInvocation(context);

        serverCallListener = startListener(rpcInvocation, context.getMethodDescriptor(), context.getInvoker());
        DefaultListeningDecoder listeningDecoder = new DefaultListeningDecoder(
                context.getHttpMessageDecoder(), context.getMethodMetadata().getActualRequestTypes());
        listeningDecoder.setListener(new Http2StreamingDecodeListener(serverCallListener));
        streamingDecoder.setFragmentListener(new DefaultFragmentListener(listeningDecoder));
        return new StreamingHttpMessageListener(streamingDecoder);
    }

    final class DefaultFragmentListener implements FragmentListener {

        private final ListeningDecoder listeningDecoder;

        public DefaultFragmentListener(ListeningDecoder listeningDecoder) {
            this.listeningDecoder = listeningDecoder;
        }

        @Override
        public void bytesRead(int numBytes) {
            try {
                getH2StreamChannel().consumeBytes(numBytes);
            } catch (Exception e) {
                LOGGER.warn(PROTOCOL_FAILED_RESPONSE, "", "", "Failed to consume bytes for flow control", e);
            }
        }

        @Override
        public void onFragmentMessage(InputStream rawMessage, int messageLength) {
            listeningDecoder.decode(rawMessage);
        }

        @Override
        public void onClose() {
            listeningDecoder.close();
        }
    }

    /**
     * Get the H2StreamChannel for flow control.
     */
    protected H2StreamChannel getH2StreamChannel() {
        return h2StreamChannel;
    }

    private ServerCallListener startListener(
            RpcInvocation invocation, MethodDescriptor methodDescriptor, Invoker<?> invoker) {
        switch (methodDescriptor.getRpcType()) {
            case UNARY:
                prepareUnaryServerCall();
                return new UnaryServerCallListener(invocation, invoker, responseObserver);
            case SERVER_STREAM:
                prepareStreamServerCall();
                return new ServerStreamServerCallListener(invocation, invoker, responseObserver);
            case BI_STREAM:
            case CLIENT_STREAM:
                prepareStreamServerCall();
                return new BiStreamServerCallListener(invocation, invoker, responseObserver);
            default:
                throw new IllegalStateException("Can not reach here");
        }
    }

    protected void prepareUnaryServerCall() {}

    protected void prepareStreamServerCall() {
        responseObserver = prepareResponseObserver(newStreamResponseObserver(h2StreamChannel));
    }

    @Override
    protected void initializeAltSvc(URL url) {
        if (Http3Exchanger.isEnabled(url)) {
            String value = "h3=\":" + url.getParameter(Constants.BIND_PORT_KEY, url.getPort()) + '"';
            responseObserver.addHeadersCustomizer((hs, t) -> hs.set(HttpHeaderNames.ALT_SVC.getKey(), value));
        }
    }

    @Override
    protected void onMetadataCompletion(Http2Header metadata) {
        responseObserver.setResponseEncoder(getContext().getHttpMessageEncoder());
        responseObserver.setExecutor(getExecutor());
        responseObserver.request(1);
        if (metadata.isEndStream()) {
            getStreamingDecoder().close();
        }
    }

    @Override
    protected Http2InputMessage onDataReceived(Http2InputMessage message) {
        PendingInput input = newPendingInput(message);
        pendingInputs.add(input);
        if (inputClosed) {
            closeInput(input, QUEUED);
        }
        return new Http2InputMessageFrame(message.id(), input, message.isEndStream());
    }

    protected PendingInput newPendingInput(Http2InputMessage message) {
        return new PendingInput(message);
    }

    @Override
    protected void doOnData(Http2InputMessage message) {
        PendingInput input = (PendingInput) message.getBody();
        if (!input.state.compareAndSet(QUEUED, DELIVERING)) {
            return;
        }
        try {
            if (!inputClosed) {
                super.doOnData(message);
            }
        } finally {
            closeInput(input, DELIVERING);
        }
    }

    @Override
    protected void onDataMessage(Http2InputMessage message, HttpMessageListener listener) {
        PendingInput input = (PendingInput) message.getBody();
        if (inputClosed || !input.state.compareAndSet(DELIVERING, HANDED_OFF)) {
            return;
        }
        pendingInputs.remove(input);
        super.onDataMessage(message, listener);
    }

    @Override
    protected void onDataCompletion(Http2InputMessage message) {
        if (message.isEndStream()) {
            getStreamingDecoder().close();
        }
    }

    @Override
    protected void onError(Throwable throwable) {
        responseObserver.onError(throwable);
    }

    @Override
    protected void onError(Http2InputMessage message, Throwable throwable) {
        try {
            message.close();
        } catch (Exception e) {
            throwable.addSuppressed(e);
        }
        onError(throwable);
    }

    @Override
    protected void onDataFinally(Http2InputMessage message) {}

    @Override
    public void cancelByRemote(long errorCode) {
        closeInputs();
        responseObserver.cancel(CancelStreamException.fromRemote(errorCode));
        if (serverCallListener != null) {
            serverCallListener.onCancel(errorCode);
        }
    }

    protected final StreamingDecoder getStreamingDecoder() {
        return streamingDecoder;
    }

    protected final Http2ServerChannelObserver getResponseObserver() {
        return responseObserver;
    }

    @Override
    public void close() {
        closeInputs();
        responseObserver.close();
    }

    private void closeInputs() {
        inputClosed = true;
        for (PendingInput input : pendingInputs) {
            closeInput(input, QUEUED);
        }
    }

    private void closeInput(PendingInput input, int expectedState) {
        try {
            input.close(expectedState);
        } catch (Exception e) {
            LOGGER.warn(PROTOCOL_FAILED_RESPONSE, "", "", "Failed to close request body", e);
        }
    }

    protected class PendingInput extends FilterInputStream {

        private final AtomicInteger state = new AtomicInteger(QUEUED);
        private final Http2InputMessage message;

        protected PendingInput(Http2InputMessage message) {
            super(message.getBody());
            this.message = message;
        }

        @Override
        public void close() throws IOException {
            if (state.getAndSet(CLOSED) != CLOSED) {
                closeMessage();
            }
        }

        private void close(int expectedState) throws IOException {
            if (state.compareAndSet(expectedState, CLOSED)) {
                closeMessage();
            }
        }

        private void closeMessage() throws IOException {
            pendingInputs.remove(this);
            message.close();
        }
    }

    @Override
    public void onWritabilityChanged() {
        responseObserver.onWritabilityChanged();
    }

    private static final class Http2StreamingDecodeListener implements ListeningDecoder.Listener {

        private final ServerCallListener serverCallListener;

        Http2StreamingDecodeListener(ServerCallListener serverCallListener) {
            this.serverCallListener = serverCallListener;
        }

        @Override
        public void onMessage(Object message) {
            serverCallListener.onMessage(message);
        }

        @Override
        public void onClose() {
            serverCallListener.onComplete();
        }
    }

    private static final class StreamingHttpMessageListener implements HttpMessageListener {

        private final StreamingDecoder streamingDecoder;

        StreamingHttpMessageListener(StreamingDecoder streamingDecoder) {
            this.streamingDecoder = streamingDecoder;
        }

        @Override
        public void onMessage(InputStream inputStream) {
            streamingDecoder.decode(inputStream);
        }
    }
}
