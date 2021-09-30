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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

public class GrpcDataDecoder extends ReplayingDecoder<GrpcDataDecoder.GrpcDecodeState> {
    private static final int RESERVED_MASK = 0xFE;
    private static final int COMPRESSED_FLAG_MASK = 1;
    private final int maxDataSize;

    private int len;
    private boolean compressedFlag;

    public GrpcDataDecoder(int maxDataSize) {
        super(GrpcDecodeState.HEADER);
        this.maxDataSize = maxDataSize;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case HEADER:
                int type = in.readByte();
                if ((type & RESERVED_MASK) != 0) {
                    throw GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                            .withDescription("gRPC frame header malformed: reserved bits not zero")
                            .asException();
                }
                // compression is not supported yet
                // TODO support it
                compressedFlag = (type & COMPRESSED_FLAG_MASK) != 0;
                if (compressedFlag) {
                    throw GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                            .withDescription("Compression is not supported ")
                            .asException();
                }

                len = in.readInt();
                if (len < 0 || len > maxDataSize) {
                    throw GrpcStatus.fromCode(GrpcStatus.Code.RESOURCE_EXHAUSTED)
                            .withDescription(String.format("gRPC message exceeds maximum size %d: %d",
                                    maxDataSize, len))
                            .asException();
                }
                checkpoint(GrpcDecodeState.PAYLOAD);
            case PAYLOAD:
                byte[] dst = new byte[len];
                in.readBytes(dst);
                out.add(dst);
                checkpoint(GrpcDecodeState.HEADER);
                break;
            default:
                throw new RuntimeException("Should not reach here");
        }
    }

    enum GrpcDecodeState {
        HEADER,
        PAYLOAD
    }
}
