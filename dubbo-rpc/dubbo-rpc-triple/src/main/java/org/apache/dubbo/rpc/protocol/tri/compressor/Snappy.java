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
package org.apache.dubbo.rpc.protocol.tri.compressor;

import org.apache.dubbo.rpc.RpcException;

import java.io.IOException;
import java.io.OutputStream;

import io.netty.buffer.ByteBuf;

/**
 * snappy compressor, Provide high-speed compression speed and reasonable compression ratio
 *
 * @link https://github.com/google/snappy
 */
public class Snappy implements Compressor, DeCompressor {

    public static final String SNAPPY = "snappy";

    @Override
    public String getMessageEncoding() {
        return SNAPPY;
    }

    @Override
    public byte[] compress(byte[] payloadByteArr) throws RpcException {
        if (null == payloadByteArr || 0 == payloadByteArr.length) {
            return new byte[0];
        }
        try {
            return org.xerial.snappy.Snappy.compress(payloadByteArr);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public OutputStream decorate(OutputStream outputStream) {
        return outputStream;
    }

    @Override
    public void compress(ByteBuf src, ByteBuf dst) {
        try {
            int maxCompressedLength = org.xerial.snappy.Snappy.maxCompressedLength(src.readableBytes());
            dst.ensureWritable(maxCompressedLength);
            int compressedSize = org.xerial.snappy.Snappy.compress(
                    src.nioBuffer(), dst.nioBuffer(dst.writerIndex(), maxCompressedLength));
            dst.writerIndex(dst.writerIndex() + compressedSize);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            src.release();
        }
    }

    @Override
    public byte[] decompress(byte[] payloadByteArr) {
        if (null == payloadByteArr || 0 == payloadByteArr.length) {
            return new byte[0];
        }

        try {
            return org.xerial.snappy.Snappy.uncompress(payloadByteArr);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ByteBuf decompress(ByteBuf src) {
        try {
            int uncompressedLength = org.xerial.snappy.Snappy.uncompressedLength(src.nioBuffer());
            ByteBuf dst = src.alloc().ioBuffer(uncompressedLength);
            org.xerial.snappy.Snappy.uncompress(src.nioBuffer(), dst.nioBuffer());
            return dst;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
