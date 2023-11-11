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
package org.apache.dubbo.rpc.protocol.tri.h12;

import org.apache.dubbo.remoting.api.ProtocolDetector;
import org.apache.dubbo.remoting.buffer.ByteBufferBackedChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBuffers;

import io.netty.handler.codec.http2.Http2CodecUtil;

import static java.lang.Math.min;

public class TripleProtocolDetector implements ProtocolDetector {

    public static final String HTTP_VERSION = "HTTP_VERSION";

    private final ChannelBuffer clientPrefaceString = new ByteBufferBackedChannelBuffer(
            Http2CodecUtil.connectionPrefaceBuf().nioBuffer());

    @Override
    public Result detect(ChannelBuffer in) {
        // http1
        if (in.readableBytes() < 2) {
            return Result.needMoreData();
        }
        byte[] magics = new byte[5];
        in.getBytes(in.readerIndex(), magics, 0, 5);
        if (isHttp(magics)) {
            Result recognized = Result.recognized();
            recognized.setAttribute(HTTP_VERSION, HttpVersion.HTTP1.getVersion());
            return recognized;
        }
        in.resetReaderIndex();

        // http2
        int prefaceLen = clientPrefaceString.readableBytes();
        int bytesRead = min(in.readableBytes(), prefaceLen);
        if (bytesRead == 0 || !ChannelBuffers.prefixEquals(in, clientPrefaceString, bytesRead)) {
            return Result.unrecognized();
        }
        if (bytesRead == prefaceLen) {
            Result recognized = Result.recognized();
            recognized.setAttribute(HTTP_VERSION, HttpVersion.HTTP2.getVersion());
            return recognized;
        }
        return Result.needMoreData();
    }

    private static boolean isHttp(byte[] magic) {
        if (magic[0] == 'G' && magic[1] == 'E' && magic[2] == 'T') {
            return true;
        }
        if (magic[0] == 'P' && magic[1] == 'O' && magic[2] == 'S' && magic[3] == 'T') {
            return true;
        }
        return false;
    }

    public static enum HttpVersion {
        HTTP1("http1"),
        HTTP2("http2");

        private final String version;

        HttpVersion(String version) {
            this.version = version;
        }

        public String getVersion() {
            return version;
        }
    }
}
