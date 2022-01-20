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

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;

import java.util.Map;

public class StreamClientListener implements ClientStreamListener {
    private final StreamObserver<Object> responseObserver;
    private final Connection connection;
    private final long requestId;
    private StreamObserver<Object> requestObserver;

    public StreamClientListener(Connection connection, long requestId, StreamObserver<Object> responseObserver) {
        this.connection = connection;
        this.requestId = requestId;
        this.responseObserver = responseObserver;
    }

    public void setRequestObserver(StreamObserver<Object> requestObserver) {
        this.requestObserver = requestObserver;
    }

    @Override
    public void onMessage(Object message) {
        responseObserver.onNext(message);
    }

    @Override
    public void complete(GrpcStatus grpcStatus, Map<String, Object> attachments) {
        if (grpcStatus.isOk()) {
            responseObserver.onCompleted();
        }
    }
}
