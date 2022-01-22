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

import org.apache.dubbo.rpc.RpcException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link GrpcDataDecoder}
 */
public class GrpcDataDecoderTest {

    @Test
    public void test() throws Exception {
        GrpcDataDecoder grpcDataDecoder = new GrpcDataDecoder(1024, true);
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(ctx.channel()).thenReturn(channel);
        ByteBuf in = ByteBufAllocator.DEFAULT.buffer();
        List<Object> out = new ArrayList<>();

        // test frame header malformed
        in.writeByte(0xFE);
        try {
            grpcDataDecoder.decode(ctx, in, out);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof RpcException);
            RpcException rpcException = (RpcException) e;
            Assertions.assertEquals(rpcException.getCode(), GrpcStatus.Code.INTERNAL.code);
            Assertions.assertEquals(rpcException.getMessage(), "gRPC frame header malformed: reserved bits not zero");
        }
        in.clear();

        // test message exceeds maximum size
        in.writeByte(0);
        in.writeInt(2048);
        try {
            grpcDataDecoder.decode(ctx, in, out);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof RpcException);
            RpcException rpcException = (RpcException) e;
            Assertions.assertEquals(rpcException.getCode(), GrpcStatus.Code.RESOURCE_EXHAUSTED.code);
            Assertions.assertEquals(rpcException.getMessage(), "gRPC message exceeds maximum size 1024: 2048");
        }

        // test normal
        in.writeByte(1);
        in.writeInt(1024);
        byte[] bytes = new byte[1024];
        Arrays.fill(bytes, (byte) 1);
        in.writeBytes(bytes);
        in.markReaderIndex();
        AbstractClientStream stream = Mockito.mock(AbstractClientStream.class);
        Mockito.when(stream.getDeCompressor()).thenReturn(Compressor.NONE);
        Attribute<AbstractClientStream> attribute = Mockito.mock(Attribute.class);
        Mockito.when(attribute.get()).thenReturn(stream);
        Mockito.when(channel.attr(TripleConstant.CLIENT_STREAM_KEY)).thenReturn(attribute);
        grpcDataDecoder.decode(ctx, in, out);
        Assertions.assertFalse(out.isEmpty());
        Assertions.assertArrayEquals((byte[]) out.get(0), bytes);

        // test compressor not found
        in.resetReaderIndex();
        Mockito.when(stream.getDeCompressor()).thenReturn(null);
        try {
            grpcDataDecoder.decode(ctx, in, out);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof RpcException);
            RpcException rpcException = (RpcException) e;
            Assertions.assertEquals(rpcException.getCode(), GrpcStatus.Code.UNIMPLEMENTED.code);
            Assertions.assertEquals(rpcException.getMessage(), "gRPC message compressor not found");
        }

    }
}
