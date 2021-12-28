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
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.CancellationContext;

public class ClientStream extends AbstractClientStream implements Stream {

    protected ClientStream(URL url) {
        super(url);
    }

    @Override
    protected InboundTransportObserver createInboundTransportObserver() {
        return new ClientStreamInboundTransportObserverImpl();
    }

    @Override
    protected void doOnStartCall() {
        Response response = new Response(getRequestId(), TripleConstant.TRI_VERSION);
        AppResponse result = getMethodDescriptor().isServerStream() ? callServerStream() : callBiStream();
        response.setResult(result);
        DefaultFuture2.received(getConnection(), response);
    }

    private AppResponse callServerStream() {
        StreamObserver<Object> obServer = (StreamObserver<Object>) getRpcInvocation().getArguments()[1];
        obServer = attachCancelContext(obServer, getCancellationContext());
        subscribe(obServer);
        inboundMessageObserver().onNext(getRpcInvocation().getArguments()[0]);
        inboundMessageObserver().onCompleted();
        return new AppResponse();
    }

    private AppResponse callBiStream() {
        StreamObserver<Object> obServer = (StreamObserver<Object>) getRpcInvocation().getArguments()[0];
        obServer = attachCancelContext(obServer, getCancellationContext());
        subscribe(obServer);
        return new AppResponse(inboundMessageObserver());
    }

    private <T> StreamObserver<T> attachCancelContext(StreamObserver<T> observer, CancellationContext context) {
        if (observer instanceof CancelableStreamObserver) {
            CancelableStreamObserver<T> streamObserver = (CancelableStreamObserver<T>) observer;
            streamObserver.setCancellationContext(context);
            return streamObserver;
        }
        return observer;
    }

    private class ClientStreamInboundTransportObserverImpl extends InboundTransportObserver {

        private boolean error = false;

        @Override
        public void onData(byte[] data, boolean endStream) {
            execute(() -> {
                try {
                    final Object resp = deserializeResponse(data);
                    outboundMessageSubscriber().onNext(resp);
                } catch (Throwable throwable) {
                    onError(throwable);
                }
            });
        }

        @Override
        public void onError(GrpcStatus status) {
            onError(status.asException());
        }

        @Override
        public void onComplete() {
            execute(() -> {
                getState().setServerEndStreamReceived();
                final Metadata trailers = getTrailers() == null ? getHeaders() : getTrailers();
                final GrpcStatus status = extractStatusFromMeta(trailers);
                if (GrpcStatus.Code.isOk(status.code.code)) {
                    outboundMessageSubscriber().onCompleted();
                } else {
                    final Throwable trailersException = getThrowableFromTrailers(trailers);
                    if (trailersException != null) {
                        onError(trailersException);
                    } else {
                        onError(status.cause);
                    }
                }
            });
        }

        private void onError(Throwable throwable) {
            if (error) {
                return;
            }
            error = true;
            if (!getState().serverSendStreamReceived()) {
                cancel(throwable);
            }
            outboundMessageSubscriber().onError(throwable);
        }
    }
}
