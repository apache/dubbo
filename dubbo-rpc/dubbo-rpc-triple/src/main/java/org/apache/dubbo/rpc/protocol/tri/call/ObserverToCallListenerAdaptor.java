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

package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;

import java.util.Map;

public class ObserverToCallListenerAdaptor implements ClientCall.Listener {
    private final StreamObserver<Object> responseObserver;
    private final boolean streamingMethod;
    private final long requestId;
    private Object appResponse;

    public ObserverToCallListenerAdaptor(long requestId, StreamObserver<Object> responseObserver, boolean streamingMethod) {
        this.requestId = requestId;
        this.responseObserver = responseObserver;
        this.streamingMethod = streamingMethod;
    }

    @Override
    public void onMessage(Object message) {
        if (streamingMethod) {
            responseObserver.onNext(message);
        } else {
            this.appResponse = message;
        }
    }

    @Override
    public void onClose(GrpcStatus status, Map<String, Object> trailers) {
        if (!streamingMethod) {
            AppResponse result = new AppResponse();
            Response response = new Response(requestId, TripleConstant.TRI_VERSION);
            result.setObjectAttachments(trailers);
            response.setResult(result);
            if (status.isOk()) {
                result.setValue(appResponse);
            } else {
                result.setException(status.cause);
                response.setResult(result);
                if (result.hasException()) {
                    final byte code = GrpcStatus.toDubboStatus(status.code);
                    response.setStatus(code);
                }
            }
            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    }
}
