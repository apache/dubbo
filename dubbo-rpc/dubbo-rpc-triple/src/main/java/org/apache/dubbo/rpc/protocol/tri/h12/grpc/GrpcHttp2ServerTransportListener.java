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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.UnimplementedException;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.remoting.http12.message.MethodMetadata;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.TripleCustomerProtocolWapper;
import org.apache.dubbo.rpc.protocol.tri.call.AbstractServerCall;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.h12.HttpMessageListener;
import org.apache.dubbo.rpc.protocol.tri.h12.http2.GenericHttp2ServerTransportListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_PARSE;

public class GrpcHttp2ServerTransportListener extends GenericHttp2ServerTransportListener
        implements Http2TransportListener {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(AbstractServerCall.class);

    public GrpcHttp2ServerTransportListener(H2StreamChannel h2StreamChannel, URL url, FrameworkModel frameworkModel) {
        super(h2StreamChannel, url, frameworkModel);
        initialize();
    }

    private void initialize() {
        getServerChannelObserver().setTrailersCustomizer(this::grpcTrailersCustomize);
    }

    private void grpcTrailersCustomize(HttpHeaders httpHeaders, Throwable throwable) {
        httpHeaders.set(GrpcHeaderNames.GRPC_STATUS.getName(), "0");
        if (throwable != null) {
            httpHeaders.set(GrpcHeaderNames.GRPC_STATUS.getName(), httpStatusToGrpcStatus(throwable));
            httpHeaders.set(GrpcHeaderNames.GRPC_MESSAGE.getName(), throwable.getMessage());
        }
    }

    private static String httpStatusToGrpcStatus(Throwable throwable) {
        // http status code map to grpc status code
        return String.valueOf(TriRpcStatus.INTERNAL.code.code);
    }

    @Override
    protected RpcInvocation buildRpcInvocation(
            Invoker<?> invoker, ServiceDescriptor serviceDescriptor, MethodDescriptor methodDescriptor) {
        RpcInvocation rpcInvocation = super.buildRpcInvocation(invoker, serviceDescriptor, methodDescriptor);
        HttpHeaders headers = getHttpMetadata().headers();
        String timeoutString = headers.getFirst(GrpcHeaderNames.GRPC_TIMEOUT.getName());
        try {
            if (Objects.nonNull(timeoutString)) {
                Long timeout = GrpcUtils.parseTimeoutToMills(timeoutString);
                rpcInvocation.put("timeout", timeout);
            }
        } catch (Throwable t) {
            LOGGER.warn(
                    PROTOCOL_FAILED_PARSE,
                    "",
                    "",
                    String.format(
                            "Failed to parse request timeout set from:%s, service=%s " + "method=%s",
                            timeoutString,
                            serviceDescriptor.getInterfaceName(),
                            getMethodDescriptor().getMethodName()));
        }
        return rpcInvocation;
    }

    @Override
    protected StreamingDecoder newStreamingDecoder() {
        return new GrpcStreamingDecoder();
    }

    @Override
    protected HttpMessageListener newHttpMessageListener() {
        Http2Header httpMetadata = getHttpMetadata();
        boolean hasStub = getPathResolver().hasNativeStub(httpMetadata.path());
        if (hasStub) {
            return GrpcHttp2ServerTransportListener.super.newHttpMessageListener();
        }
        return new LazyFindMethodListener();
    }

    @Override
    protected void onMetadataCompletion(Http2Header metadata) {
        super.onMetadataCompletion(metadata);
        processGrpcHeaders(metadata);
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
    protected GrpcStreamingDecoder getStreamingDecoder() {
        return (GrpcStreamingDecoder) super.getStreamingDecoder();
    }

    private class LazyFindMethodListener implements HttpMessageListener {

        private final StreamingDecoder streamingDecoder;

        private LazyFindMethodListener() {
            this.streamingDecoder = new GrpcStreamingDecoder();
            this.streamingDecoder.setFragmentListener(new DetermineMethodDescriptorListener());
            this.streamingDecoder.request(Integer.MAX_VALUE);
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
                ByteArrayOutputStream merge =
                        new ByteArrayOutputStream(dataHeader.available() + rawMessage.available());
                transferToOutputStream(merge, dataHeader);
                ByteArrayOutputStream bos = new ByteArrayOutputStream(rawMessage.available());
                transferToOutputStream(bos, rawMessage);
                byte[] data = bos.toByteArray();
                MethodDescriptor methodDescriptor = getMethodDescriptor();
                if (methodDescriptor == null) {
                    Http2Header httpMetadata = getHttpMetadata();
                    String path = httpMetadata.path();
                    String[] parts = path.split("/");
                    String originalMethodName = parts[2];
                    methodDescriptor = findReflectionMethodDescriptor(getServiceDescriptor(), originalMethodName);
                    if (methodDescriptor == null) {
                        List<MethodDescriptor> methodDescriptors =
                                getServiceDescriptor().getMethods(originalMethodName);
                        final TripleCustomerProtocolWapper.TripleRequestWrapper request;
                        request = TripleCustomerProtocolWapper.TripleRequestWrapper.parseFrom(data);
                        final String[] paramTypes = request.getArgTypes()
                                .toArray(new String[request.getArgs().size()]);
                        // wrapper mode the method can overload so maybe list
                        for (MethodDescriptor descriptor : methodDescriptors) {
                            // params type is array
                            if (Arrays.equals(descriptor.getCompatibleParamSignatures(), paramTypes)) {
                                methodDescriptor = descriptor;
                                break;
                            }
                        }
                        if (methodDescriptor == null) {
                            throw new UnimplementedException("method:" + originalMethodName);
                        }
                    }
                    setMethodDescriptor(methodDescriptor);
                    setMethodMetadata(MethodMetadata.fromMethodDescriptor(methodDescriptor));
                    setRpcInvocation(buildRpcInvocation(getInvoker(), getServiceDescriptor(), methodDescriptor));
                    // replace decoder
                    HttpMessageListener httpMessageListener =
                            GrpcHttp2ServerTransportListener.super.newHttpMessageListener();
                    GrpcCompositeCodec grpcCompositeCodec = (GrpcCompositeCodec) getHttpMessageCodec();grpcCompositeCodec.setDecodeTypes(getMethodMetadata().getActualRequestTypes());
                    ((GrpcCompositeCodec) GrpcHttp2ServerTransportListener.super
                                    .getServerChannelObserver()
                                    .getResponseEncoder())
                            .setEncodeTypes(new Class[] {getMethodMetadata().getActualResponseType()});
                    setHttpMessageListener(httpMessageListener);
                }
                transferToOutputStream(merge, new ByteArrayInputStream(data));
                getHttpMessageListener().onMessage(new ByteArrayInputStream(merge.toByteArray()));
            } catch (IOException e) {
                throw new DecodeException(e);
            }
        }

        private void transferToOutputStream(OutputStream out, InputStream inputStream) throws IOException {
            byte[] bytes = new byte[1024];
            int len;
            while ((len = inputStream.read(bytes)) != -1) {
                out.write(bytes, 0, len);
            }
        }
    }
}
