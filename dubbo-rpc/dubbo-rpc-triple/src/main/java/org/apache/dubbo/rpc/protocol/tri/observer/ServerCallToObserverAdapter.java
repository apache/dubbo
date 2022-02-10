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

package org.apache.dubbo.rpc.protocol.tri.observer;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.protocol.tri.CancelableStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.ServerStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.call.ServerCall;

public class ServerCallToObserverAdapter<T> extends CancelableStreamObserver<T> implements ServerStreamObserver<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CancelableStreamObserver.class);
    private final ServerCall call;
    public final CancellationContext cancellationContext;

    public ServerCallToObserverAdapter(ServerCall call) {
        this.call = call;
        this.cancellationContext = RpcContext.getCancellationContext();
    }

    public boolean isAutoRequestN() {
        return call.autoRequestN;
    }

    @Override
    public void disableAutoRequestN() {
        call.disableAutoRequestN();
    }

    @Override
    public void onNext(Object data) {
        call.writeMessage(data);
    }

    @Override
    public void onError(Throwable throwable) {
        final GrpcStatus status = GrpcStatus.getStatus(throwable);
        call.close(status, null);
        if (status.code == GrpcStatus.Code.CANCELLED) {
            cancellationContext.cancel(throwable);
        }
    }

    @Override
    public void onCompleted() {
        call.close(GrpcStatus.fromCode(GrpcStatus.Code.OK), null);
    }

    @Override
    public void requestN(int n) {
        call.requestN(n);
    }

//    public final void setCancellationContext(CancellationContext cancellationContext) {
//        if (contextSet.compareAndSet(false, true)) {
//            this.cancellationContext = cancellationContext;
//        } else {
//            if (LOGGER.isWarnEnabled()) {
//                LOGGER.warn("CancellationContext already set,do not repeat the set, ignore this set");
//            }
//        }
//    }

    public final void cancel(Throwable throwable) {
        if (cancellationContext == null) {
            return;
        }
        cancellationContext.cancel(throwable);
    }
}
