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
package org.apache.dubbo.rpc.protocol.rest.netty;


import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;


import java.util.List;

public class RestHttpResponseEncoder extends MessageToMessageEncoder<NettyHttpResponse> {

    @Override
    protected void encode(ChannelHandlerContext ctx, NettyHttpResponse nettyResponse, List<Object> out) throws Exception {
        nettyResponse.getOutputStream().flush();
        if (nettyResponse.isCommitted()) {
            out.add(LastHttpContent.EMPTY_LAST_CONTENT);
        } else {
            io.netty.handler.codec.http.HttpResponse response = nettyResponse.getDefaultHttpResponse();
            out.add(response);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void transformHeaders(NettyHttpResponse nettyResponse, HttpResponse response) {
        if (nettyResponse.isKeepAlive()) {
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        } else {
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        }

    }

}

