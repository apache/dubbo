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
package org.apache.dubbo.rpc.protocol.tri.h12;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;

public class UnaryServerCallListener extends AbstractServerCallListener {

    private final boolean applyCustomizeException;

    public UnaryServerCallListener(
            RpcInvocation invocation,
            Invoker<?> invoker,
            StreamObserver<Object> responseObserver,
            boolean applyCustomizeException) {
        super(invocation, invoker, responseObserver);
        this.applyCustomizeException = applyCustomizeException;
    }

    @Override
    public void onReturn(Object value) {
        responseObserver.onNext(value);
        responseObserver.onCompleted();
    }

    @Override
    public void onMessage(Object message) {
        if (message instanceof Object[]) {
            invocation.setArguments((Object[]) message);
        } else {
            invocation.setArguments(new Object[] {message});
        }
    }

    @Override
    protected void onResponseException(Throwable t) {
        if (applyCustomizeException) {
            TriRpcStatus status = TriRpcStatus.getStatus(t);
            int exceptionCode = status.code.code;
            if (exceptionCode == TriRpcStatus.UNKNOWN.code.code) {
                exceptionCode = RpcException.BIZ_EXCEPTION;
            }
            if (responseObserver instanceof ServerCallToObserverAdapter) {
                ((ServerCallToObserverAdapter<Object>) responseObserver).setExceptionCode(exceptionCode);
            }
            onReturn(t);
        } else {
            super.onResponseException(t);
        }
    }

    @Override
    public void onCancel(long code) {
        // ignore
    }

    @Override
    public void onComplete() {
        invoke();
    }
}
