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
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;

public class ServerStream extends AbstractServerStream implements Stream {
    protected ServerStream(URL url) {
        super(url);
    }

    @Override
    protected StreamObserver<Object> createStreamObserver() {
        return new ServerStreamObserver();
    }

    @Override
    protected TransportObserver createTransportObserver() {
        return new StreamTransportObserver();
    }

    private class ServerStreamObserver implements StreamObserver<Object> {
        private boolean headersSent;

        @Override
        public void onNext(Object data) {
            if (!headersSent) {
                getTransportSubscriber().tryOnMetadata(new DefaultMetadata(), false);
                headersSent = true;
            }
            final byte[] bytes = encodeResponse(data);
            getTransportSubscriber().tryOnData(bytes, false);
        }

        @Override
        public void onError(Throwable throwable) {
            final GrpcStatus status = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withCause(throwable)
                    .withDescription("Biz exception");
            transportError(status);
        }

        @Override
        public void onCompleted() {
            Metadata metadata = new DefaultMetadata();
            metadata.put(TripleConstant.MESSAGE_KEY, "OK");
            metadata.put(TripleConstant.STATUS_KEY, Integer.toString(GrpcStatus.Code.OK.code));
            getTransportSubscriber().tryOnMetadata(metadata, true);
        }
    }

    private class StreamTransportObserver extends AbstractTransportObserver implements TransportObserver {

        @Override
        public void onMetadata(Metadata metadata, boolean endStream, OperationHandler handler) {
            super.onMetadata(metadata, endStream, handler);
            final RpcInvocation inv = buildInvocation(metadata);
            inv.setArguments(new Object[]{asStreamObserver()});
            final Result result = getInvoker().invoke(inv);
            try {
                subscribe((StreamObserver<Object>) result.getValue());
            } catch (Throwable t) {
                transportError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                        .withDescription("Failed to create server's observer"));
            }
        }

        @Override
        public void onData(byte[] in, boolean endStream, OperationHandler handler) {
            try {
                final Object[] arguments = deserializeRequest(in);
                if (arguments != null) {
                    getStreamSubscriber().onNext(arguments[0]);
                }
            } catch (Throwable t) {
                transportError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                        .withDescription("Deserialize request failed")
                        .withCause(t));
            }
        }

        @Override
        public void onComplete(OperationHandler handler) {
            getStreamSubscriber().onCompleted();
        }
    }
}
