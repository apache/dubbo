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

import org.apache.dubbo.remoting.api.ProtocolDetector;
import org.apache.dubbo.remoting.buffer.ByteBufferBackedChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2CodecUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * {@link Http2ProtocolDetector}
 */
class Http2ProtocolDetectorTest {

    @Test
    void testDetect() {
        ProtocolDetector detector = new Http2ProtocolDetector();
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);

        ByteBuf connectionPrefaceBuf = Http2CodecUtil.connectionPrefaceBuf();
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
        ChannelBuffer in = new ByteBufferBackedChannelBuffer(byteBuf.nioBuffer());
        ProtocolDetector.Result result = detector.detect(in);
        Assertions.assertEquals(result, ProtocolDetector.Result.UNRECOGNIZED);

        byteBuf.writeBytes(connectionPrefaceBuf);
        result = detector.detect(new ByteBufferBackedChannelBuffer(byteBuf.nioBuffer()));
        Assertions.assertEquals(result, ProtocolDetector.Result.RECOGNIZED);

        byteBuf.clear();
        byteBuf.writeBytes(connectionPrefaceBuf, 0, 1);
        result = detector.detect(new ByteBufferBackedChannelBuffer(byteBuf.nioBuffer()));
        Assertions.assertEquals(result, ProtocolDetector.Result.NEED_MORE_DATA);

    }
}
