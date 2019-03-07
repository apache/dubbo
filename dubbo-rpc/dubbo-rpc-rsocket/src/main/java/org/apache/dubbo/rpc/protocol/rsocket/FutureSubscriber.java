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
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.rpc.RpcResult;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FutureSubscriber extends CompletableFuture<RpcResult> implements Subscriber<Payload> {

    private final Serialization serialization;

    private final Class<?> retType;

    public FutureSubscriber(Serialization serialization, Class<?> retType) {
        this.serialization = serialization;
        this.retType = retType;
    }


    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(1);
    }

    @Override
    public void onNext(Payload payload) {
        try {
            RpcResult rpcResult = new RpcResult();
            ByteBuffer dataBuffer = payload.getData();
            byte[] dataBytes = new byte[dataBuffer.remaining()];
            dataBuffer.get(dataBytes, dataBuffer.position(), dataBuffer.remaining());
            InputStream dataInputStream = new ByteArrayInputStream(dataBytes);
            ObjectInput in = serialization.deserialize(null, dataInputStream);

            int flag = in.readByte();
            if ((flag & RSocketConstants.FLAG_ERROR) != 0) {
                Throwable t = (Throwable) in.readObject();
                rpcResult.setException(t);
            } else {
                Object value = null;
                if ((flag & RSocketConstants.FLAG_NULL_VALUE) == 0) {
                    if (retType == null) {
                        value = in.readObject();
                    } else {
                        value = in.readObject(retType);
                    }
                    rpcResult.setValue(value);
                }
            }

            if ((flag & RSocketConstants.FLAG_HAS_ATTACHMENT) != 0) {
                Map<String, String> attachment = in.readObject(Map.class);
                rpcResult.setAttachments(attachment);

            }

            this.complete(rpcResult);


        } catch (Throwable t) {
            this.completeExceptionally(t);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        this.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
    }
}
