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

package org.apache.dubbo.rpc.protocol.tri.stream;

import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;

import java.util.Map;

public class UnaryClientListener implements ClientStreamListener {
    private final Connection connection;
    private final int requestId;
    private Object appResponse;

    public UnaryClientListener(Connection connection, int requestId) {
        this.connection = connection;
        this.requestId = requestId;
    }

    @Override
    public void onMessage(Object message) {
        if(appResponse!=null){
            complete(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withDescription("Too many response data"), null);
        }
        this.appResponse=message;
    }

    @Override
    public void complete(GrpcStatus status, Map<String, Object> attachments) {
        AppResponse result = new AppResponse();
        Response response = new Response(requestId, TripleConstant.TRI_VERSION);
        result.setObjectAttachments(attachments);
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
        DefaultFuture2.received(connection, response);
    }
}
