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
package org.apache.dubbo.remoting.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2CodecUtil;

import static java.lang.Math.min;

public class Http2ProtocolDetector implements ProtocolDetector {
    private final ByteBuf clientPrefaceString = Http2CodecUtil.connectionPrefaceBuf();

    @Override
    public Result detect(ChannelHandlerContext ctx, ByteBuf in) {
        int prefaceLen = clientPrefaceString.readableBytes();
        int bytesRead = min(in.readableBytes(), prefaceLen);

        // If the input so far doesn't match the preface, break the connection.
        if (bytesRead == 0 || !ByteBufUtil.equals(in, 0,
                clientPrefaceString, 0, bytesRead)) {

            return Result.UNRECOGNIZED;
        }
        if (bytesRead == prefaceLen) {
            return Result.RECOGNIZED;
        }
        return Result.NEED_MORE_DATA;
    }
}
