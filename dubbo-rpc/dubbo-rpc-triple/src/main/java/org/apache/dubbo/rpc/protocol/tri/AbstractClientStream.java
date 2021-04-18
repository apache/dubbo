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

import java.util.concurrent.Executor;

import io.netty.channel.Channel;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.triple.TripleWrapper;

public abstract class AbstractClientStream extends AbstractStream implements Stream {
    private final ConsumerModel consumerModel;
    protected Executor callbackExecutor;
    private Channel channel;

    protected AbstractClientStream(URL url) {
        super(url);
        this.consumerModel = lookupConsumerModel(url);
    }

    public static UnaryClientStream unary(URL url) {
        return new UnaryClientStream(url);
    }

    public static AbstractClientStream stream(URL url) {
        return new ClientStream(url);
    }

    private ConsumerModel lookupConsumerModel(URL url) {
        ServiceRepository repo = ApplicationModel.getServiceRepository();
        final ConsumerModel model = repo.lookupReferredService(url.getServiceKey());
        if (model != null) {
            ClassLoadUtil.switchContextLoader(model.getServiceInterfaceClass().getClassLoader());
        }
        return model;
    }

    public ConsumerModel getConsumerModel() {
        return consumerModel;
    }

    public Executor getCallbackExecutor() {
        return callbackExecutor;
    }

    public AbstractClientStream callback(Executor callbackExecutor) {
        this.callbackExecutor = callbackExecutor;
        return this;
    }

    public AbstractClientStream channel(Channel channel) {
        this.channel = channel;
        return this;
    }

    public Channel getChannel() {
        return channel;
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
            RpcInvocation invocation = (RpcInvocation)value;
            return TripleUtil.wrapReq(getUrl(), invocation, getMultipleSerialization());
        }
    }

    private Object getRequestValue(Object value) {
        if (getMethodDescriptor().isUnary()) {
            RpcInvocation invocation = (RpcInvocation)value;
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
                serialize(wrapper.getSerializeType());
                return TripleUtil.unwrapResp(getUrl(), wrapper, getMultipleSerialization());
            } else {
                return TripleUtil.unpack(data, getMethodDescriptor().getReturnClass());
            }
        } finally {
            ClassLoadUtil.switchContextLoader(tccl);
        }
    }

}
