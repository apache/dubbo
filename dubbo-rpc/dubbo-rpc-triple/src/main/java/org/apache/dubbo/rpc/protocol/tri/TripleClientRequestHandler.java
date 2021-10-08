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
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2Error;

import java.util.Arrays;
import java.util.List;

public class TripleClientRequestHandler extends ChannelDuplexHandler {

    private final FrameworkModel frameworkModel;

    public TripleClientRequestHandler(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Request) {
            writeRequest(ctx, (Request) msg, promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }

    private void writeRequest(ChannelHandlerContext ctx, final Request req, final ChannelPromise promise) {
        DefaultFuture2.addTimeoutListener(req.getId(), ctx::close);
        final RpcInvocation inv = (RpcInvocation) req.getData();
        final URL url = inv.getInvoker().getUrl();
        ConsumerModel consumerModel = inv.getServiceModel() != null ? (ConsumerModel) inv.getServiceModel() : (ConsumerModel) url.getServiceModel();

        MethodDescriptor methodDescriptor = getTriMethodDescriptor(consumerModel,inv);

        ClassLoadUtil.switchContextLoader(consumerModel.getClassLoader());
        AbstractClientStream stream;
        if (methodDescriptor.isUnary()) {
            stream = AbstractClientStream.unary(url);
        } else {
            stream = AbstractClientStream.stream(url);
        }
        final CancellationContext cancellationContext = inv.getCancellationContext();
        // for client cancel,send rst frame to server
        cancellationContext.addListener(context -> {
            stream.asTransportObserver().onReset(Http2Error.CANCEL);;
        });
        stream.setCancellationContext(cancellationContext);

        String ssl = url.getParameter(CommonConstants.SSL_ENABLED_KEY);
        if (StringUtils.isNotEmpty(ssl)) {
            ctx.channel().attr(TripleConstant.SSL_ATTRIBUTE_KEY).set(Boolean.parseBoolean(ssl));
        }
        stream.service(consumerModel)
            .connection(Connection.getConnectionFromChannel(ctx.channel()))
            .method(methodDescriptor)
            .methodName(methodDescriptor.getMethodName())
            .request(req)
            .serialize((String) inv.getObjectAttachment(Constants.SERIALIZATION_KEY))
            .subscribe(new ClientTransportObserver(ctx, stream, promise));

        if (methodDescriptor.isUnary()) {
            stream.asStreamObserver().onNext(inv);
            stream.asStreamObserver().onCompleted();
        } else {
            Response response = new Response(req.getId(), req.getVersion());
            AppResponse result;
            // the stream method params is fixed
            if (methodDescriptor.getRpcType() == MethodDescriptor.RpcType.BIDIRECTIONAL_STREAM
                || methodDescriptor.getRpcType() == MethodDescriptor.RpcType.CLIENT_STREAM) {
                final StreamObserver<Object> streamObserver = (StreamObserver<Object>) inv.getArguments()[0];
                stream.subscribe(streamObserver);
                result = new AppResponse(stream.asStreamObserver());
            } else {
                final StreamObserver<Object> streamObserver = (StreamObserver<Object>) inv.getArguments()[1];
                stream.subscribe(streamObserver);
                result = new AppResponse();
                stream.asStreamObserver().onNext(inv.getArguments()[0]);
                stream.asStreamObserver().onCompleted();
            }
            response.setResult(result);
            DefaultFuture2.received(stream.getConnection(), response);
        }
    }

    /**
     * Get the trI protocol special MethodDescriptor
     */
    private MethodDescriptor getTriMethodDescriptor(ConsumerModel consumerModel, RpcInvocation inv) {
        List<MethodDescriptor> methodDescriptors = consumerModel.getServiceModel().getMethods(inv.getMethodName());
        if (CollectionUtils.isEmpty(methodDescriptors)) {
            throw new IllegalStateException("methodDescriptors must not be null method=" + inv.getMethodName());
        }
        for (MethodDescriptor methodDescriptor : methodDescriptors) {
            if (Arrays.equals(inv.getParameterTypes(), methodDescriptor.getRealParameterClasses())) {
                return methodDescriptor;
            }
        }
        throw new IllegalStateException("methodDescriptors must not be null method=" + inv.getMethodName());
    }
}
