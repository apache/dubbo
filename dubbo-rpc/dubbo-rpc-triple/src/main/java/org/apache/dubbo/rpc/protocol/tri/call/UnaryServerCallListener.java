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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;

public class UnaryServerCallListener extends AbstractServerCallListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCall.class);

    public UnaryServerCallListener(RpcInvocation invocation, Invoker<?> invoker,
                                   ServerCallToObserverAdapter<Object> responseObserver) {
        super(invocation, invoker, responseObserver);
    }

    @Override
    public void onReturn(Object value) {
        responseObserver.onNext(value);
        responseObserver.onCompleted(TriRpcStatus.OK);
    }

    @Override
    public void onMessage(Object message) {
        if (message instanceof Object[]) {
            invocation.setArguments((Object[]) message);
        } else {
            invocation.setArguments(new Object[]{message});
        }
    }


    @Override
    public void onCancel(String errorInfo) {
        // ignored
    }

    @Override
    public void onComplete() {
        invoke();
    }

}
