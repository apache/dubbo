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
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.RpcInvocation;

public class UnaryClientStream extends AbstractClientStream implements Stream{
    protected UnaryClientStream(URL url) {
        super(url);
    }

    private Request req;

    protected UnaryClientStream req(Request req) {
        this.req = req;
        return this;
    }

    @Override
    protected StreamObserver<Object> createStreamObserver() {
        return new StreamObserver<Object>() {
            @Override
            public void onNext(Object data) {
                RpcInvocation invocation = (RpcInvocation)data;

                getTransportSubscriber().tryOnMetadata(new DefaultMetadata(), false);
                final byte[] bytes = encodeRequest(invocation);
                getTransportSubscriber().tryOnData(bytes, false);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {
                getTransportSubscriber().tryOnComplete();
            }
        };
    }

    @Override
    protected TransportObserver createTransportObserver() {
        return new UnaryClientTransportObserver();
    }

    private class UnaryClientTransportObserver extends UnaryTransportObserver implements TransportObserver {

        @Override
        public void onComplete(OperationHandler handler) {
            try {
                final Object resp = deserializeResponse(getData());
                Response response = new Response(req.getId(), req.getVersion());
                final AppResponse result = new AppResponse(resp);
                result.setObjectAttachments(parseMetadataToMap(getTrailers()));
                response.setResult(result);
                DefaultFuture2.received(Connection.getConnectionFromChannel(getChannel()), response);
            } catch (Exception e) {
                final GrpcStatus status = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withCause(e)
                    .withDescription("Failed to deserialize response");
                onError(status);
            }
        }

        private void onError(GrpcStatus status) {
            Response response = new Response(req.getId(), req.getVersion());
            if (status.description != null) {
                response.setErrorMessage(status.description);
            } else {
                response.setErrorMessage(status.cause.getMessage());
            }
            final byte code = GrpcStatus.toDubboStatus(status.code);
            response.setStatus(code);
            DefaultFuture2.received(Connection.getConnectionFromChannel(getChannel()), response);
        }
    }
}
