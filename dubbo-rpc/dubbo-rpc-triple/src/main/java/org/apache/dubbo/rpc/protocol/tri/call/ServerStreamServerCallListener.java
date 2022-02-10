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

import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;

public class ServerStreamServerCallListener extends AbstractServerCallListener {

    private final ServerCallToObserverAdapter<Object> responseObserver;

    public ServerStreamServerCallListener(ServerCall call, RpcInvocation invocation, Invoker<?> invoker) {
        super(call, invocation, invoker);
        call.requestN(2);
        this.responseObserver = new ServerCallToObserverAdapter<>(call);
    }

    @Override
    protected void onServerResponse(AppResponse response) {
    }

    @Override
    public void onMessage(Object message) {
        invocation.setArguments(new Object[]{message, responseObserver});
        invoke();
    }

    @Override
    public void onHalfClose() {

    }

    @Override
    public void onCancel(String errorInfo) {
        responseObserver.cancel(GrpcStatus.fromCode(GrpcStatus.Code.CANCELLED).withDescription(errorInfo).asException());
    }


    @Override
    public void onComplete() {

    }
}
