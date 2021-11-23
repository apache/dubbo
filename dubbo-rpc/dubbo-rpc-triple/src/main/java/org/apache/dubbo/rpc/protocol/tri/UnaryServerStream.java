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
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static org.apache.dubbo.rpc.protocol.tri.GrpcStatus.getStatus;

public class UnaryServerStream extends AbstractServerStream implements Stream {

    protected UnaryServerStream(URL url) {
        super(url);
    }

    @Override
    protected StreamObserver<Object> createStreamObserver() {
        return null;
    }

    @Override
    protected InboundTransportObserver createInboundTransportObserver() {
        return new UnaryServerTransportObserver();
    }

    private class UnaryServerTransportObserver extends ServerUnaryInboundTransportObserver {
        @Override
        public void onError(GrpcStatus status) {
            transportError(status);
        }

        @Override
        public void onComplete() {
            execute(() -> {
                if (getData() != null) {
                    invoke();
                } else {
                    onError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                        .withDescription("Missing request data"));
                }
            });
        }

        public void invoke() {
            RpcInvocation invocation = buildUnaryInvocation(getHeaders(), getData());
            if (invocation == null) {
                return;
            }
            final Result result = getInvoker().invoke(invocation);
            CompletionStage<Object> future = result.thenApply(Function.identity());
            future.whenComplete((o, throwable) -> {
                if (throwable != null) {
                    LOGGER.error("Invoke error", throwable);
                    transportError(getStatus(throwable));
                    return;
                }
                AppResponse response = (AppResponse) o;
                if (response.hasException()) {
                    transportError(getStatus(response.getException()));
                    return;
                }
                Metadata metadata = createResponseMeta();
                outboundTransportObserver().onMetadata(metadata, false);
                final byte[] data = encodeResponse(response.getValue());
                if (data == null) {
                    // already handled in encodeResponse()
                    return;
                }
                outboundTransportObserver().onData(data, false);
                Metadata trailers = TripleConstant.getSuccessResponseMeta();
                convertAttachment(trailers, response.getObjectAttachments());
                outboundTransportObserver().onMetadata(trailers, true);
            });
            RpcContext.removeContext();
        }
    }
}
