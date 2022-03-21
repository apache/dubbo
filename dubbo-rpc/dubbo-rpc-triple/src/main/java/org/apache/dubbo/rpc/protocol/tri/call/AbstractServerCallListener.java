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
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;

public abstract class AbstractServerCallListener implements ServerCall.Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractServerCallListener.class);
    public final CancellationContext cancellationContext;
    final RpcInvocation invocation;
    final Invoker<?> invoker;
    final ServerCallToObserverAdapter<Object> responseObserver;

    public AbstractServerCallListener(RpcInvocation invocation, Invoker<?> invoker,
        ServerCallToObserverAdapter<Object> responseObserver) {
        this.invocation = invocation;
        this.invoker = invoker;
        this.cancellationContext = responseObserver.cancellationContext;
        this.responseObserver = responseObserver;
    }

    public Object invoke() throws Throwable {
        RpcContext.restoreCancellationContext(cancellationContext);
        final long stInMillis = System.currentTimeMillis();
        try {
            final Result response = invoker.invoke(invocation);
            responseObserver.setResponseAttachments(response.getObjectAttachments());
            if (response.hasException()) {
                throw response.getException();
            }
            final long cost = System.currentTimeMillis() - stInMillis;
            if (responseObserver.isTimeout(cost)) {
                LOGGER.error(String.format(
                    "Invoke timeout at server side, ignored to send response. service=%s method=%s cost=%s",
                    invocation.getTargetServiceUniqueName(),
                    invocation.getMethodName(),
                    cost));
                responseObserver.onCompleted(TriRpcStatus.DEADLINE_EXCEEDED);
                return null;
            } else {
                return response.getValue();
            }
        } finally {
            RpcContext.removeCancellationContext();
            RpcContext.removeContext();
        }
    }
}
