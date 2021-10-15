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
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.triple.TripleWrapper;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http2.Http2Error;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;


public abstract class AbstractClientStream extends AbstractStream implements Stream {
    private ConsumerModel consumerModel;
    private Connection connection;

    protected AbstractClientStream(URL url) {
        super(url);
    }

    protected AbstractClientStream(URL url, Executor executor) {
        super(url, executor);
    }

    public static UnaryClientStream unary(URL url) {
        return new UnaryClientStream(url);
    }

    public static ClientStream stream(URL url) {
        return new ClientStream(url);
    }

    public static AbstractClientStream newClientStream(URL url, boolean unary) {
        AbstractClientStream stream = unary ? unary(url) : stream(url);
        final CancellationContext cancellationContext = stream.getCancellationContext();
        // for client cancel,send rst frame to server
        cancellationContext.addListener(context -> {
            if (LOGGER.isWarnEnabled()) {
                Throwable throwable = cancellationContext.getCancellationCause();
                LOGGER.warn("Cancel by local throwable is ", throwable);
            }
            stream.asTransportObserver().onReset(Http2Error.CANCEL);
        });
        return stream;
    }

    public AbstractClientStream service(ConsumerModel model) {
        this.consumerModel = model;
        return this;
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

    @Override
    public void execute(Runnable runnable) {
        try {
            super.execute(runnable);
        } catch (RejectedExecutionException e) {
            LOGGER.error("Consumer's thread pool is full", e);
            getStreamSubscriber().onError(GrpcStatus.fromCode(GrpcStatus.Code.RESOURCE_EXHAUSTED)
                .withDescription("Consumer's thread pool is full").asException());
        } catch (Throwable t) {
            LOGGER.error("Consumer submit request to thread pool error ", t);
            getStreamSubscriber().onError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withCause(t)
                .withDescription("Consumer's error")
                .asException());
        }
    }

    protected byte[] encodeRequest(Object value) {
        final byte[] out;
        final Object obj;

        if (getMethodDescriptor().isNeedWrap()) {
            obj = getRequestWrapper(value);
        } else {
            obj = getRequestValue(value);
        }
        out = TripleUtil.pack(obj);

        return super.compress(out);
    }

    private TripleWrapper.TripleRequestWrapper getRequestWrapper(Object value) {
        if (getMethodDescriptor().isStream()) {
            String type = getMethodDescriptor().getParameterClasses()[0].getName();
            return TripleUtil.wrapReq(getUrl(), getSerializeType(), value, type, getMultipleSerialization());
        } else {
            RpcInvocation invocation = (RpcInvocation) value;
            return TripleUtil.wrapReq(getUrl(), invocation, getMultipleSerialization());
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
                final TripleWrapper.TripleResponseWrapper wrapper = TripleUtil.unpack(data,
                    TripleWrapper.TripleResponseWrapper.class);
                if (!getSerializeType().equals(TripleUtil.convertHessianFromWrapper(wrapper.getSerializeType()))) {
                    throw new UnsupportedOperationException("Received inconsistent serialization type from server, " +
                        "reject to deserialize! Expected:" + getSerializeType() +
                        " Actual:" + TripleUtil.convertHessianFromWrapper(wrapper.getSerializeType()));
                }
                return TripleUtil.unwrapResp(getUrl(), wrapper, getMultipleSerialization());
            } else {
                return TripleUtil.unpack(data, getMethodDescriptor().getReturnClass());
            }
        } finally {
            ClassLoadUtil.switchContextLoader(tccl);
        }
    }

    protected Metadata createRequestMeta(RpcInvocation inv) {
        Metadata metadata = new DefaultMetadata();
        metadata.put(TripleHeaderEnum.PATH_KEY.getHeader(), "/" + inv.getObjectAttachment(CommonConstants.PATH_KEY) + "/" + inv.getMethodName())
            .put(TripleHeaderEnum.AUTHORITY_KEY.getHeader(), getUrl().getAddress())
            .put(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(), TripleConstant.CONTENT_PROTO)
            .put(TripleHeaderEnum.TIMEOUT.getHeader(), inv.get(CommonConstants.TIMEOUT_KEY) + "m")
            .put(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);

        metadata.putIfNotNull(TripleHeaderEnum.SERVICE_VERSION.getHeader(), getUrl().getVersion())
            .putIfNotNull(TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader(),
                (String) inv.getObjectAttachments().remove(CommonConstants.APPLICATION_KEY))
            .putIfNotNull(TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader(),
                (String) inv.getObjectAttachments().remove(CommonConstants.REMOTE_APPLICATION_KEY))
            .putIfNotNull(TripleHeaderEnum.SERVICE_GROUP.getHeader(), getUrl().getGroup())
            .putIfNotNull(TripleHeaderEnum.GRPC_ENCODING.getHeader(), getCompressor().getMessageEncoding())
            .putIfNotNull(TripleHeaderEnum.GRPC_ACCEPT_ENCODING.getHeader(), Compressor.getAcceptEncoding(getUrl().getOrDefaultFrameworkModel()));
        final Map<String, Object> attachments = inv.getObjectAttachments();
        if (attachments != null) {
            convertAttachment(metadata, attachments);
        }
        return metadata;
    }

    @Override
    protected void cancelByRemoteReset(Http2Error http2Error) {
        DefaultFuture2.getFuture(getRequest().getId()).cancel();
    }

    @Override
    protected void cancelByLocal(Throwable throwable) {
        getCancellationContext().cancel(throwable);
    }


}
