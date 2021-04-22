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
import org.apache.dubbo.common.stream.StreamObserver;

import java.util.concurrent.Executor;

public class ClientStream extends AbstractClientStream implements Stream {
    protected ClientStream(URL url, Executor executor) {
        super(url, executor);
    }

    @Override
    protected StreamObserver<Object> createStreamObserver() {
        return new ClientStreamObserver() {
            @Override
            public void onNext(Object data) {
                execute(() -> {
                    getTransportSubscriber().tryOnMetadata(new DefaultMetadata(), false);
                    final byte[] bytes = encodeRequest(data);
                    getTransportSubscriber().tryOnData(bytes, false);
                });
            }

            @Override
            public void onError(Throwable throwable) {
                transportError(throwable);
            }
        };
    }

    @Override
    protected TransportObserver createTransportObserver() {
        return new TransportObserver() {
            @Override
            public void onMetadata(Metadata metadata, boolean endStream, OperationHandler handler) {
            }

            @Override
            public void onData(byte[] data, boolean endStream, OperationHandler handler) {
                execute(() -> {
                    final Object resp = deserializeResponse(data);
                    getStreamSubscriber().onNext(resp);
                });
            }

            @Override
            public void onComplete(OperationHandler handler) {
                execute(() -> getStreamSubscriber().onCompleted());
            }
        };
    }
}
