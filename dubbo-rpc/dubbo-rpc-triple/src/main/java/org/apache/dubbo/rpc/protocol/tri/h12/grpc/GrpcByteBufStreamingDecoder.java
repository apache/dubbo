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
package org.apache.dubbo.rpc.protocol.tri.h12.grpc;

import org.apache.dubbo.remoting.http12.message.ByteBufLengthFieldDecoder;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.tri.compressor.CompressorConfigure;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;

import io.netty.buffer.ByteBuf;

public class GrpcByteBufStreamingDecoder extends ByteBufLengthFieldDecoder implements CompressorConfigure {

    private static final int COMPRESSED_FLAG_MASK = 1;
    private static final int RESERVED_MASK = 0xFE;

    private boolean compressedFlag;

    private DeCompressor deCompressor = DeCompressor.NONE;

    public GrpcByteBufStreamingDecoder() {
        super(1, 4);
    }

    @Override
    public void setDeCompressor(DeCompressor deCompressor) {
        this.deCompressor = deCompressor;
    }

    @Override
    protected void processHeader() {
        ByteBuf offsetBuffer = accumulate.readSlice(lengthFieldOffset);
        processOffset(offsetBuffer);
        requiredLength = readLengthField(accumulate, lengthFieldLength);
        state = DecodeState.PAYLOAD;
    }

    @Override
    protected void processBody() {
        ByteBuf rawMessage = readRawMessage(accumulate, requiredLength);
        invokeListener(rawMessage);
        state = DecodeState.HEADER;
        requiredLength = fixedHeaderLength;
        accumulate.discardReadComponents();
    }

    protected void processOffset(ByteBuf buffer) {
        byte type = buffer.readByte();
        if ((type & RESERVED_MASK) != 0) {
            throw new RpcException("gRPC frame header malformed: reserved bits not zero");
        }
        compressedFlag = (type & COMPRESSED_FLAG_MASK) != 0;
    }

    protected ByteBuf readRawMessage(ByteBuf buffer, int length) {
        ByteBuf rawMessage = buffer.readSlice(length);
        return compressedFlag ? deCompressedMessage(rawMessage) : rawMessage;
    }

    private ByteBuf deCompressedMessage(ByteBuf rawMessage) {
        return deCompressor.decompress(rawMessage);
    }
}
