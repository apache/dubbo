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
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.protocol.tri.RpcStatus;
import org.apache.dubbo.rpc.protocol.tri.call.ServerCall;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.ByteString;

import java.io.IOException;

public class WrapperResponseObserver<T> extends ServerCallToObserverAdapter<T> {
    private final ServerCall call;
    private final String returnType;
    private final MultipleSerialization multipleSerialization;
    private final URL url;

    public WrapperResponseObserver(ServerCall call, CancellationContext cancellationContext,
                                   String returnType, MultipleSerialization multipleSerialization,
                                   URL url) {
        super(call, cancellationContext);
        this.url = url;
        this.call = call;
        this.returnType = returnType;
        this.multipleSerialization = multipleSerialization;
    }

    @Override
    public void onNext(Object data) {
        GenericPack pack = new GenericPack(multipleSerialization, call.serializerType, url);
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
        super.onNext(data);
    }

}