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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.AbstractTransportObserver;
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.H2TransportObserver;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.WriteQueue;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.frame.TriDecoder;
import org.apache.dubbo.rpc.protocol.tri.pack.Pack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbPack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbUnpack;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.DebugInfo;
import com.google.rpc.Status;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.rpc.protocol.tri.GrpcStatus.getStatus;

public class ServerStream extends AbstractStream implements Stream {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerStream.class);

    public final ServerTransportObserver transportObserver = new ServerTransportObserver();
    private final ProviderModel providerModel;
    private final List<HeaderFilter> headerFilters;
    private final String methodName;
    private final DeCompressor deCompressor;
    private final Invoker<?> invoker;
    private final ServiceDescriptor serviceDescriptor;
    private final PbUnpack<?> unpack;
    private final MultipleSerialization serialization;
    private final WriteQueue writeQueue;
    private TriDecoder decoder;
    private MethodDescriptor methodDescriptor;
    private List<MethodDescriptor> methodDescriptors;
    private boolean headerSent;
    private boolean trailersSent;
    private Pack pack;
    private boolean closed;

    public ServerStream(URL url,
                        WriteQueue writeQueue,
                        Executor executor,
                        ServiceDescriptor serviceDescriptor,
                        ProviderModel providerModel,
                        List<HeaderFilter> headerFilters,
                        String methodName,
                        MethodDescriptor methodDescriptor,
                        Invoker<?> invoker,
                        List<MethodDescriptor> methodDescriptors,
                        DeCompressor deCompressor,
                        MultipleSerialization serialization) {
        super(url, executor);
        this.writeQueue = writeQueue;
        this.providerModel = providerModel;
        this.serviceDescriptor = serviceDescriptor;
        this.headerFilters = headerFilters;
        this.invoker = invoker;
        this.methodName = methodName;
        this.serialization = serialization;
        this.deCompressor = deCompressor;
        this.methodDescriptor = methodDescriptor;
        this.methodDescriptors = methodDescriptors;
        if (methodDescriptor == null || methodDescriptor.isNeedWrap()) {
            unpack = PbUnpack.REQ_PB_UNPACK;
        } else {
            unpack = new PbUnpack(methodDescriptor.getParameterClasses()[0]);
            pack = PbPack.INSTANCE;
        }
    }


    protected Long parseTimeoutToNanos(String timeoutVal) {
        if (StringUtils.isEmpty(timeoutVal) || StringUtils.isContains(timeoutVal, "null")) {
            return null;
        }
        long value = Long.parseLong(timeoutVal.substring(0, timeoutVal.length() - 1));
        char unit = timeoutVal.charAt(timeoutVal.length() - 1);
        switch (unit) {
            case 'n':
                return value;
            case 'u':
                return TimeUnit.MICROSECONDS.toNanos(value);
            case 'm':
                return TimeUnit.MILLISECONDS.toNanos(value);
            case 'S':
                return TimeUnit.SECONDS.toNanos(value);
            case 'M':
                return TimeUnit.MINUTES.toNanos(value);
            case 'H':
                return TimeUnit.HOURS.toNanos(value);
            default:
                // invalid timeout config
                return null;
        }
    }


    private String getGrpcMessage(GrpcStatus status) {
        if (StringUtils.isNotEmpty(status.description)) {
            return status.description;
        }
        if (status.cause != null) {
            return status.cause.getMessage();
        }
        return "unknown";
    }

    void sendHeader(Http2Headers headers) {
        if (headerSent && trailersSent) {
            // todo handle this state
            return;
        }
        if (!headerSent) {
            headerSent = true;
            writeQueue.enqueue(HeaderQueueCommand.createHeaders(headers, false), true);
        } else {
            trailersSent = true;
            writeQueue.enqueue(HeaderQueueCommand.createHeaders(headers, true), true);
        }
    }

    void sendMessage(Object message) {
        final byte[] data;
        try {
            data = pack.pack(message);
        } catch (IOException e) {
            close(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withDescription("Serialize response failed")
                .withCause(e), null);
            return;
        }
        if (data == null) {
            close(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withDescription("Missing response"), null);
            return;
        }
        writeQueue.enqueue(DataQueueCommand.createGrpcCommand(data, false), true);
    }

    public void close(GrpcStatus status, Http2Headers trailers) {
        if (closed) {
            return;
        }
        closed = true;
        if (headerSent && trailersSent) {
            // already closed
            // todo add sign for outbound status
            return;
        }
        final Http2Headers headers = getTrailers(status, headerSent);
        sendHeader(headers);
    }

    private Http2Headers getTrailers(GrpcStatus grpcStatus, boolean headerSent) {
        Http2Headers headers = new DefaultHttp2Headers();
        if (!headerSent) {
            headers.status(HttpResponseStatus.OK.codeAsText());
            headers.set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
        }
        String grpcMessage = getGrpcMessage(grpcStatus);
        grpcMessage = GrpcStatus.encodeMessage(grpcMessage);
        headers.set(TripleHeaderEnum.MESSAGE_KEY.getHeader(), grpcMessage);
        headers.set(TripleHeaderEnum.STATUS_KEY.getHeader(), String.valueOf(grpcStatus.code.code));
        Status.Builder builder = Status.newBuilder()
            .setCode(grpcStatus.code.code)
            .setMessage(grpcMessage);
        Throwable throwable = grpcStatus.cause;
        if (throwable == null) {
            Status status = builder.build();
            headers.set(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader(),
                H2TransportObserver.encodeBase64ASCII(status.toByteArray()));
            return headers;
        }
        DebugInfo debugInfo = DebugInfo.newBuilder()
            .addAllStackEntries(ExceptionUtils.getStackFrameList(throwable, 10))
            // can not use now
            // .setDetail(throwable.getMessage())
            .build();
        builder.addDetails(Any.pack(debugInfo));
        Status status = builder.build();
        headers.set(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader(),
            H2TransportObserver.encodeBase64ASCII(status.toByteArray()));
        return headers;
    }

    @Override
    public void writeMessage(byte[] message) {

    }

    public class ServerTransportObserver extends AbstractTransportObserver implements H2TransportObserver {

        @Override
        public void onHeader(Http2Headers headers, boolean endStream) {
            try {
                final TriDecoder.Listener listener = data -> {
                    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                    try {
                        trySetMethodDescriptor(data);
                        if (closed) {
                            return;
                        }
                        final RpcInvocation inv = buildInvocation(headers);
                        if (closed) {
                            return;
                        }
                        headerFilters.forEach(f -> f.invoke(invoker, inv));
                        if (closed) {
                            return;
                        }
                        if (providerModel != null) {
                            ClassLoadUtil.switchContextLoader(providerModel.getServiceInterfaceClass().getClassLoader());
                        }

                        final Object unpack = ServerStream.this.unpack.unpack(data);
                        if (unpack instanceof Object[]) {
                            inv.setArguments((Object[]) unpack);
                        } else {
                            inv.setArguments(new Object[]{unpack});
                        }
                        if (pack == null) {
                            pack = PbPack.INSTANCE;
                        }
                        invoke(inv);
                    } catch (IOException e) {
                        close(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL).withDescription("Server error")
                            .withCause(e), null);
                    } finally {
                        ClassLoadUtil.switchContextLoader(tccl);
                    }
                };
                decoder = new TriDecoder(deCompressor, listener);
            } catch (Throwable t) {
                close(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withCause(t), null);
            }
        }

        void trySetMethodDescriptor(byte[] data) throws InvalidProtocolBufferException {
            if (methodDescriptor != null) {
                return;
            }
            final TripleWrapper.TripleRequestWrapper request;
            request = TripleWrapper.TripleRequestWrapper.parseFrom(data);

            final String[] paramTypes = request.getArgTypesList().toArray(new String[request.getArgsCount()]);
            // wrapper mode the method can overload so maybe list
            for (MethodDescriptor descriptor : methodDescriptors) {
                // params type is array
                if (Arrays.equals(descriptor.getCompatibleParamSignatures(), paramTypes)) {
                    methodDescriptor = descriptor;
                    break;
                }
            }
            if (methodDescriptor == null) {
                close(GrpcStatus.fromCode(GrpcStatus.Code.UNIMPLEMENTED)
                    .withDescription("Method :" + methodName + "[" + Arrays.toString(paramTypes) + "] " +
                        "not found of service:" + serviceDescriptor.getServiceName()), null);
            }
        }

        @Override
        public void onData(ByteBuf data, boolean endStream) {
            decoder.deframe(data);
            if (endStream) {
                decoder.close();
            }
        }


        @Override
        public void cancelByRemote(GrpcStatus status) {
            close(status, null);
        }

        /**
         * Build the RpcInvocation with metadata and execute headerFilter
         *
         * @param headers request header
         * @return RpcInvocation
         */
        protected RpcInvocation buildInvocation(Http2Headers headers) {
            RpcInvocation inv = new RpcInvocation(url().getServiceModel(),
                methodName, serviceDescriptor.getServiceName(),
                url().getProtocolServiceKey(), methodDescriptor.getParameterClasses(), new Object[0]);
            inv.setTargetServiceUniqueName(url().getServiceKey());
            inv.setReturnTypes(methodDescriptor.getReturnTypes());

            final Map<String, Object> attachments = headersToMap(headers);
            inv.setObjectAttachments(attachments);
            // handle timeout
            CharSequence timeout = headers.get(TripleHeaderEnum.TIMEOUT.getHeader());
            try {
                if (!Objects.isNull(timeout)) {
                    final Long timeoutInNanos = parseTimeoutToNanos(timeout.toString());
                    if (!Objects.isNull(timeoutInNanos)) {
                        inv.setAttachment(TIMEOUT_KEY, timeoutInNanos);
                    }
                }
            } catch (Throwable t) {
                LOGGER.warn(String.format("Failed to parse request timeout set from:%s, service=%s method=%s",
                    timeout, serviceDescriptor.getServiceName(), methodName));
            }
            return inv;
        }

        public void invoke(RpcInvocation invocation) {
            final long stInNano = System.nanoTime();
            final Result result = invoker.invoke(invocation);
            CompletionStage<Object> future = result.thenApply(Function.identity());
            future.whenComplete((o, throwable) -> {
                if (throwable != null) {
                    LOGGER.error("Invoke error", throwable);
                    close(getStatus(throwable), null);
                    return;
                }
                AppResponse response = (AppResponse) o;
                if (response.hasException()) {
                    close(getStatus(response.getException()), null);
                    return;
                }
                final Object timeoutVal = invocation.getObjectAttachment(TIMEOUT_KEY);
                final long cost = System.nanoTime() - stInNano;
                if (timeoutVal != null && cost > ((Long) timeoutVal)) {
                    LOGGER.error(String.format("Invoke timeout at server side, ignored to send response. service=%s method=%s cost=%s timeout=%s",
                        invocation.getTargetServiceUniqueName(),
                        invocation.getMethodName(),
                        cost, timeoutVal));
                    close(GrpcStatus.fromCode(GrpcStatus.Code.DEADLINE_EXCEEDED), null);
                } else {
                    Http2Headers metadata = TripleConstant.createSuccessHttp2Headers();
                    // todo add encoding
                    sendHeader(metadata);
                    sendMessage(response.getValue());
                    DefaultHttp2Headers trailers = TripleConstant.createSuccessHttp2Trailers();
                    StreamUtils.convertAttachment(trailers, response.getObjectAttachments());
                    sendHeader(trailers);
                }
            });
            RpcContext.removeContext();
        }
    }


}
