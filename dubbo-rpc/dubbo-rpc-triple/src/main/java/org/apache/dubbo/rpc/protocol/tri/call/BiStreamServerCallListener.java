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

import io.netty.handler.codec.http2.DefaultHttp2WindowUpdateFrame;
import io.netty.handler.codec.http2.Http2Connection;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;

public class BiStreamServerCallListener extends AbstractServerCallListener {

    private StreamObserver<Object> requestObserver;

    private static final Logger LOGGER = LoggerFactory.getLogger(BiStreamServerCallListener.class);

    public BiStreamServerCallListener(RpcInvocation invocation, Invoker<?> invoker,
        ServerCallToObserverAdapter<Object> responseObserver) {
        super(invocation, invoker, responseObserver);
        invocation.setArguments(new Object[]{responseObserver});
        invoke();
    }

    @Override
    public void onReturn(Object value) {
        this.requestObserver = (StreamObserver<Object>) value;
    }

    @Override
    public void onMessage(Object message,DefaultHttp2WindowUpdateFrame stream, Http2Connection connection) {
        if (message instanceof Object[]) {
            message = ((Object[]) message)[0];
        }
        requestObserver.onNext(message);
        if (responseObserver.isAutoRequestN()) {
            responseObserver.request(1);
        }
        //stream add flowcontrol update windowsize
        if(null != stream && null != connection.stream(stream.stream().id())){
            try {
                connection.local().flowController().consumeBytes(connection.stream(stream.stream().id()), stream.windowSizeIncrement());
            }catch (Exception e){
                LOGGER.error("flowcontroller failed ", e);
            }
        }
    }

    @Override
    public void onCancel(TriRpcStatus status) {
        responseObserver.onError(status.asException());
    }


    @Override
    public void onComplete() {
        requestObserver.onCompleted();
    }
}
