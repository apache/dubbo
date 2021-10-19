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

//    @Override
//    protected StreamObserver<Object> createStreamObserver() {
//        return new ClientStreamObserverImpl(getCancellationContext());
//    }

    @Override
    protected TransportObserver createTransportObserver() {
        return new ClientTransportObserverImpl();
    }

    @Override
    protected void startCall() {
        Response response = new Response(getRequest().getId(), getRequest().getVersion());
        AppResponse result = getMethodDescriptor().isServerStream() ? callServerStream() : callBiStream();
        response.setResult(result);
        DefaultFuture2.received(getConnection(), response);
    }

    private AppResponse callServerStream() {
        StreamObserver<Object> obServer = (StreamObserver<Object>) getRpcInvocation().getArguments()[1];
        obServer = attachCancelContext(obServer, getCancellationContext());
        subscribe(obServer);
        asStreamObserver().onNext(getRpcInvocation().getArguments()[0]);
        asStreamObserver().onCompleted();
        return new AppResponse();
    }

    private AppResponse callBiStream() {
        StreamObserver<Object> obServer = (StreamObserver<Object>) getRpcInvocation().getArguments()[0];
        obServer = attachCancelContext(obServer, getCancellationContext());
        subscribe(obServer);
        return new AppResponse(asStreamObserver());
    }

    private <T> StreamObserver<T> attachCancelContext(StreamObserver<T> observer, CancellationContext context) {
        if (observer instanceof CancelableStreamObserver) {
            CancelableStreamObserver<T> streamObserver = (CancelableStreamObserver<T>) observer;
            streamObserver.setCancellationContext(context);
            return streamObserver;
        }
        return observer;
    }

//    private class ClientStreamObserverImpl extends CancelableStreamObserver<Object> implements ClientStreamObserver<Object> {
//
//        public ClientStreamObserverImpl(CancellationContext cancellationContext) {
//            super(cancellationContext);
//        }
//
//        @Override
//        public void onNext(Object data) {
//            if (getState().allowSendMeta()) {
//                getState().setMetaSend();
//                final Metadata metadata = createRequestMeta(getRpcInvocation());
//                getTransportSubscriber().onMetadata(metadata, false);
//            }
//            if (getState().allowSendData()) {
//                final byte[] bytes = encodeRequest(data);
//                getTransportSubscriber().onData(bytes, false);
//            }
//        }
//
//        @Override
//        public void onError(Throwable throwable) {
//            if (getState().allowSendEndStream()) {
//                getState().setEndStreamSend();
//                transportError(throwable);
//            }
//        }
//
//        @Override
//        public void onCompleted() {
//            if (getState().allowSendEndStream()) {
//                getState().setEndStreamSend();
//                getTransportSubscriber().onComplete();
//            }
//        }
//
//        @Override
//        public void setCompression(String compression) {
//            if (!getState().allowSendMeta()) {
//                cancel(new IllegalStateException("Metadata already has been sent,can not set compression"));
//                return;
//            }
//            Compressor compressor = Compressor.getCompressor(getUrl().getOrDefaultFrameworkModel(), compression);
//            setCompressor(compressor);
//        }
//    }

    private class ClientTransportObserverImpl extends AbstractTransportObserver {

        @Override
        public void onData(byte[] data, boolean endStream) {
            execute(() -> {
                final Object resp = deserializeResponse(data);
                getStreamSubscriber().onNext(resp);
            });
        }

        @Override
        public void onComplete() {
            execute(() -> {
                final GrpcStatus status = extractStatusFromMeta(getHeaders());

                if (GrpcStatus.Code.isOk(status.code.code)) {
                    getStreamSubscriber().onCompleted();
                } else {
                    getStreamSubscriber().onError(status.asException());
                }
            });
        }
    }
}
