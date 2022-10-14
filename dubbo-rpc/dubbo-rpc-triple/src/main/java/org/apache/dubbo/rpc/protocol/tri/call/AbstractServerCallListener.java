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
import io.netty.handler.codec.http2.Http2Stream;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.TriHttp2LocalFlowController;
import org.apache.dubbo.rpc.protocol.tri.TripleFlowControlFrame;
import java.net.InetSocketAddress;

public abstract class AbstractServerCallListener implements AbstractServerCall.Listener {

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

    public void invoke(TripleFlowControlFrame tripleFlowControlBean) {
        RpcContext.restoreCancellationContext(cancellationContext);
        InetSocketAddress remoteAddress = (InetSocketAddress) invocation.getAttributes()
            .remove(AbstractServerCall.REMOTE_ADDRESS_KEY);
        RpcContext.getServerContext().setRemoteAddress(remoteAddress);
        String remoteApp = (String) invocation.getAttributes()
            .remove(TripleHeaderEnum.CONSUMER_APP_NAME_KEY);
        if (null != remoteApp) {
            RpcContext.getServerContext().setRemoteApplicationName(remoteApp);
        }
        final long stInMillis = System.currentTimeMillis();
        try {
            final Result response = invoker.invoke(invocation);
            //unary and serverstream add flowcontrol update windowsize
            if(null != tripleFlowControlBean && null != tripleFlowControlBean.getHttp2Connection() && tripleFlowControlBean.getWindowSizeIncrement() > 0 && null != tripleFlowControlBean.getHttp2WindowUpdateFrame()){
                Http2Stream stream = tripleFlowControlBean.getHttp2Connection().stream(tripleFlowControlBean.getHttp2WindowUpdateFrame().stream().id());
                TriHttp2LocalFlowController triHttp2LocalFlowController = (TriHttp2LocalFlowController)tripleFlowControlBean.getHttp2Connection().local().flowController();
                triHttp2LocalFlowController.consumeTriBytes(stream, tripleFlowControlBean.getWindowSizeIncrement());
            }
            response.whenCompleteWithContext((r, t) -> {
                responseObserver.setResponseAttachments(response.getObjectAttachments());
                if (t != null) {
                    responseObserver.onError(t);
                    return;
                }
                if (response.hasException()) {
                    responseObserver.onError(response.getException());
                    return;
                }
                final long cost = System.currentTimeMillis() - stInMillis;
                if (responseObserver.isTimeout(cost)) {
                    LOGGER.error(String.format(
                        "Invoke timeout at server side, ignored to send response. service=%s method=%s cost=%s",
                        invocation.getTargetServiceUniqueName(),
                        invocation.getMethodName(),
                        cost));
                    responseObserver.onCompleted(TriRpcStatus.DEADLINE_EXCEEDED);
                    return;
                }
                onReturn(r.getValue());
            });
        } catch (Throwable t) {
            responseObserver.onError(t);
        } finally {
            RpcContext.removeCancellationContext();
            RpcContext.removeContext();
        }
    }

    public abstract void onReturn(Object value);
}
