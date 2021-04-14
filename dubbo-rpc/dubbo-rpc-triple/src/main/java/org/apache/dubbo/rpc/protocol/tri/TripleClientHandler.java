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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.api.ConnectionHandler;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2SettingsFrame;
import io.netty.util.ReferenceCountUtil;

import java.util.concurrent.Executor;

public class TripleClientHandler extends ChannelDuplexHandler {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Request) {
            writeRequest(ctx, (Request)msg, promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2SettingsFrame) {
            // already handled
        } else if (msg instanceof Http2GoAwayFrame) {
            final ConnectionHandler connectionHandler = ctx.pipeline().get(ConnectionHandler.class);
            connectionHandler.onGoAway(ctx.channel());
            ReferenceCountUtil.release(msg);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    private void writeRequest(ChannelHandlerContext ctx, final Request req, ChannelPromise promise) {
        final RpcInvocation inv = (RpcInvocation)req.getData();
        final URL url = inv.getInvoker().getUrl();
        ServiceRepository repo = ApplicationModel.getServiceRepository();
        MethodDescriptor methodDescriptor = repo.lookupMethod(inv.getServiceName(), inv.getMethodName());
        final Executor callback = (Executor) inv.getAttributes().remove("callback.executor");
        ClientStream clientStream;
        if (!methodDescriptor.isStream()) {
            final ClientStream2 stream = ClientStream2.unary();
            stream.onMetadata(new ReqWrapperMeta(req), (result, cause) -> {

            });

            stream.onNext(req.getData());
        } else {
            ClientStream2 stream=ClientStream2.stream(new ReqWrapperMeta(req));

            final StreamObserver<Object> observer = stream.asObserver();

            StreamObserver<Object> responseOBServer = (StreamObserver<Object>)inv.getArguments()[0];
            stream.subscribe(responseOBServer);

//            clientStream = new StreamClientStream(url, ctx, methodDescriptor, req, callback);
//            clientStream.streamCreated(req, promise);
//            clientStream.setObserver(responseOBServer);
//            StreamObserver<Object> writer = new StreamOutboundWriter(clientStream);
            Response response = new Response(req.getId(), req.getVersion());
            final AppResponse result = new AppResponse(observer);
            response.setResult(result);
            DefaultFuture2.received(Connection.getConnectionFromChannel(ctx.channel()), response);
        }
    }
}
