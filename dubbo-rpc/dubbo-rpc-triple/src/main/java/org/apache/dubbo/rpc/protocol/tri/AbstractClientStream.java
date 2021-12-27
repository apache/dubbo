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
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceModel;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.rpc.DebugInfo;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AsciiString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import static org.apache.dubbo.rpc.Constants.COMPRESSOR_KEY;
import static org.apache.dubbo.rpc.protocol.tri.Compressor.DEFAULT_COMPRESSOR;


/**
 * Abstracting common actions for client streaming.
 */
public abstract class AbstractClientStream extends AbstractStream implements Stream {

    private final AsciiString scheme;
    private ConsumerModel consumerModel;
    private Connection connection;
    private RpcInvocation rpcInvocation;
    private long requestId;

    protected AbstractClientStream(URL url) {
        super(url);
        this.scheme = getSchemeFromUrl(url);
        this.getCancellationContext().addListener(context -> {
            Throwable throwable = this.getCancellationContext().getCancellationCause();
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Triple request to "
                    + getConsumerModel().getServiceName() + "#" + getMethodName() +
                    " was canceled by local exception ", throwable);
            }
            // for client cancel,send rst frame to server
            this.outboundTransportObserver()
                .onError(GrpcStatus.fromCode(GrpcStatus.Code.CANCELLED).withCause(throwable));
        });
    }


    public static UnaryClientStream unary(URL url) {
        return new UnaryClientStream(url);
    }

    public static ClientStream stream(URL url) {
        return new ClientStream(url);
    }

    /**
     * TODO move this method to somewhere else
     *
     * @param req        the request
     * @param connection connection
     * @return a client stream
     */
    public static AbstractClientStream newClientStream(Request req, Connection connection) {
        final RpcInvocation inv = (RpcInvocation) req.getData();
        final URL url = inv.getInvoker().getUrl();
        ConsumerModel consumerModel = inv.getServiceModel() != null ? (ConsumerModel) inv.getServiceModel() : (ConsumerModel) url.getServiceModel();
        MethodDescriptor methodDescriptor = getTriMethodDescriptor(consumerModel, inv);
        ClassLoadUtil.switchContextLoader(consumerModel.getClassLoader());
        AbstractClientStream stream = methodDescriptor.isUnary() ? unary(url) : stream(url);
        Compressor compressor = getCompressor(url, consumerModel);
        stream.request(req)
            .service(consumerModel)
            .connection(connection)
            .serialize((String) inv.getObjectAttachment(Constants.SERIALIZATION_KEY))
            .method(methodDescriptor)
            .setCompressor(compressor);
        return stream;
    }

    private static Compressor getCompressor(URL url, ServiceModel model) {
        String compressorStr = url.getParameter(COMPRESSOR_KEY);
        if (compressorStr == null) {
            // Compressor can not be set by dynamic config
            compressorStr = ConfigurationUtils
                .getCachedDynamicProperty(model.getModuleModel(), COMPRESSOR_KEY, DEFAULT_COMPRESSOR);
        }
        return Compressor.getCompressor(url.getOrDefaultFrameworkModel(), compressorStr);
    }

    /**
     * Get the tri protocol special MethodDescriptor
     */
    private static MethodDescriptor getTriMethodDescriptor(ConsumerModel consumerModel, RpcInvocation inv) {
        List<MethodDescriptor> methodDescriptors = consumerModel.getServiceModel().getMethods(inv.getMethodName());
        if (CollectionUtils.isEmpty(methodDescriptors)) {
            throw new IllegalStateException("methodDescriptors must not be null method=" + inv.getMethodName());
        }
        for (MethodDescriptor methodDescriptor : methodDescriptors) {
            if (Arrays.equals(inv.getParameterTypes(), methodDescriptor.getRealParameterClasses())) {
                return methodDescriptor;
            }
        }
        throw new IllegalStateException("methodDescriptors must not be null method=" + inv.getMethodName());
    }

    private Map<Class<?>, Object> tranFromStatusDetails(List<Any> detailList) {
        Map<Class<?>, Object> map = new HashMap<>();
        try {
            for (Any any : detailList) {
                if (any.is(ErrorInfo.class)) {
                    ErrorInfo errorInfo = any.unpack(ErrorInfo.class);
                    map.putIfAbsent(ErrorInfo.class, errorInfo);
                } else if (any.is(DebugInfo.class)) {
                    DebugInfo debugInfo = any.unpack(DebugInfo.class);
                    map.putIfAbsent(DebugInfo.class, debugInfo);
                }
                // support others type but now only support this
            }
        } catch (Throwable t) {
            LOGGER.error("tran from grpc-status-details error", t);
        }
        return map;
    }

    Throwable getThrowableFromTrailers(Metadata metadata) {
        if (null == metadata) {
            return null;
        }
        // second get status detail
        if (!metadata.contains(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader())) {
            return null;
        }
        final CharSequence raw = metadata.get(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader());
        byte[] statusDetailBin = decodeASCIIByte(raw);
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            final Status statusDetail = unpack(statusDetailBin, Status.class);
            List<Any> detailList = statusDetail.getDetailsList();
            Map<Class<?>, Object> classObjectMap = tranFromStatusDetails(detailList);

            // get common exception from DebugInfo
            DebugInfo debugInfo = (DebugInfo) classObjectMap.get(DebugInfo.class);
            if (debugInfo == null) {
                return new RpcException(statusDetail.getCode(),
                    GrpcStatus.decodeMessage(statusDetail.getMessage()));
            }
            String msg = ExceptionUtils.getStackFrameString(debugInfo.getStackEntriesList());
            return new RpcException(statusDetail.getCode(), msg);
        } finally {
            ClassLoadUtil.switchContextLoader(tccl);
        }
    }

    protected void startCall(WriteQueue queue, ChannelPromise promise) {
        execute(() -> {
            final ClientOutboundTransportObserver clientTransportObserver = new ClientOutboundTransportObserver(queue, promise);
            subscribe(clientTransportObserver);
            try {
                doOnStartCall();
            } catch (Throwable throwable) {
                cancel(throwable);
                DefaultFuture2.getFuture(getRequestId()).cancel();
            }
        });
    }

    protected abstract void doOnStartCall();

    @Override
    protected StreamObserver<Object> createStreamObserver() {
        return new ClientStreamObserverImpl(getCancellationContext());
    }

    @Override
    protected void cancelByRemoteReset() {
        DefaultFuture2.getFuture(getRequestId()).cancel();
    }

    @Override
    protected void cancelByLocal(Throwable throwable) {
        getCancellationContext().cancel(throwable);
    }

    @Override
    public void execute(Runnable runnable) {
        try {
            super.execute(runnable);
        } catch (RejectedExecutionException e) {
            LOGGER.error("Consumer's thread pool is full", e);
            outboundMessageSubscriber().onError(GrpcStatus.fromCode(GrpcStatus.Code.RESOURCE_EXHAUSTED)
                .withDescription("Consumer's thread pool is full").asException());
        } catch (Throwable t) {
            LOGGER.error("Consumer submit request to thread pool error ", t);
            outboundMessageSubscriber().onError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withCause(t)
                .withDescription("Consumer's error")
                .asException());
        }
    }

    public AbstractClientStream service(ConsumerModel model) {
        this.consumerModel = model;
        return this;
    }

    public AbstractClientStream request(Request request) {
        this.requestId = request.getId();
        this.rpcInvocation = (RpcInvocation) request.getData();
        return this;
    }

    protected RpcInvocation getRpcInvocation() {
        return this.rpcInvocation;
    }

    public AsciiString getScheme() {
        return scheme;
    }

    public long getRequestId() {
        return requestId;
    }

    private AsciiString getSchemeFromUrl(URL url) {
        try {
            Boolean ssl = url.getParameter(CommonConstants.SSL_ENABLED_KEY, Boolean.class);
            if (ssl == null) {
                return TripleConstant.HTTP_SCHEME;
            }
            return ssl ? TripleConstant.HTTPS_SCHEME : TripleConstant.HTTP_SCHEME;
        } catch (Exception e) {
            return TripleConstant.HTTP_SCHEME;
        }
    }

    public ConsumerModel getConsumerModel() {
        return consumerModel;
    }

    public AbstractClientStream connection(Connection connection) {
        this.connection = connection;
        return this;
    }

    public Connection getConnection() {
        return connection;
    }

    protected byte[] encodeRequest(Object value) {
        final byte[] out;
        final Object obj;

        if (getMethodDescriptor().isNeedWrap()) {
            obj = getRequestWrapper(value);
        } else {
            obj = getRequestValue(value);
        }
        out = pack(obj);
        return super.compress(out);
    }

    private TripleWrapper.TripleRequestWrapper getRequestWrapper(Object value) {
        if (getMethodDescriptor().isStream()) {
            String type = getMethodDescriptor().getParameterClasses()[0].getName();
            return wrapReq(getUrl(), getSerializeType(), value, type, getMultipleSerialization());
        } else {
            RpcInvocation invocation = (RpcInvocation) value;
            return wrapReq(getUrl(), invocation, getMultipleSerialization());
        }
    }

    private TripleWrapper.TripleRequestWrapper wrapReq(URL url, RpcInvocation invocation,
                                                       MultipleSerialization serialization) {
        try {
            String serializationName = (String) invocation.getObjectAttachment(Constants.SERIALIZATION_KEY);
            final TripleWrapper.TripleRequestWrapper.Builder builder = TripleWrapper.TripleRequestWrapper.newBuilder()
                .setSerializeType(convertHessianToWrapper(serializationName));
            for (int i = 0; i < invocation.getArguments().length; i++) {
                final String clz = invocation.getParameterTypes()[i].getName();
                builder.addArgTypes(clz);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                serialization.serialize(url, serializationName, clz, invocation.getArguments()[i], bos);
                builder.addArgs(ByteString.copyFrom(bos.toByteArray()));
            }
            return builder.build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to pack wrapper req", e);
        }
    }

    public TripleWrapper.TripleRequestWrapper wrapReq(URL url, String serializeType, Object req,
                                                      String type,
                                                      MultipleSerialization multipleSerialization) {
        try {
            final TripleWrapper.TripleRequestWrapper.Builder builder = TripleWrapper.TripleRequestWrapper.newBuilder()
                .addArgTypes(type)
                .setSerializeType(convertHessianToWrapper(serializeType));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            multipleSerialization.serialize(url, serializeType, type, req, bos);
            builder.addArgs(ByteString.copyFrom(bos.toByteArray()));
            bos.close();
            return builder.build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to pack wrapper req", e);
        }
    }

    private Object getRequestValue(Object value) {
        if (getMethodDescriptor().isUnary()) {
            RpcInvocation invocation = (RpcInvocation) value;
            return invocation.getArguments()[0];
        }
        return value;
    }

    protected Object deserializeResponse(byte[] data) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            if (getConsumerModel() != null) {
                ClassLoadUtil.switchContextLoader(getConsumerModel().getClassLoader());
            }
            if (getMethodDescriptor().isNeedWrap()) {
                final TripleWrapper.TripleResponseWrapper wrapper = unpack(data,
                    TripleWrapper.TripleResponseWrapper.class);
                if (!getSerializeType().equals(convertHessianFromWrapper(wrapper.getSerializeType()))) {
                    throw new UnsupportedOperationException("Received inconsistent serialization type from server, " +
                        "reject to deserialize! Expected:" + getSerializeType() +
                        " Actual:" + convertHessianFromWrapper(wrapper.getSerializeType()));
                }
                return unwrapResp(getUrl(), wrapper, getMultipleSerialization());
            } else {
                return unpack(data, getMethodDescriptor().getReturnClass());
            }
        } finally {
            ClassLoadUtil.switchContextLoader(tccl);
        }
    }

    public Object unwrapResp(URL url, TripleWrapper.TripleResponseWrapper wrap,
                             MultipleSerialization serialization) {
        String serializeType = convertHessianFromWrapper(wrap.getSerializeType());
        try {
            final ByteArrayInputStream bais = new ByteArrayInputStream(wrap.getData().toByteArray());
            final Object ret = serialization.deserialize(url, serializeType, wrap.getType(), bais);
            bais.close();
            return ret;
        } catch (Exception e) {
            throw new RuntimeException("Failed to unwrap resp", e);
        }
    }


    protected Metadata createRequestMeta(RpcInvocation inv) {
        Metadata metadata = new DefaultMetadata();
        // put http2 params
        metadata.put(Http2Headers.PseudoHeaderName.SCHEME.value(), this.getScheme())
            .put(Http2Headers.PseudoHeaderName.PATH.value(), getMethodPath(inv))
            .put(Http2Headers.PseudoHeaderName.AUTHORITY.value(), getUrl().getAddress())
            .put(Http2Headers.PseudoHeaderName.METHOD.value(), HttpMethod.POST.asciiName());

        metadata.put(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(), TripleConstant.CONTENT_PROTO)
            .put(TripleHeaderEnum.TIMEOUT.getHeader(), inv.get(CommonConstants.TIMEOUT_KEY) + "m")
            .put(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS)
        ;

        metadata.putIfNotNull(TripleHeaderEnum.SERVICE_VERSION.getHeader(), getUrl().getVersion())
            .putIfNotNull(TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader(),
                (String) inv.getObjectAttachments().remove(CommonConstants.APPLICATION_KEY))
            .putIfNotNull(TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader(),
                (String) inv.getObjectAttachments().remove(CommonConstants.REMOTE_APPLICATION_KEY))
            .putIfNotNull(TripleHeaderEnum.SERVICE_GROUP.getHeader(), getUrl().getGroup())
            .putIfNotNull(TripleHeaderEnum.GRPC_ACCEPT_ENCODING.getHeader(), Compressor.getAcceptEncoding(getUrl().getOrDefaultFrameworkModel()));
        if (!Compressor.NONE.getMessageEncoding().equals(getCompressor().getMessageEncoding())) {
            metadata.putIfNotNull(TripleHeaderEnum.GRPC_ENCODING.getHeader(), getCompressor().getMessageEncoding());
        }
        final Map<String, Object> attachments = inv.getObjectAttachments();
        if (attachments != null) {
            convertAttachment(metadata, attachments);
        }
        return metadata;
    }

    private String getMethodPath(RpcInvocation inv) {
        return "/" + inv.getObjectAttachment(CommonConstants.PATH_KEY) + "/" + inv.getMethodName();
    }

    protected class ClientStreamObserverImpl extends CancelableStreamObserver<Object> implements ClientStreamObserver<Object> {

        public ClientStreamObserverImpl(CancellationContext cancellationContext) {
            super(cancellationContext);
        }

        @Override
        public void onNext(Object data) {
            if (getState().allowSendMeta()) {
                final Metadata metadata = createRequestMeta(getRpcInvocation());
                outboundTransportObserver().onMetadata(metadata, false);
            }
            if (getState().allowSendData()) {
                final byte[] bytes = encodeRequest(data);
                outboundTransportObserver().onData(bytes, false);
            }
        }

        /**
         * Handle all exceptions in the request process, other procedures directly throw
         * <p>
         * other procedures is {@link ClientStreamObserver#onNext(Object)} and {@link ClientStreamObserver#onCompleted()}
         */
        @Override
        public void onError(Throwable throwable) {
            if (getState().allowSendEndStream()) {
                GrpcStatus status = GrpcStatus.getStatus(throwable);
                transportError(status, null, getState().allowSendMeta());
            } else {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Triple request to "
                        + getConsumerModel().getServiceName() + "#" + getMethodName() +
                        " was failed by exception ", throwable);
                }
            }
        }

        @Override
        public void onCompleted() {
            if (getState().allowSendEndStream()) {
                outboundTransportObserver().onComplete();
            }
        }

        @Override
        public void setCompression(String compression) {
            if (!getState().allowSendMeta()) {
                cancel(new IllegalStateException("Metadata already has been sent,can not set compression"));
                return;
            }
            Compressor compressor = Compressor.getCompressor(getUrl().getOrDefaultFrameworkModel(), compression);
            setCompressor(compressor);
        }
    }
}
