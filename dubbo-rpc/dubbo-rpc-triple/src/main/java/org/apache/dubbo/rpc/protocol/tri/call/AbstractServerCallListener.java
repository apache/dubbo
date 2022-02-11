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
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.rpc.protocol.tri.GrpcStatus.getStatus;

public abstract class AbstractServerCallListener implements ServerCall.Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractServerCallListener.class);

    final RpcInvocation invocation;
    final ServerCall call;
    final Invoker<?> invoker;
    public final CancellationContext cancellationContext;

    public AbstractServerCallListener(ServerCall call, RpcInvocation invocation, Invoker<?> invoker) {
        this.call = call;
        this.invocation = invocation;
        this.invoker = invoker;
        this.cancellationContext = RpcContext.getCancellationContext();
    }

    public void invoke() {
        RpcContext.restoreCancellationContext(cancellationContext);
        final long stInNano = System.nanoTime();
        final Result result = invoker.invoke(invocation);
        CompletionStage<Object> future = result.thenApply(Function.identity());
        future.whenComplete(onResponse(stInNano));
        RpcContext.removeCancellationContext();
        RpcContext.removeContext();
    }

    private BiConsumer<Object, Throwable> onResponse(long stInNano) {
        return (o, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Invoke error", throwable);
                call.close(getStatus(throwable), null);
                return;
            }
            AppResponse response = (AppResponse) o;
            if (response.hasException()) {
                call.close(getStatus(response.getException()), null);
                return;
            }
            final Object timeoutVal = invocation.getObjectAttachment(TIMEOUT_KEY);
            final long cost = System.nanoTime() - stInNano;
            if (timeoutVal != null && cost > ((Long) timeoutVal)) {
                LOGGER.error(String.format("Invoke timeout at server side, ignored to send response. service=%s method=%s cost=%s timeout=%s",
                    invocation.getTargetServiceUniqueName(),
                    invocation.getMethodName(),
                    cost, timeoutVal));
                call.close(GrpcStatus.fromCode(GrpcStatus.Code.DEADLINE_EXCEEDED), null);
            } else {
                onServerResponse(response);
            }
        };
    }

    protected abstract void onServerResponse(AppResponse response);

}
