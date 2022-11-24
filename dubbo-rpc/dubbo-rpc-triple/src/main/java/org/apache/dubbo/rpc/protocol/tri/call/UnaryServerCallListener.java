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
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2WindowUpdateFrame;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TriHttp2LocalFlowController;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.TripleFlowControlFrame;
public class UnaryServerCallListener extends AbstractServerCallListener {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(UnaryServerCallListener.class);

    private Http2Connection http2Connection;

    private int windowSizeIncrement=0;

    private Http2WindowUpdateFrame http2WindowUpdateFrame;

    public UnaryServerCallListener(RpcInvocation invocation, Invoker<?> invoker,
        ServerCallToObserverAdapter<Object> responseObserver) {
        super(invocation, invoker, responseObserver);
    }

    @Override
    public void onReturn(Object value) {
        responseObserver.onNext(value);
        responseObserver.onCompleted();
    }

    @Override
    public void onMessage(Object message) {
        TripleFlowControlFrame tripleFlowControlFrame = (TripleFlowControlFrame)message;
        if (tripleFlowControlFrame.getInstance() instanceof Object[]) {
            invocation.setArguments((Object[])tripleFlowControlFrame.getInstance());
        } else {
            invocation.setArguments(new Object[]{tripleFlowControlFrame.getInstance()});
        }
        http2WindowUpdateFrame = tripleFlowControlFrame.getTripleFlowControlBean().getHttp2WindowUpdateFrame();
        http2Connection = tripleFlowControlFrame.getTripleFlowControlBean().getHttp2Connection();
        windowSizeIncrement = windowSizeIncrement + tripleFlowControlFrame.getTripleFlowControlBean().getHttp2WindowUpdateFrame().windowSizeIncrement();
    }

    @Override
    public void onCancel(TriRpcStatus status) {
        // ignored
    }


    @Override
    public void onComplete() {
        invoke();
        try {
            //unary and serverstream add flowcontrol update windowsize
            TriHttp2LocalFlowController triHttp2LocalFlowController = (TriHttp2LocalFlowController)http2Connection.local().flowController();
            triHttp2LocalFlowController.consumeTriBytes(http2Connection.stream(http2WindowUpdateFrame.stream().id()),windowSizeIncrement);
        }catch (Exception e){
            LOGGER.error("flowcontrol exception",e);
        }
    }

}
