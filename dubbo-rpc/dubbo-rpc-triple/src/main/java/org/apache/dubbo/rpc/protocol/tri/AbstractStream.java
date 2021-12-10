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
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.model.MethodDescriptor;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.Status;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * AbstractStream provides more detailed actions for streaming process.
 */
public abstract class AbstractStream implements Stream {

    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder().withoutPadding();

    private final URL url;
    private final MultipleSerialization multipleSerialization;
    private final StreamObserver<Object> inboundMessageObserver;
    private final InboundTransportObserver inboundTransportObserver;
    private final Executor executor;
    private final CancellationContext cancellationContext;
    // AcceptEncoding does not change after the application is started,
    // so it can be obtained when constructing the stream
    private final String acceptEncoding;

    private MethodDescriptor methodDescriptor;
    private String methodName;
    private String serializeType;
    private StreamObserver<Object> outboundMessageSubscriber;
    private OutboundTransportObserver outboundTransportObserver;
    private Compressor compressor = IdentityCompressor.NONE;
    private Compressor deCompressor = IdentityCompressor.NONE;
    private volatile boolean cancelled = false;

    protected AbstractStream(URL url) {
        this(url, null);
    }

    protected AbstractStream(URL url, Executor executor) {
        this.url = url;
        final Executor sourceExecutor = lookupExecutor(url, executor);
        // wrap executor to ensure linear stream message processing
        this.executor = wrapperSerializingExecutor(sourceExecutor);
        final String value = url.getParameter(Constants.MULTI_SERIALIZATION_KEY, CommonConstants.DEFAULT_KEY);
        this.multipleSerialization = url.getOrDefaultFrameworkModel()
            .getExtensionLoader(MultipleSerialization.class)
            .getExtension(value);
        this.cancellationContext = new CancellationContext();
        // A stream implementation must know how to process inbound transport message
        this.inboundTransportObserver = createInboundTransportObserver();
        // A stream implementation must know how to process inbound App level message
        this.inboundMessageObserver = createStreamObserver();
        this.acceptEncoding = Compressor.getAcceptEncoding(getUrl().getOrDefaultFrameworkModel());
    }


    /**
     * Cancel by remote by receiving reset frame
     */
    protected abstract void cancelByRemoteReset();

    /**
     * Cancel by local by some error
     *
     * @param throwable the cancel cause
     */
    protected abstract void cancelByLocal(Throwable throwable);

    /**
     * create request StreamObserver
     */
    protected abstract StreamObserver<Object> createStreamObserver();

    /**
     * create response TransportObserver
     */
    protected abstract InboundTransportObserver createInboundTransportObserver();

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignore) {
                // ignored
            }
        }
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

    public String getAcceptEncoding() {
        return acceptEncoding;
    }

    public TransportState getState() {
        return outboundTransportObserver.state;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    protected CancellationContext getCancellationContext() {
        return cancellationContext;
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
        this.methodName = md.getMethodName();
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
    }

    /**
     * remote cancel
     */
    protected final void cancelByRemote() {
        cancel();
        cancelByRemoteReset();
    }


    public String getSerializeType() {
        return serializeType;
    }

    public AbstractStream serialize(String serializeType) {
        if (TripleConstant.HESSIAN4.equals(serializeType)) {
            serializeType = TripleConstant.HESSIAN2;
        }
        this.serializeType = serializeType;
        return this;
    }

    public MultipleSerialization getMultipleSerialization() {
        return multipleSerialization;
    }

    public StreamObserver<Object> outboundMessageSubscriber() {
        return outboundMessageSubscriber;
    }

    public OutboundTransportObserver outboundTransportObserver() {
        return outboundTransportObserver;
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
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
    public void subscribe(StreamObserver<Object> outboundMessageObserver) {
        this.outboundMessageSubscriber = outboundMessageObserver;
    }

    @Override
    public void subscribe(OutboundTransportObserver observer) {
        this.outboundTransportObserver = observer;
    }

    public StreamObserver<Object> inboundMessageObserver() {
        return inboundMessageObserver;
    }

    @Override
    public TransportObserver inboundTransportObserver() {
        return inboundTransportObserver;
    }

    // https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md#responses
    protected void transportError(GrpcStatus status, Map<String, Object> attachments, boolean onlyTrailers) {
        if (!onlyTrailers) {
            // set metadata
            Metadata metadata = new DefaultMetadata();
            outboundTransportObserver().onMetadata(metadata, false);
        }
        // set trailers
        Metadata trailers = getTrailers(status);
        if (attachments != null) {
            convertAttachment(trailers, attachments);
        }
        outboundTransportObserver().onMetadata(trailers, true);
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error("[Triple-Error] status=" + status.code.code
                + " method=" + getMethodName() + " onlyTrailers=" + onlyTrailers, status.cause);
        }
    }

    protected void transportError(GrpcStatus status) {
        transportError(status, null, false);
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
        String grpcMessage = getGrpcMessage(grpcStatus);
        grpcMessage = GrpcStatus.encodeMessage(grpcMessage);
        metadata.put(TripleHeaderEnum.MESSAGE_KEY.getHeader(), grpcMessage);
        metadata.put(TripleHeaderEnum.STATUS_KEY.getHeader(), String.valueOf(grpcStatus.code.code));
        Status.Builder builder = Status.newBuilder()
            .setCode(grpcStatus.code.code)
            .setMessage(grpcMessage);
        Throwable throwable = grpcStatus.cause;
        if (throwable == null) {
            Status status = builder.build();
            metadata.put(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader(),
                encodeBase64ASCII(status.toByteArray()));
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
            encodeBase64ASCII(status.toByteArray()));
        return metadata;
    }

    /**
     * Parse metadata to a KV pairs map.
     *
     * @param metadata the metadata from remote
     * @return KV pairs map
     */
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
            if (key.endsWith(TripleConstant.GRPC_BIN_SUFFIX) && key.length() > TripleConstant.GRPC_BIN_SUFFIX.length()) {
                try {
                    attachments.put(key.substring(0, key.length() - TripleConstant.GRPC_BIN_SUFFIX.length()), decodeASCIIByte(header.getValue()));
                } catch (Exception e) {
                    LOGGER.error("Failed to parse response attachment key=" + key, e);
                }
            } else {
                attachments.put(key, header.getValue().toString());
            }
        }
        return attachments;
    }

    /**
     * Parse and put the KV pairs into metadata. Ignore Http2 PseudoHeaderName and internal name.
     * Only raw byte array or string value will be put.
     *
     * @param metadata    the metadata holder
     * @param attachments KV pairs
     */
    protected void convertAttachment(Metadata metadata, Map<String, Object> attachments) {
        if (attachments == null) {
            return;
        }
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

    /**
     * Convert each user's attach value to metadata
     *
     * @param metadata {@link Metadata}
     * @param key      metadata key
     * @param v        metadata value (Metadata Only string and byte arrays are allowed)
     */
    private void convertSingleAttachment(Metadata metadata, String key, Object v) {
        try {
            if (v instanceof String) {
                String str = (String) v;
                metadata.put(key, str);
            } else if (v instanceof byte[]) {
                String str = encodeBase64ASCII((byte[]) v);
                metadata.put(key + TripleConstant.GRPC_BIN_SUFFIX, str);
            }
        } catch (Throwable t) {
            LOGGER.warn("Meet exception when convert single attachment key:" + key + " value=" + v, t);
        }
    }

    protected String convertHessianFromWrapper(String serializeType) {
        if (TripleConstant.HESSIAN4.equals(serializeType)) {
            return TripleConstant.HESSIAN2;
        }
        return serializeType;
    }

    protected <T> T unpack(byte[] data, Class<T> clz) {
        return unpack(new ByteArrayInputStream(data), clz);
    }

    protected <T> T unpack(InputStream is, Class<T> clz) {
        try {
            final T req = SingleProtobufUtils.deserialize(is, clz);
            return req;
        } catch (IOException e) {
            throw new RuntimeException("Failed to unpack req", e);
        } finally {
            closeQuietly(is);
        }
    }

    protected byte[] pack(Object obj) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            SingleProtobufUtils.serialize(obj, baos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to pack protobuf object", e);
        }
        return baos.toByteArray();
    }


    protected String encodeBase64ASCII(byte[] in) {
        byte[] bytes = encodeBase64(in);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    protected byte[] encodeBase64(byte[] in) {
        return BASE64_ENCODER.encode(in);
    }

    protected byte[] decodeASCIIByte(CharSequence value) {
        return BASE64_DECODER.decode(value.toString().getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Convert hessian version from Dubbo's SPI version(hessian2) to wrapper API version (hessian4)
     *
     * @param serializeType literal type
     * @return hessian4 if the param is hessian2, otherwise return the param
     */
    protected String convertHessianToWrapper(String serializeType) {
        if (TripleConstant.HESSIAN2.equals(serializeType)) {
            return TripleConstant.HESSIAN4;
        }
        return serializeType;
    }

    protected byte[] compress(byte[] data) {
        return this.getCompressor().compress(data);
    }

    protected byte[] decompress(byte[] data) {
        return this.getDeCompressor().decompress(data);
    }


}
