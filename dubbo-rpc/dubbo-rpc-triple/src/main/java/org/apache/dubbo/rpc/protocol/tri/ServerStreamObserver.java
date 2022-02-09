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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.rpc.protocol.tri.observer.ManagedStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.call.ServerCall;

public class ServerStreamObserver<T> extends ManagedStreamObserver<T> {
    private final ServerCall call;

    @Override
    public void setCompression(String compression) {
        super.setCompression(compression);
    }

    @Override
    public void disableAutoRequestN() {
        super.disableAutoRequestN();
    }

    public ServerStreamObserver(ServerCall call) {
        this.call = call;
    }

    @Override
    public void onNext(Object data) {
        call.writeMessage(data);
    }

    @Override
    public void onError(Throwable throwable) {
        call.close(GrpcStatus.getStatus(throwable), null);
    }

    @Override
    public void onCompleted() {
        call.close(GrpcStatus.fromCode(GrpcStatus.Code.OK), null);
    }

    @Override
    public void requestN(int n) {
        call.requestN(n);
    }
}
