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
package org.apache.dubbo.rpc.protocol.rsocket;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.DefaultPayload;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.Cleanable;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.utils.AtomicPositiveInteger;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcResult;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.support.RpcUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class RSocketInvoker<T> extends AbstractInvoker<T> {

    private final RSocket[] clients;

    private final AtomicPositiveInteger index = new AtomicPositiveInteger();

    private final String version;

    private final ReentrantLock destroyLock = new ReentrantLock();

    private final Set<Invoker<?>> invokers;

    private final Serialization serialization;

    public RSocketInvoker(Class<T> serviceType, URL url, RSocket[] clients, Set<Invoker<?>> invokers) {
        super(serviceType, url, new String[]{Constants.INTERFACE_KEY, Constants.GROUP_KEY, Constants.TOKEN_KEY, Constants.TIMEOUT_KEY});
        this.clients = clients;
        // get version.
        this.version = url.getParameter(Constants.VERSION_KEY, "0.0.0");
        this.invokers = invokers;

        this.serialization = CodecSupport.getSerialization(getUrl());
    }

    @Override
    protected Result doInvoke(final Invocation invocation) throws Throwable {
        RpcInvocation inv = (RpcInvocation) invocation;
        final String methodName = RpcUtils.getMethodName(invocation);
        inv.setAttachment(Constants.PATH_KEY, getUrl().getPath());
        inv.setAttachment(Constants.VERSION_KEY, version);

        RSocket currentClient;
        if (clients.length == 1) {
            currentClient = clients[0];
        } else {
            currentClient = clients[index.getAndIncrement() % clients.length];
        }
        try {
            //TODO support timeout
            int timeout = getUrl().getMethodParameter(methodName, Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);

            RpcContext.getContext().setFuture(null);
            //encode inv: metadata and data(arg,attachment)
            Payload requestPayload = encodeInvocation(invocation);

            Class<?> retType = RpcUtils.getReturnType(invocation);

            if (retType != null && retType.isAssignableFrom(Mono.class)) {
                Mono<Payload> responseMono = currentClient.requestResponse(requestPayload);
                Mono<Object> bizMono = responseMono.map(new Function<Payload, Object>() {
                    @Override
                    public Object apply(Payload payload) {
                        return decodeData(payload);
                    }
                });
                RpcResult rpcResult = new RpcResult();
                rpcResult.setValue(bizMono);
                return rpcResult;
            } else if (retType != null && retType.isAssignableFrom(Flux.class)) {
                return requestStream(currentClient, requestPayload);
            } else {
                //request-reponse
                Mono<Payload> responseMono = currentClient.requestResponse(requestPayload);
                FutureSubscriber futureSubscriber = new FutureSubscriber(serialization, retType);
                responseMono.subscribe(futureSubscriber);
                return (Result) futureSubscriber.get();
            }

            //TODO support stream arg
        } catch (Throwable t) {
            throw new RpcException(t);
        }
    }


    private Result requestStream(RSocket currentClient, Payload requestPayload) {
        Flux<Payload> responseFlux = currentClient.requestStream(requestPayload);
        Flux<Object> retFlux = responseFlux.map(new Function<Payload, Object>() {

            @Override
            public Object apply(Payload payload) {
                return decodeData(payload);
            }
        });

        RpcResult rpcResult = new RpcResult();
        rpcResult.setValue(retFlux);
        return rpcResult;
    }


    private Object decodeData(Payload payload) {
        try {
            //TODO save the copy
            ByteBuffer dataBuffer = payload.getData();
            byte[] dataBytes = new byte[dataBuffer.remaining()];
            dataBuffer.get(dataBytes, dataBuffer.position(), dataBuffer.remaining());
            InputStream dataInputStream = new ByteArrayInputStream(dataBytes);
            ObjectInput in = serialization.deserialize(null, dataInputStream);
            int flag = in.readByte();
            if ((flag & RSocketConstants.FLAG_ERROR) != 0) {
                Throwable t = (Throwable) in.readObject();
                throw t;
            } else {
                return in.readObject();
            }
        } catch (Throwable t) {
            throw Exceptions.propagate(t);
        }
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        for (RSocket client : clients) {
            if (client.availability() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        // in order to avoid closing a client multiple times, a counter is used in case of connection per jvm, every
        // time when client.close() is called, counter counts down once, and when counter reaches zero, client will be
        // closed.
        if (super.isDestroyed()) {
            return;
        } else {
            // double check to avoid dup close
            destroyLock.lock();
            try {
                if (super.isDestroyed()) {
                    return;
                }
                super.destroy();
                if (invokers != null) {
                    invokers.remove(this);
                }
                for (RSocket client : clients) {
                    try {
                        client.dispose();
                    } catch (Throwable t) {
                        logger.warn(t.getMessage(), t);
                    }
                }

            } finally {
                destroyLock.unlock();
            }
        }
    }

    private Payload encodeInvocation(Invocation invocation) throws IOException {
        byte[] metadata = encodeMetadata(invocation);
        byte[] data = encodeData(invocation);
        return DefaultPayload.create(data, metadata);
    }

    private byte[] encodeMetadata(Invocation invocation) throws IOException {
        Map<String, Object> metadataMap = new HashMap<String, Object>();
        metadataMap.put(RSocketConstants.SERVICE_NAME_KEY, invocation.getAttachment(Constants.PATH_KEY));
        metadataMap.put(RSocketConstants.SERVICE_VERSION_KEY, invocation.getAttachment(Constants.VERSION_KEY));
        metadataMap.put(RSocketConstants.METHOD_NAME_KEY, invocation.getMethodName());
        metadataMap.put(RSocketConstants.PARAM_TYPE_KEY, ReflectUtils.getDesc(invocation.getParameterTypes()));
        metadataMap.put(RSocketConstants.SERIALIZE_TYPE_KEY, (Byte) serialization.getContentTypeId());
        return MetadataCodec.encodeMetadata(metadataMap);
    }


    private byte[] encodeData(Invocation invocation) throws IOException {
        ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();
        Serialization serialization = CodecSupport.getSerialization(getUrl());
        ObjectOutput out = serialization.serialize(getUrl(), dataOutputStream);
        RpcInvocation inv = (RpcInvocation) invocation;
        Object[] args = inv.getArguments();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                out.writeObject(args[i]);
            }
        }
        out.writeObject(RpcUtils.getNecessaryAttachments(inv));

        //clean
        out.flushBuffer();
        if (out instanceof Cleanable) {
            ((Cleanable) out).cleanup();
        }
        return dataOutputStream.toByteArray();
    }
}
