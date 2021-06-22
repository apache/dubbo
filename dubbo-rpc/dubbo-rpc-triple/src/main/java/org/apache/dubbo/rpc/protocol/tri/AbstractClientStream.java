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
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.triple.TripleWrapper;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;

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

    public static UnaryClientStream unary(URL url, Executor executor) {
        return new UnaryClientStream(url, executor);
    }

    public static AbstractClientStream stream(URL url) {
        return new ClientStream(url);
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

        return out;
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
                ClassLoadUtil.switchContextLoader(getConsumerModel().getServiceInterfaceClass().getClassLoader());
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
        metadata.put(TripleConstant.PATH_KEY, "/" + inv.getObjectAttachment(CommonConstants.PATH_KEY) + "/" + inv.getMethodName())
            .put(TripleConstant.AUTHORITY_KEY, getUrl().getAddress())
            .put(TripleConstant.CONTENT_TYPE_KEY, TripleConstant.CONTENT_PROTO)
            .put(TripleConstant.TIMEOUT, inv.get(CommonConstants.TIMEOUT_KEY) + "m")
            .put(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);

        metadata.putIfNotNull(TripleConstant.SERVICE_VERSION, inv.getInvoker().getUrl().getVersion())
            .putIfNotNull(TripleConstant.CONSUMER_APP_NAME_KEY,
                (String) inv.getObjectAttachments().remove(CommonConstants.APPLICATION_KEY))
            .putIfNotNull(TripleConstant.CONSUMER_APP_NAME_KEY,
                (String) inv.getObjectAttachments().remove(CommonConstants.REMOTE_APPLICATION_KEY))
            .putIfNotNull(TripleConstant.SERVICE_GROUP, inv.getInvoker().getUrl().getGroup());
        inv.getObjectAttachments().remove(CommonConstants.GROUP_KEY);
        inv.getObjectAttachments().remove(CommonConstants.INTERFACE_KEY);
        inv.getObjectAttachments().remove(CommonConstants.PATH_KEY);
        metadata.forEach(e -> metadata.put(e.getKey(), e.getValue()));
        final Map<String, Object> attachments = inv.getObjectAttachments();
        if (attachments != null) {
            convertAttachment(metadata, attachments);
        }
        return metadata;
    }

    protected class ClientStreamObserver implements StreamObserver<Object> {
        @Override
        public void onNext(Object data) {
            RpcInvocation invocation = (RpcInvocation) data;
            final Metadata metadata = createRequestMeta(invocation);
            getTransportSubscriber().tryOnMetadata(metadata, false);
            final byte[] bytes = encodeRequest(invocation);
            getTransportSubscriber().tryOnData(bytes, false);
        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onCompleted() {
            getTransportSubscriber().tryOnComplete();
        }
    }

}
