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

package org.apache.dubbo.rpc.protocol.tri.observer;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.protocol.tri.CancelableStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.RpcStatus;
import org.apache.dubbo.rpc.protocol.tri.ServerStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.call.ServerCall;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.Map;

public class ServerCallToObserverAdapter<T> extends CancelableStreamObserver<T> implements ServerStreamObserver<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CancelableStreamObserver.class);
    private final ServerCall call;
    public final CancellationContext cancellationContext;
    private final boolean wrap;
    private final MultipleSerialization multipleSerialization;
    private final String returnType;
    private final URL url;
    private boolean terminated;

    public
    ServerCallToObserverAdapter(URL url,
                                       ServerCall call,
                                       CancellationContext cancellationContext,
                                       boolean wrap,
                                       String returnType,
                                       MultipleSerialization multipleSerialization) {
        this.call = call;
        this.url = url;
        this.wrap = wrap;
        this.returnType = returnType;
        this.cancellationContext = cancellationContext;
        this.multipleSerialization = multipleSerialization;
    }

    public boolean isAutoRequestN() {
        return call.isAutoRequestN();
    }

    @Override
    public void onNext(Object data) {
        if (terminated) {
            throw new IllegalStateException("Stream observer has been terminated, no more data is allowed");
        }
        GenericPack pack = new GenericPack(multipleSerialization, call.serializerType, url);
        if (wrap) {
            try {
                data = TripleWrapper.TripleResponseWrapper.newBuilder()
                    .setSerializeType(call.serializerType)
                    .setType(returnType)
                    .setData(ByteString.copyFrom(pack.pack(data)))
                    .build();
            } catch (IOException e) {
                throw RpcStatus.INTERNAL
                    .withDescription("Serialize response failed")
                    .withCause(e)
                    .asException();
            }
        }
        call.writeMessage(data);
    }

    @Override
    public void onError(Throwable throwable) {
        if (terminated) {
            return;
        }
        final RpcStatus status = RpcStatus.getStatus(throwable);
        call.close(status, null);
        terminated = true;
    }

    public void onCompleted(Map<String, Object> attachments) {
        if (terminated) {
            return;
        }
        call.close(RpcStatus.OK, attachments);
        terminated = true;
    }

    @Override
    public void onCompleted() {
        if (terminated) {
            return;
        }
        call.close(RpcStatus.OK, null);
        terminated = true;
    }

    @Override
    public void setCompression(String compression) {
        call.setCompression(compression);
    }

    public void cancel(Throwable throwable) {
        cancellationContext.cancel(throwable);
    }

    @Override
    public void disableAutoInboundFlowControl() {
        call.disableAutoRequestN();
    }

    @Override
    public void request(int count) {
        call.requestN(count);
    }
}
