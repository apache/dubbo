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
import org.apache.dubbo.common.threadlocal.NamedInternalThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus.Code;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.Status;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class AbstractStream implements Stream {
    protected static final String DUPLICATED_DATA = "Duplicated data";
    private static final List<Executor> CALLBACK_EXECUTORS = new ArrayList<>(4);

    static {
        ThreadFactory tripleTF = new NamedInternalThreadFactory("tri-callback", true);
        for (int i = 0; i < 4; i++) {
            final ThreadPoolExecutor tp = new ThreadPoolExecutor(1, 1, 0, TimeUnit.DAYS,
                    new LinkedBlockingQueue<>(1024),
                    tripleTF, new ThreadPoolExecutor.AbortPolicy());
            CALLBACK_EXECUTORS.add(tp);
        }

    }

    private final URL url;
    private final MultipleSerialization multipleSerialization;
    private final StreamObserver<Object> streamObserver;
    private final TransportObserver transportObserver;
    private final Executor executor;
    private ServiceDescriptor serviceDescriptor;
    private MethodDescriptor methodDescriptor;
    private String methodName;
    private Request request;
    private String serializeType;
    private StreamObserver<Object> streamSubscriber;
    private TransportObserver transportSubscriber;

    protected AbstractStream(URL url) {
        this(url, allocateCallbackExecutor());
    }

    protected AbstractStream(URL url, Executor executor) {
        this.url = url;
        this.executor = executor;
        final String value = url.getParameter(Constants.MULTI_SERIALIZATION_KEY, CommonConstants.DEFAULT_KEY);
        this.multipleSerialization = url.getOrDefaultFrameworkModel().getExtensionLoader(MultipleSerialization.class)
                .getExtension(value);
        this.streamObserver = createStreamObserver();
        this.transportObserver = createTransportObserver();
    }

    private static Executor allocateCallbackExecutor() {
        return CALLBACK_EXECUTORS.get(ThreadLocalRandom.current().nextInt(4));
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
                + " method=" + getMethodName() +" onlyTrailers=" + onlyTrailers, status.cause);
        }
    }

    protected void transportError(GrpcStatus status, Map<String, Object> attachments) {
        transportError(status, attachments,false);
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
                .addAllStackEntries(ExceptionUtils.getStackFrameList(throwable,10))
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
        Map<String, Object> attachments = new LinkedHashMap<>();
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

    protected static abstract class AbstractTransportObserver implements TransportObserver {
        private Metadata headers;
        private Metadata trailers;

        public Metadata getHeaders() {
            return headers;
        }

        public Metadata getTrailers() {
            return trailers;
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

    protected abstract static class UnaryTransportObserver extends AbstractTransportObserver implements TransportObserver {
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

        protected abstract void doOnComplete() ;


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
