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
import org.apache.dubbo.common.config.ConfigurationUtils;
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

import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.rpc.Constants.COMPRESSOR_KEY;
import static org.apache.dubbo.rpc.protocol.tri.Compressor.DEFAULT_COMPRESSOR;

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

        MethodDescriptor methodDescriptor = getTriMethodDescriptor(consumerModel, inv);

        ClassLoadUtil.switchContextLoader(consumerModel.getClassLoader());
        final AbstractClientStream stream = AbstractClientStream.newClientStream(url, methodDescriptor.isUnary());

        String ssl = url.getParameter(CommonConstants.SSL_ENABLED_KEY);
        if (StringUtils.isNotEmpty(ssl)) {
            ctx.channel().attr(TripleConstant.SSL_ATTRIBUTE_KEY).set(Boolean.parseBoolean(ssl));
        }
        // Compressor can not be set by dynamic config
        String compressorStr = ConfigurationUtils
            .getCachedDynamicProperty(inv.getModuleModel(), COMPRESSOR_KEY, DEFAULT_COMPRESSOR);

        Compressor compressor = Compressor.getCompressor(url.getOrDefaultFrameworkModel(), compressorStr);
        if (compressor != null) {
            stream.setCompressor(compressor);
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
                StreamObserver<Object> obServer = (StreamObserver<Object>) inv.getArguments()[0];
                obServer = attachCancelContext(obServer, stream.getCancellationContext());
                stream.subscribe(obServer);
                result = new AppResponse(stream.asStreamObserver());
            } else {
                StreamObserver<Object> obServer = (StreamObserver<Object>) inv.getArguments()[1];
                obServer = attachCancelContext(obServer, stream.getCancellationContext());
                stream.subscribe(obServer);
                result = new AppResponse();
                stream.asStreamObserver().onNext(inv.getArguments()[0]);
                stream.asStreamObserver().onCompleted();
            }
            response.setResult(result);
            DefaultFuture2.received(stream.getConnection(), response);
        }
    }

    /**
     * Get the tri protocol special MethodDescriptor
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


    public <T> StreamObserver<T> attachCancelContext(StreamObserver<T> observer, CancellationContext context) {
        if (observer instanceof CancelableStreamObserver) {
            CancelableStreamObserver<T> streamObserver = ((CancelableStreamObserver<T>) observer);
            streamObserver.setCancellationContext(context);
            return streamObserver;
        }
        return observer;
    }
}
