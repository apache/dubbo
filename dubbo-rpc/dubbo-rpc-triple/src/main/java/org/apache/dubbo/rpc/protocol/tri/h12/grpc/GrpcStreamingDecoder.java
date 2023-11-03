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

import org.apache.dubbo.remoting.http12.message.LengthFieldStreamingDecoder;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;

import java.io.IOException;
import java.io.InputStream;

public class GrpcStreamingDecoder extends LengthFieldStreamingDecoder {

    private static final int COMPRESSED_FLAG_MASK = 1;
    private static final int RESERVED_MASK = 0xFE;

    private boolean compressedFlag;

    private DeCompressor deCompressor = DeCompressor.NONE;

    public GrpcStreamingDecoder() {
        super(1, 4);
    }

    public void setDeCompressor(DeCompressor deCompressor) {
        this.deCompressor = deCompressor;
    }

    @Override
    protected void processOffset(InputStream inputStream, int lengthFieldOffset) throws IOException {
        int type = inputStream.read();
        if ((type & RESERVED_MASK) != 0) {
            throw new RpcException("gRPC frame header malformed: reserved bits not zero");
        }
        compressedFlag = (type & COMPRESSED_FLAG_MASK) != 0;
    }

    @Override
    protected byte[] readRawMessage(InputStream inputStream, int length) throws IOException {
        byte[] rawMessage = super.readRawMessage(inputStream, length);
        return compressedFlag ? deCompressedMessage(rawMessage) : rawMessage;
    }

    private byte[] deCompressedMessage(byte[] rawMessage) {
        return deCompressor.decompress(rawMessage);
    }
}
