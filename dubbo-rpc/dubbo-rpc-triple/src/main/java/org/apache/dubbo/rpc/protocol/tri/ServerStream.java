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
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;

public class ServerStream extends AbstractServerStream implements Stream {
    protected ServerStream(URL url) {
        super(url);
    }

    @Override
    protected StreamObserver<Object> createStreamObserver() {
        return new ServerStreamObserverImpl();
    }

    @Override
    protected TransportObserver createTransportObserver() {
        return new StreamTransportObserver();
    }

    private class ServerStreamObserverImpl implements ServerStreamObserver<Object> {
        private boolean headersSent;

        @Override
        public void onNext(Object data) {
            if (!headersSent) {
                getTransportSubscriber().onMetadata(createRequestMeta(), false);
                headersSent = true;
            }
            final byte[] bytes = encodeResponse(data);
            getTransportSubscriber().onData(bytes, false);
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
            metadata.put(TripleHeaderEnum.MESSAGE_KEY.getHeader(), "OK");
            metadata.put(TripleHeaderEnum.STATUS_KEY.getHeader(), Integer.toString(GrpcStatus.Code.OK.code));
            getTransportSubscriber().onMetadata(metadata, true);
        }

        @Override
        public void setCompression(String compression) {
            if (headersSent) {
                final GrpcStatus status = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription("Metadata already has been sent,can not set compression");
                transportError(status);
                return;
            }
            Compressor compressor = Compressor.getCompressor(getUrl().getOrDefaultFrameworkModel(), compression);
            setCompressor(compressor);
        }
    }

    private class StreamTransportObserver extends AbstractTransportObserver implements TransportObserver {

        /**
         * for server stream the method only save header
         * <p>
         * for bi stream run api impl code and put observer to streamSubscriber
         *
         * <pre class="code">
         * public StreamObserver<GreeterRequest> biStream(StreamObserver<GreeterReply> replyStream) {
         *      // happen on this
         *      // you can add cancel listener on use {@link RpcContext#getCancellationContext()}
         *      return new StreamObserver<GreeterRequest>() {
         *          // ...
         *      };
         * }
         * </pre>
         */
        @Override
        public void onMetadata(Metadata metadata, boolean endStream) {
            super.onMetadata(metadata, endStream);
            if (getMethodDescriptor().getRpcType() == MethodDescriptor.RpcType.SERVER_STREAM) {
                return;
            }
            execute(() -> {
                try {
                    RpcContext.restoreCancellationContext(getCancellationContext());
                    final RpcInvocation inv = buildInvocation(metadata);
                    inv.setArguments(new Object[]{asStreamObserver()});
                    final Result result = getInvoker().invoke(inv);
                    try {
                        subscribe((StreamObserver<Object>) result.getValue());
                    } catch (Throwable t) {
                        transportError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                            .withDescription("Failed to create server's observer"));
                    }
                } finally {
                    RpcContext.removeCancellationContext();
                }
            });

        }

        @Override
        public void onData(byte[] in, boolean endStream) {
            execute(() -> {
                try {
                    if (getMethodDescriptor().getRpcType() == MethodDescriptor.RpcType.SERVER_STREAM) {
                        serverStreamOnData(in);
                        return;
                    }
                    biStreamOnData(in);
                } catch (Throwable t) {
                    transportError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                        .withDescription("Deserialize request failed")
                        .withCause(t));
                }
            });
        }

        /**
         * call observer onNext
         */
        private void biStreamOnData(byte[] in) {
            final Object[] arguments = deserializeRequest(in);
            if (arguments != null) {
                getStreamSubscriber().onNext(arguments[0]);
            }
        }

        /**
         * call api impl code
         *
         * <pre class="code">
         * public void cancelServerStream(GreeterRequest request, StreamObserver<GreeterReply> replyStream) {
         *      // happen on this
         *      // you can add cancel listener on use {@link RpcContext#getCancellationContext()}
         *      // if you want listener cancel,plz do not call onCompleted()
         *     }
         * </pre>
         */
        private void serverStreamOnData(byte[] in) {
            try {
                RpcContext.restoreCancellationContext(getCancellationContext());
                RpcInvocation inv = buildInvocation(getHeaders());
                final Object[] arguments = deserializeRequest(in);
                if (arguments != null) {
                    inv.setArguments(new Object[]{arguments[0], asStreamObserver()});
                    getInvoker().invoke(inv);
                }
            } finally {
                RpcContext.removeCancellationContext();
            }
        }

        /**
         * for server stream the method do nothing
         * <p>
         * for bi stream call onCompleted
         */
        @Override
        public void onComplete() {
            if (getMethodDescriptor().getRpcType() == MethodDescriptor.RpcType.SERVER_STREAM) {
                return;
            }
            execute(() -> {
                getStreamSubscriber().onCompleted();
            });
        }
    }
}
