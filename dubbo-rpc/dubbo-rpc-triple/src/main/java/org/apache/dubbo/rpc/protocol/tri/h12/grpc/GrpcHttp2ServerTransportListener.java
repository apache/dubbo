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
package org.apache.dubbo.rpc.protocol.tri.h12.grpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.UnimplementedException;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2ServerChannelObserver;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.remoting.http12.message.MethodMetadata;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.DescriptorUtils;
import org.apache.dubbo.rpc.protocol.tri.ReflectionPackableMethod;
import org.apache.dubbo.rpc.protocol.tri.RpcInvocationBuildContext;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.h12.HttpMessageListener;
import org.apache.dubbo.rpc.protocol.tri.h12.http2.GenericHttp2ServerTransportListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_PARSE;

public class GrpcHttp2ServerTransportListener extends GenericHttp2ServerTransportListener
        implements Http2TransportListener {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(GrpcHttp2ServerTransportListener.class);

    public GrpcHttp2ServerTransportListener(H2StreamChannel h2StreamChannel, URL url, FrameworkModel frameworkModel) {
        super(h2StreamChannel, url, frameworkModel);
    }

    @Override
    protected void onBeforeMetadata(Http2Header metadata) {}

    @Override
    protected Executor initializeExecutor(URL url, Http2Header metadata) {
        return getExecutor(url, metadata);
    }

    @Override
    protected void onPrepareMetadata(Http2Header metadata) {
        doRoute(metadata);
    }

    @Override
    protected void onMetadataCompletion(Http2Header metadata) {
        processGrpcHeaders(metadata);
        super.onMetadataCompletion(metadata);
    }

    private void processGrpcHeaders(Http2Header metadata) {
        String messageEncoding = metadata.header(GrpcHeaderNames.GRPC_ENCODING.getName());
        if (messageEncoding != null && !Identity.MESSAGE_ENCODING.equals(messageEncoding)) {
            DeCompressor compressor = DeCompressor.getCompressor(getFrameworkModel(), messageEncoding);
            if (compressor == null) {
                throw new UnimplementedException(
                        GrpcHeaderNames.GRPC_ENCODING.getName() + " '" + messageEncoding + "'");
            }
            ((GrpcStreamingDecoder) getStreamingDecoder()).setDeCompressor(compressor);
        }
    }

    @Override
    protected StreamingDecoder newStreamingDecoder() {
        return new GrpcStreamingDecoder();
    }

    @Override
    protected Http2ServerChannelObserver newResponseObserver(H2StreamChannel h2StreamChannel) {
        return new GrpcUnaryServerChannelObserver(getFrameworkModel(), h2StreamChannel);
    }

    @Override
    protected Http2ServerChannelObserver newStreamResponseObserver(H2StreamChannel h2StreamChannel) {
        return new GrpcStreamServerChannelObserver(getFrameworkModel(), h2StreamChannel);
    }

    @Override
    protected Http2ServerChannelObserver prepareResponseObserver(Http2ServerChannelObserver responseObserver) {
        responseObserver.addTrailersCustomizer(getExceptionCustomizerWrapper()::customizeGrpcStatus);
        return super.prepareResponseObserver(responseObserver);
    }

    @Override
    protected HttpMessageListener buildHttpMessageListener() {
        return getContext().isHasStub() ? super.buildHttpMessageListener() : new LazyFindMethodListener();
    }

    @Override
    protected void prepareUnaryServerCall() {
        if (needWrap()) {
            getExceptionCustomizerWrapper().setNeedWrap(true);
        }
    }

    private boolean needWrap() {
        RpcInvocationBuildContext context = getContext();
        if (context.isHasStub()) {
            return false;
        }
        MethodMetadata methodMetadata = context.getMethodMetadata();
        return ReflectionPackableMethod.needWrap(
                context.getMethodDescriptor(),
                methodMetadata.getActualRequestTypes(),
                methodMetadata.getActualResponseType());
    }

    @Override
    protected RpcInvocation onBuildRpcInvocationCompletion(RpcInvocation invocation) {
        String timeoutString = getHttpMetadata().header(GrpcHeaderNames.GRPC_TIMEOUT.getName());
        try {
            if (timeoutString != null) {
                Long timeout = GrpcUtils.parseTimeoutToMills(timeoutString);
                invocation.put(CommonConstants.TIMEOUT_KEY, timeout);
            }
        } catch (Throwable t) {
            LOGGER.warn(
                    PROTOCOL_FAILED_PARSE,
                    "",
                    "",
                    String.format(
                            "Failed to parse request timeout set from:%s, service=%s " + "method=%s",
                            timeoutString,
                            getContext().getServiceDescriptor().getInterfaceName(),
                            getContext().getMethodName()));
        }
        return invocation;
    }

    @Override
    protected Function<Throwable, Object> getExceptionCustomizer() {
        return getExceptionCustomizerWrapper()::customizeGrpc;
    }

    @Override
    protected void setMethodDescriptor(MethodDescriptor methodDescriptor) {
        ((GrpcCompositeCodec) getContext().getHttpMessageDecoder()).loadPackableMethod(methodDescriptor);
        super.setMethodDescriptor(methodDescriptor);
    }

    private class LazyFindMethodListener implements HttpMessageListener {

        private final StreamingDecoder streamingDecoder;

        private LazyFindMethodListener() {
            streamingDecoder = new GrpcStreamingDecoder();
            streamingDecoder.setFragmentListener(new DetermineMethodDescriptorListener());
            streamingDecoder.request(Integer.MAX_VALUE);
        }

        @Override
        public void onMessage(InputStream inputStream) {
            streamingDecoder.decode(inputStream);
        }
    }

    private class DetermineMethodDescriptorListener implements StreamingDecoder.FragmentListener {

        @Override
        public void onClose() {
            getStreamingDecoder().close();
        }

        @Override
        public void onFragmentMessage(InputStream rawMessage) {
            try {
                RpcInvocationBuildContext context = getContext();
                if (context.getMethodDescriptor() == null) {
                    MethodDescriptor methodDescriptor = DescriptorUtils.findTripleMethodDescriptor(
                            context.getServiceDescriptor(), context.getMethodName(), rawMessage);
                    setMethodDescriptor(methodDescriptor);
                    setHttpMessageListener(GrpcHttp2ServerTransportListener.super.buildHttpMessageListener());
                }

                ((GrpcStreamingDecoder) getStreamingDecoder()).invokeListener(rawMessage);
            } catch (IOException e) {
                throw new DecodeException(e);
            }
        }
    }
}
