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
import org.apache.dubbo.rpc.protocol.tri.CancelableStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.ServerStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.call.ServerCall;

public class ServerCallToObserverAdapter<T> extends CancelableStreamObserver<T> implements ServerStreamObserver<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CancelableStreamObserver.class);
    private final ServerCall call;
    private final CancellationContext cancellationContext;
    private boolean terminated;

    public ServerCallToObserverAdapter(ServerCall call, CancellationContext cancellationContext) {
        this.call = call;
        this.cancellationContext = cancellationContext;
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
        if (terminated) {
            throw new IllegalStateException("Stream observer has been terminated, no more data is allowed");
        }
        call.writeMessage(data);
    }

    @Override
    public void onError(Throwable throwable) {
        if (terminated) {
            return;
        }
        final GrpcStatus status = GrpcStatus.getStatus(throwable);
        call.close(status, null);
        terminated = true;
    }

    @Override
    public void onCompleted() {
        if (terminated) {
            return;
        }
        call.close(GrpcStatus.fromCode(GrpcStatus.Code.OK), null);
        terminated = true;
    }

    @Override
    public void requestN(int n) {
        call.requestN(n);
    }

    @Override
    public void setCompression(String compression) {
        call.setCompression(compression);
    }

    public void cancel(Throwable throwable) {
        cancellationContext.cancel(throwable);
    }
}
