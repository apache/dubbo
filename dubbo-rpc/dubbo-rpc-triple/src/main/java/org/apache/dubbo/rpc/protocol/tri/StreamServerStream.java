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

import java.io.InputStream;

import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus.Code;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class StreamServerStream extends ServerStream implements Stream {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamServerStream.class);

    private StreamObserver<Object> observer;


    public StreamServerStream(Invoker<?> invoker, ServiceDescriptor serviceDescriptor, MethodDescriptor md, ChannelHandlerContext ctx) {
        super(invoker, ExecutorUtil.setThreadName(invoker.getUrl(), "DubboPUServerHandler"), serviceDescriptor, md, ctx);
    }


    @Override
    public void streamCreated(boolean endStream) throws Exception {
        RpcInvocation inv = buildInvocation();
        inv.setArguments(new Object[]{new StreamOutboundWriter(this)});
        inv.setParameterTypes(new Class[] {StreamObserver.class});
        inv.setReturnTypes(new Class[] {StreamObserver.class});

        Result result = getInvoker().invoke(inv);
        observer = (StreamObserver<Object>)result.get().getValue();
        setProcessor(new Processor(this, getMd(), getUrl(), getSerializeType(), getMultipleSerialization()));
        final Http2Headers headers = new DefaultHttp2Headers()
            .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO)
            .status(OK.codeAsText())
            .setInt(TripleConstant.STATUS_KEY, Code.OK.code);
        getCtx().writeAndFlush(new DefaultHttp2HeadersFrame(headers, endStream));
    }

    @Override
    protected void onSingleMessage(InputStream is) throws Exception {
        getProcessor().onSingleMessage(is);
    }


    @Override
    public void onError(GrpcStatus status) {
    }

    @Override
    public void write(Object obj, ChannelPromise promise) {
        final Message message = (Message) obj;
        final ByteBuf buf = getProcessor().encodeResponse(message, getCtx());
        getCtx().writeAndFlush(new DefaultHttp2DataFrame(buf));
    }

    public void halfClose() {
        onComplete();
    }


    public void onComplete() {
        // todo 需要判断 header /data/trailers 发送状态 避免异常时发送重复stream导致h2 error
        final Http2Headers trailers = new DefaultHttp2Headers()
            .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO)
            .status(OK.codeAsText())
            .setInt(TripleConstant.STATUS_KEY, Code.OK.code);
        getCtx().writeAndFlush(new DefaultHttp2HeadersFrame(trailers, true));
    }

    public StreamObserver<Object> getObserver() {
        return observer;
    }
}
