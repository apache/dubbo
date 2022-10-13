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

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2WindowUpdateFrame;
import org.apache.dubbo.rpc.protocol.tri.TripleFlowControlFrame;
public class ServerStreamServerCallListener extends AbstractServerCallListener {

    private Http2Connection http2Connection;

    private int windowSizeIncrement=0;

    private Http2WindowUpdateFrame http2WindowUpdateFrame;

    public ServerStreamServerCallListener(RpcInvocation invocation, Invoker<?> invoker,
        ServerCallToObserverAdapter<Object> responseObserver) {
        super(invocation, invoker, responseObserver);
    }

    @Override
    public void onReturn(Object value) {
    }

    @Override
    public void onMessage(TripleFlowControlFrame message) {
        if (message.getInstance() instanceof Object[]) {
            Object[] data = (Object[])message.getInstance();
            invocation.setArguments(new Object[]{data[0], responseObserver});
        }else{
            invocation.setArguments(new Object[]{message.getInstance(), responseObserver});
        }
        http2WindowUpdateFrame = message.getHttp2WindowUpdateFrame();
        http2Connection = message.getHttp2Connection();
        windowSizeIncrement = windowSizeIncrement +  message.getHttp2WindowUpdateFrame().windowSizeIncrement();
    }

    @Override
    public void onCancel(TriRpcStatus status) {
        responseObserver.onError(status.asException());
    }



    @Override
    public void onComplete() {
        invoke(new TripleFlowControlFrame(http2Connection,windowSizeIncrement,http2WindowUpdateFrame,null));
    }
}
