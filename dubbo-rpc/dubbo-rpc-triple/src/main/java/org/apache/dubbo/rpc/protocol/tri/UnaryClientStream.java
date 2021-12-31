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
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.AppResponse;

public class UnaryClientStream extends AbstractClientStream implements Stream {

    protected UnaryClientStream(URL url) {
        super(url);
    }

    @Override
    protected void doOnStartCall() {
        inboundMessageObserver().onNext(getRpcInvocation());
        inboundMessageObserver().onCompleted();
    }

    @Override
    protected InboundTransportObserver createInboundTransportObserver() {
        return new ClientUnaryInboundTransportObserver();
    }


    private class ClientUnaryInboundTransportObserver extends ServerUnaryInboundTransportObserver {

        @Override
        public void onComplete() {
            execute(() -> {
                final GrpcStatus status = extractStatusFromMeta(getHeaders());
                if (GrpcStatus.Code.isOk(status.code.code)) {
                    try {
                        AppResponse result;
                        if (!Void.TYPE.equals(getMethodDescriptor().getReturnClass())) {
                            final Object resp = deserializeResponse(getData());
                            result = new AppResponse(resp);
                        } else {
                            result = new AppResponse();
                        }
                        Response response = new Response(getRequestId(), TripleConstant.TRI_VERSION);
                        result.setObjectAttachments(parseMetadataToAttachmentMap(getTrailers()));
                        response.setResult(result);
                        DefaultFuture2.received(getConnection(), response);
                    } catch (Exception e) {
                        final GrpcStatus clientStatus = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                            .withCause(e)
                            .withDescription("Failed to deserialize response");
                        onError(clientStatus);
                    }
                } else {
                    onError(status);
                }
            });
        }

        @Override
        public void onError(GrpcStatus status) {
            Response response = new Response(getRequestId(), TripleConstant.TRI_VERSION);
            response.setErrorMessage(status.description);
            final AppResponse result = new AppResponse();
            final Metadata trailers = getTrailers() == null ? getHeaders() : getTrailers();
            final Throwable trailersException = getThrowableFromTrailers(trailers);
            if (trailersException != null) {
                result.setException(trailersException);
            } else {
                result.setException(status.cause);
            }
            result.setObjectAttachments(UnaryClientStream.this.parseMetadataToAttachmentMap(trailers));
            response.setResult(result);
            if (!result.hasException()) {
                final byte code = GrpcStatus.toDubboStatus(status.code);
                response.setStatus(code);
            }
            DefaultFuture2.received(getConnection(), response);
        }


    }
}
