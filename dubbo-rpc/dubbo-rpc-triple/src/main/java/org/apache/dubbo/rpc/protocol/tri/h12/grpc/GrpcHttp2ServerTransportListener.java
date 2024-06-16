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
import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.UnimplementedException;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2ServerChannelObserver;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.remoting.http12.message.MethodMetadata;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.DescriptorUtils;
import org.apache.dubbo.rpc.protocol.tri.RpcInvocationBuildContext;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.h12.HttpMessageListener;
import org.apache.dubbo.rpc.protocol.tri.h12.http2.GenericHttp2ServerTransportListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_PARSE;

public class GrpcHttp2ServerTransportListener extends GenericHttp2ServerTransportListener
        implements Http2TransportListener {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(GrpcHttp2ServerTransportListener.class);

    public GrpcHttp2ServerTransportListener(H2StreamChannel h2StreamChannel, URL url, FrameworkModel frameworkModel) {
        super(h2StreamChannel, url, frameworkModel);
        getServerChannelObserver().setTrailersCustomizer(this::grpcTrailersCustomize);
    }

    private void grpcTrailersCustomize(HttpHeaders httpHeaders, Throwable throwable) {
        if (throwable == null) {
            httpHeaders.set(GrpcHeaderNames.GRPC_STATUS.getName(), "0");
        } else {
            httpHeaders.set(GrpcHeaderNames.GRPC_STATUS.getName(), httpStatusToGrpcStatus(throwable));
            httpHeaders.set(GrpcHeaderNames.GRPC_MESSAGE.getName(), throwable.getMessage());
        }
    }

    private static String httpStatusToGrpcStatus(Throwable throwable) {
        // http status code map to grpc status code
        return String.valueOf(TriRpcStatus.INTERNAL.code.code);
    }

    @Override
    protected StreamingDecoder newStreamingDecoder() {
        return new GrpcStreamingDecoder();
    }

    @Override
    protected Http2ServerChannelObserver newHttp2ServerChannelObserver(
            FrameworkModel frameworkModel, H2StreamChannel h2StreamChannel) {
        return new GrpcServerChannelObserver(frameworkModel, h2StreamChannel);
    }

    @Override
    protected HttpMessageListener buildHttpMessageListener() {
        return getContext().isHasStub() ? super.buildHttpMessageListener() : new LazyFindMethodListener();
    }

    @Override
    protected void onUnary() {}

    @Override
    protected void onMetadataCompletion(Http2Header metadata) {
        processGrpcHeaders(metadata);
        super.onMetadataCompletion(metadata);
    }

    private void processGrpcHeaders(Http2Header metadata) {
        String messageEncoding = metadata.headers().getFirst(GrpcHeaderNames.GRPC_ENCODING.getName());
        if (null != messageEncoding) {
            if (!Identity.MESSAGE_ENCODING.equals(messageEncoding)) {
                DeCompressor compressor = DeCompressor.getCompressor(getFrameworkModel(), messageEncoding);
                if (null == compressor) {
                    throw new UnimplementedException(
                            GrpcHeaderNames.GRPC_ENCODING.getName() + " '" + messageEncoding + "'");
                }
                getStreamingDecoder().setDeCompressor(compressor);
            }
        }
    }

    @Override
    protected RpcInvocation onBuildRpcInvocationCompletion(RpcInvocation invocation) {
        String timeoutString = getHttpMetadata().headers().getFirst(GrpcHeaderNames.GRPC_TIMEOUT.getName());
        try {
            if (null != timeoutString) {
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
    protected GrpcStreamingDecoder getStreamingDecoder() {
        return (GrpcStreamingDecoder) super.getStreamingDecoder();
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
        public void onFragmentMessage(InputStream rawMessage) {}

        @Override
        public void onClose() {
            getStreamingDecoder().close();
        }

        @Override
        public void onFragmentMessage(InputStream dataHeader, InputStream rawMessage) {
            try {
                ByteArrayOutputStream merged =
                        new ByteArrayOutputStream(dataHeader.available() + rawMessage.available());
                StreamUtils.copy(dataHeader, merged);
                byte[] data = StreamUtils.readBytes(rawMessage);

                RpcInvocationBuildContext context = getContext();
                if (null == context.getMethodDescriptor()) {
                    context.setMethodDescriptor(DescriptorUtils.findTripleMethodDescriptor(
                            context.getServiceDescriptor(), context.getMethodName(), data));

                    setHttpMessageListener(GrpcHttp2ServerTransportListener.super.buildHttpMessageListener());

                    // replace decoder
                    GrpcCompositeCodec grpcCompositeCodec = (GrpcCompositeCodec) context.getHttpMessageDecoder();
                    MethodMetadata methodMetadata = context.getMethodMetadata();
                    grpcCompositeCodec.setDecodeTypes(methodMetadata.getActualRequestTypes());
                    grpcCompositeCodec.setEncodeTypes(new Class[] {methodMetadata.getActualResponseType()});
                    getServerChannelObserver().setResponseEncoder(grpcCompositeCodec);
                }

                merged.write(data);
                getHttpMessageListener().onMessage(new ByteArrayInputStream(merged.toByteArray()));
            } catch (IOException e) {
                throw new DecodeException(e);
            }
        }
    }
}
