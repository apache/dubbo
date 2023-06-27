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
package org.apache.dubbo.remoting.http12;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;

/**
 * @author icodening
 * @date 2023.06.03
 */
public abstract class AbstractServerCallListener implements ServerCall.Listener {

    protected final RpcInvocation invocation;

    protected final Invoker<?> invoker;

    protected final StreamObserver<Object> responseObserver;

    public AbstractServerCallListener(RpcInvocation invocation, Invoker<?> invoker,
                                      StreamObserver<Object> responseObserver) {
        this.invocation = invocation;
        this.invoker = invoker;
        this.responseObserver = responseObserver;
    }

    public void invoke() {
        try {
            final Result response = invoker.invoke(invocation);
            response.whenCompleteWithContext((r, t) -> {
                if (t != null) {
                    responseObserver.onError(t);
                    return;
                }
                if (response.hasException()) {
                    doOnResponseHasException(response.getException());
                    return;
                }
                onReturn(r.getValue());
            });
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            RpcContext.removeCancellationContext();
            RpcContext.removeContext();
        }
    }

    protected void doOnResponseHasException(Throwable t) {
        responseObserver.onError(t);
    }

    public abstract void onReturn(Object value);

}
