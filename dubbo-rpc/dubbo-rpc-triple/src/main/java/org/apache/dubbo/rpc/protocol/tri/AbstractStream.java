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
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.Status;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * AbstractStream provides more detailed actions for streaming process.
 */
public abstract class AbstractStream implements Stream {


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
    private Compressor compressor = Compressor.NONE;
    private DeCompressor deCompressor = DeCompressor.NONE;
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
            Metadata metadata = createDefaultMetadata();
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
     * default header
     * <p>
     * only status and content-type
     */
    protected Metadata createDefaultMetadata() {
        Metadata metadata = new DefaultMetadata();
        metadata.put(Http2Headers.PseudoHeaderName.STATUS.value(), HttpResponseStatus.OK.codeAsText());
        metadata.put(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
        return metadata;
    }


}