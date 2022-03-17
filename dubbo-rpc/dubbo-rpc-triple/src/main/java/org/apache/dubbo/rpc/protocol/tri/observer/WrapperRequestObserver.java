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

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class WrapperRequestObserver implements StreamObserver<Object> {
    private final StreamObserver<Object> delegate;
    private final String[] argumentsType;
    private final GenericPack genericPack;

    private WrapperRequestObserver(StreamObserver<Object> delegate, String[] argumentsType, GenericPack genericPack) {
        this.delegate = delegate;
        this.argumentsType = argumentsType;
        this.genericPack = genericPack;
    }

    public static StreamObserver<Object> wrap(StreamObserver<Object> delegate, String[] argumentsType, GenericPack genericPack) {
        return new WrapperRequestObserver(delegate, argumentsType, genericPack);
    }

    @Override
    public void onNext(Object data) {
        final Object[] arguments;
        if (data instanceof Object[]) {
            arguments = (Object[]) data;
        } else {
            arguments = new Object[]{data};
        }
        final TripleWrapper.TripleRequestWrapper.Builder builder = TripleWrapper.TripleRequestWrapper.newBuilder()
            .setSerializeType(genericPack.serializationName);
        for (String type : argumentsType) {
            builder.addArgTypes(type);
        }
        try {
            for (Object argument : arguments) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bos.write(genericPack.pack(argument));
                builder.addArgs(ByteString.copyFrom(bos.toByteArray()));
            }
        } catch (IOException e) {
            throw TriRpcStatus.INTERNAL
                .withDescription("Serialize request failed")
                .withCause(e)
                .asException();
        }
        delegate.onNext(builder.build());
    }

    @Override
    public void onError(Throwable throwable) {
        delegate.onError(throwable);
    }

    @Override
    public void onCompleted() {
        delegate.onCompleted();
    }
}
