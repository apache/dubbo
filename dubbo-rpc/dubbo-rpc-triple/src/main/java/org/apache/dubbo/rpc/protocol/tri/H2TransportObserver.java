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
import io.netty.handler.codec.http2.Http2Headers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * An observer used for transport messaging which provides full streaming support.
 * A TransportObserver receives raw data or control messages from local/remote.
 * Implementations should prefer to extend {@link OutboundTransportObserver} and {@link InboundTransportObserver}
 * instead of this interface.
 */
public interface H2TransportObserver {
    Base64.Decoder BASE64_DECODER = Base64.getDecoder();
    Base64.Encoder BASE64_ENCODER = Base64.getEncoder().withoutPadding();


    static String encodeBase64ASCII(byte[] in) {
        byte[] bytes = encodeBase64(in);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    static byte[] encodeBase64(byte[] in) {
        return BASE64_ENCODER.encode(in);
    }

    static byte[] decodeASCIIByte(CharSequence value) {
        return BASE64_DECODER.decode(value.toString().getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Transport metadata
     *
     * @param headers   metadata KV paris
     * @param endStream whether this data should terminate the stream
     */
    void onHeader(Http2Headers headers, boolean endStream);

    /**
     * Transport data
     *
     * @param data      raw byte array
     * @param endStream whether this data should terminate the stream
     */
    void onData(ByteBuf data, boolean endStream);


    void cancelByRemote(GrpcStatus status);

}
