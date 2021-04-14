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

import org.apache.dubbo.common.constants.CommonConstants;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2NoMoreStreamIdsException;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.AsciiString;

import java.io.InputStream;
import java.util.Map;

public abstract class ClientStream2 implements Stream{
    private Http2StreamChannel channel;
    private static final AsciiString SCHEME = AsciiString.of("http");
    private final String authority;

    private final ChannelHandlerContext ctx;
    public ClientStream2(ChannelHandlerContext ctx){
        final Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(ctx.channel());
        this.ctx=ctx;
        this.channel= streamChannelBootstrap.open().syncUninterruptibly().getNow();
    }

    @Override
    public void onMetadata(Metadata metadata, OperationHandler handler) {
        Http2Headers headers = new DefaultHttp2Headers()
                .authority(authority)
                .scheme(SCHEME)
                .method(HttpMethod.POST.asciiName())
                .path("/" + invocation.getObjectAttachment(CommonConstants.PATH_KEY) + "/" + invocation.getMethodName())
                .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO)
                .set(TripleConstant.TIMEOUT, invocation.get(CommonConstants.TIMEOUT_KEY) + "m")
                .set(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);

        final String version = invocation.getInvoker().getUrl().getVersion();
        if (version != null) {
            headers.set(TripleConstant.SERVICE_VERSION, version);
        }

        final String app = (String)invocation.getObjectAttachment(CommonConstants.APPLICATION_KEY);
        if (app != null) {
            headers.set(TripleConstant.CONSUMER_APP_NAME_KEY, app);
            invocation.getObjectAttachments().remove(CommonConstants.APPLICATION_KEY);
        }

        final String group = invocation.getInvoker().getUrl().getGroup();
        if (group != null) {
            headers.set(TripleConstant.SERVICE_GROUP, group);
            invocation.getObjectAttachments().remove(CommonConstants.GROUP_KEY);
        }
        final Map<String, Object> attachments = invocation.getObjectAttachments();
        if (attachments != null) {
            convertAttachment(headers, attachments);
        }
        headers.remove("path");
        headers.remove("interface");
        DefaultHttp2HeadersFrame frame = new DefaultHttp2HeadersFrame(headers);
        final TripleHttp2ClientResponseHandler responseHandler = new TripleHttp2ClientResponseHandler();

        TripleUtil.setClientStream(channel, this);
        channel.pipeline().addLast(responseHandler)
                .addLast(new GrpcDataDecoder(Integer.MAX_VALUE))
                .addLast(new TripleClientInboundHandler());
        channel.write(frame).addListener(future -> {
            if (!future.isSuccess()) {
                if (future.cause() instanceof Http2NoMoreStreamIdsException) {
                    ctx.close();
                }
                handler.operationDone(OperationResult.FAILURE,future.cause());
            }
        });


    }

    @Override
    public void onNext(Object data) {
        InputStream in = framer.frame(data);
        // bytebuf
        channel.write(in);
    }

    public static ClientStream2 stream(Metadata metadata){

    }
    public static ClientStream2 unary(){
        return
    }

}
