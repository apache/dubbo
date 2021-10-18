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
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.threadpool.serial.SerializingExecutor;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus.Code;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.Status;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

public abstract class AbstractStream implements Stream {
    protected static final String DUPLICATED_DATA = "Duplicated data";

    private final URL url;
    private final MultipleSerialization multipleSerialization;
    private final StreamObserver<Object> streamObserver;
    private final TransportObserver transportObserver;
    private final Executor executor;
    private final CancellationContext cancellationContext;
    private ServiceDescriptor serviceDescriptor;
    private MethodDescriptor methodDescriptor;
    private String methodName;
    private Request request;
    private String serializeType;
    private StreamObserver<Object> streamSubscriber;
    private TransportObserver transportSubscriber;
    private Compressor compressor = IdentityCompressor.NONE;
    private Compressor deCompressor = IdentityCompressor.NONE;
    private volatile boolean cancelled = false;

    protected AbstractStream(URL url) {
        this(url, null);
    }

    protected AbstractStream(URL url, Executor executor) {
        this.url = url;
        final Executor sourceExecutor = lookupExecutor(url, executor);
        this.executor = wrapperSerializingExecutor(sourceExecutor);
        final String value = url.getParameter(Constants.MULTI_SERIALIZATION_KEY, CommonConstants.DEFAULT_KEY);
        this.multipleSerialization = url.getOrDefaultFrameworkModel().getExtensionLoader(MultipleSerialization.class)
            .getExtension(value);
        this.cancellationContext = new CancellationContext();
        this.transportObserver = createTransportObserver();
        this.streamObserver = createStreamObserver();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    protected CancellationContext getCancellationContext() {
        return cancellationContext;
    }

    private Executor lookupExecutor(URL url, Executor executor) {
        // only server maybe not null
        if (executor != null) {
            return executor;
        }
        ExecutorRepository executorRepository = url.getOrDefaultApplicationModel()
            .getExtensionLoader(ExecutorRepository.class)
            .getDefaultExtension();
        Executor urlExecutor = executorRepository.getExecutor(url);
        if (urlExecutor == null) {
            urlExecutor = executorRepository.createExecutorIfAbsent(url);
        }
        return urlExecutor;
    }

    private Executor wrapperSerializingExecutor(Executor executor) {
        return new SerializingExecutor(executor);
    }

    public Request getRequest() {
        return request;
    }

    public AbstractStream request(Request request) {
        this.request = request;
        return this;
    }

    @Override
    public void execute(Runnable runnable) {
        executor.execute(runnable);
    }

    public String getMethodName() {
        return methodName;
    }

    public AbstractStream methodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public AbstractStream method(MethodDescriptor md) {
        this.methodDescriptor = md;
        return this;
    }

    /**
     * local cancel
     *
     * @param cause cancel case
     */
    protected final void cancel(Throwable cause) {
        cancel();
        cancelByLocal(cause);
    }

    private void cancel() {
        cancelled = true;
        execute(RpcContext::removeCancellationContext);
    }

    /**
     * remote cancel
     *
     * @param http2Error {@link Http2Error}
     */
    protected final void cancelByRemote(Http2Error http2Error) {
        cancel();
        cancelByRemoteReset(http2Error);
    }

    protected abstract void cancelByRemoteReset(Http2Error http2Error);

    protected abstract void cancelByLocal(Throwable throwable);

    protected abstract StreamObserver<Object> createStreamObserver();

    protected abstract TransportObserver createTransportObserver();

    public String getSerializeType() {
        return serializeType;
    }

    public AbstractStream serialize(String serializeType) {
        if ("hessian4".equals(serializeType)) {
            serializeType = "hessian2";
        }
        this.serializeType = serializeType;
        return this;
    }

    public MultipleSerialization getMultipleSerialization() {
        return multipleSerialization;
    }

    public StreamObserver<Object> getStreamSubscriber() {
        return streamSubscriber;
    }

    public TransportObserver getTransportSubscriber() {
        return transportSubscriber;
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    public ServiceDescriptor getServiceDescriptor() {
        return serviceDescriptor;
    }

    public void setServiceDescriptor(ServiceDescriptor serviceDescriptor) {
        this.serviceDescriptor = serviceDescriptor;
    }

    public Compressor getCompressor() {
        return this.compressor;
    }

    /**
     * set compressor if required
     *
     * @param compressor {@link Compressor}
     */
    protected AbstractStream setCompressor(Compressor compressor) {
        // If compressor is NULL, this will not be set.
        // Consider whether to throw an exception or handle silently,
        // But now choose silent processing, Fall back to default.
        if (compressor != null) {
            this.compressor = compressor;
        } else {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Compressor is Null, Fall back to default compression." +
                    " MessageEncoding is " + getCompressor().getMessageEncoding());
            }
        }
        return this;
    }

    public Compressor getDeCompressor() {
        return this.deCompressor;
    }

    protected AbstractStream setDeCompressor(Compressor compressor) {
        // If compressor is NULL, this will not be set.
        // Consider whether to throw an exception or handle silently,
        // But now choose silent processing, Fall back to default.
        if (compressor != null) {
            this.deCompressor = compressor;
        } else {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Compressor is Null, Fall back to default deCompression." +
                    " MessageEncoding is " + getDeCompressor().getMessageEncoding());
            }
        }
        return this;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public void subscribe(StreamObserver<Object> observer) {
        this.streamSubscriber = observer;
    }

    @Override
    public void subscribe(TransportObserver observer) {
        this.transportSubscriber = observer;
    }

    @Override
    public StreamObserver<Object> asStreamObserver() {
        return streamObserver;
    }

    @Override
    public TransportObserver asTransportObserver() {
        return transportObserver;
    }

    // https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md#responses
    protected void transportError(GrpcStatus status, Map<String, Object> attachments, boolean onlyTrailers) {
        if (!onlyTrailers) {
            // set metadata
            Metadata metadata = new DefaultMetadata();
            getTransportSubscriber().onMetadata(metadata, false);
        }
        // set trailers
        Metadata trailers = getTrailers(status);
        if (attachments != null) {
            convertAttachment(trailers, attachments);
        }
        getTransportSubscriber().onMetadata(trailers, true);
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error("[Triple-Server-Error] status=" + status.code.code + " service=" + getServiceDescriptor().getServiceName()
                + " method=" + getMethodName() + " onlyTrailers=" + onlyTrailers, status.cause);
        }
    }

    protected void transportError(GrpcStatus status, Map<String, Object> attachments) {
        transportError(status, attachments, false);
    }

    protected void transportError(GrpcStatus status) {
        transportError(status, null);
    }

    protected void transportError(Throwable throwable) {
        GrpcStatus status = new GrpcStatus(Code.UNKNOWN, throwable, throwable.getMessage());
        transportError(status, null);
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

    private Metadata getTrailers(GrpcStatus grpcStatus) {
        Metadata metadata = new DefaultMetadata();
        metadata.put(TripleHeaderEnum.MESSAGE_KEY.getHeader(), getGrpcMessage(grpcStatus));
        metadata.put(TripleHeaderEnum.STATUS_KEY.getHeader(), String.valueOf(grpcStatus.code.code));
        Status.Builder builder = Status.newBuilder()
            .setCode(grpcStatus.code.code)
            .setMessage(getGrpcMessage(grpcStatus));
        Throwable throwable = grpcStatus.cause;
        if (throwable == null) {
            Status status = builder.build();
            metadata.put(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader(),
                TripleUtil.encodeBase64ASCII(status.toByteArray()));
            return metadata;
        }
        DebugInfo debugInfo = DebugInfo.newBuilder()
            .addAllStackEntries(ExceptionUtils.getStackFrameList(throwable, 10))
            // can not use now
            // .setDetail(throwable.getMessage())
            .build();
        builder.addDetails(Any.pack(debugInfo));
        Status status = builder.build();
        metadata.put(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader(),
            TripleUtil.encodeBase64ASCII(status.toByteArray()));
        return metadata;
    }

    protected Map<String, Object> parseMetadataToAttachmentMap(Metadata metadata) {
        Map<String, Object> attachments = new HashMap<>();
        for (Map.Entry<CharSequence, CharSequence> header : metadata) {
            String key = header.getKey().toString();
            if (Http2Headers.PseudoHeaderName.isPseudoHeader(key)) {
                continue;
            }
            // avoid subsequent parse protocol header
            if (TripleHeaderEnum.containsExcludeAttachments(key)) {
                continue;
            }
            if (key.endsWith("-bin") && key.length() > 4) {
                try {
                    attachments.put(key.substring(0, key.length() - 4), TripleUtil.decodeASCIIByte(header.getValue()));
                } catch (Exception e) {
                    LOGGER.error("Failed to parse response attachment key=" + key, e);
                }
            } else {
                attachments.put(key, header.getValue().toString());
            }
        }
        return attachments;
    }

    protected void convertAttachment(Metadata metadata, Map<String, Object> attachments) {
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            final String key = entry.getKey().toLowerCase(Locale.ROOT);
            if (Http2Headers.PseudoHeaderName.isPseudoHeader(key)) {
                continue;
            }
            if (TripleHeaderEnum.containsExcludeAttachments(key)) {
                continue;
            }
            final Object v = entry.getValue();
            convertSingleAttachment(metadata, key, v);
        }
    }

    private void convertSingleAttachment(Metadata metadata, String key, Object v) {
        try {
            if (v instanceof String) {
                String str = (String) v;
                metadata.put(key, str);
            } else if (v instanceof byte[]) {
                String str = TripleUtil.encodeBase64ASCII((byte[]) v);
                metadata.put(key + "-bin", str);
            }
        } catch (Throwable t) {
            LOGGER.warn("Meet exception when convert single attachment key:" + key + " value=" + v, t);
        }
    }

    protected byte[] compress(byte[] data) {
        return this.getCompressor().compress(data);
    }

    protected byte[] decompress(byte[] data) {
        return this.getDeCompressor().decompress(data);
    }

    protected abstract class AbstractTransportObserver implements TransportObserver {
        private Metadata headers;
        private Metadata trailers;

        public Metadata getHeaders() {
            return headers;
        }

        public Metadata getTrailers() {
            return trailers;
        }

        @Override
        public void onReset(Http2Error http2Error) {
            getTransportSubscriber().onReset(http2Error);
        }

        @Override
        public void onMetadata(Metadata metadata, boolean endStream) {
            if (headers == null) {
                headers = metadata;
            } else {
                trailers = metadata;
            }
        }

        protected GrpcStatus extractStatusFromMeta(Metadata metadata) {
            if (metadata.contains(TripleHeaderEnum.STATUS_KEY.getHeader())) {
                final int code = Integer.parseInt(metadata.get(TripleHeaderEnum.STATUS_KEY.getHeader()).toString());

                if (!GrpcStatus.Code.isOk(code)) {
                    GrpcStatus status = GrpcStatus.fromCode(code);
                    if (metadata.contains(TripleHeaderEnum.MESSAGE_KEY.getHeader())) {
                        final String raw = metadata.get(TripleHeaderEnum.MESSAGE_KEY.getHeader()).toString();
                        status = status.withDescription(GrpcStatus.fromMessage(raw));
                    }
                    return status;
                }
                return GrpcStatus.fromCode(Code.OK);
            }
            return GrpcStatus.fromCode(Code.OK);
        }

    }

    protected abstract class UnaryTransportObserver extends AbstractTransportObserver implements TransportObserver {
        private byte[] data;

        public byte[] getData() {
            return data;
        }

        protected abstract void onError(GrpcStatus status);

        @Override
        public void onComplete() {
            final GrpcStatus status = extractStatusFromMeta(getHeaders());
            if (Code.isOk(status.code.code)) {
                doOnComplete();
            } else {
                onError(status);
            }
        }

        protected abstract void doOnComplete();


        @Override
        public void onData(byte[] in, boolean endStream) {
            if (data == null) {
                this.data = in;
            } else {
                onError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription(DUPLICATED_DATA));
            }
        }
    }
}
