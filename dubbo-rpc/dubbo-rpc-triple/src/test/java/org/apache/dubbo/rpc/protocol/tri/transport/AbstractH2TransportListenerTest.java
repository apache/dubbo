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

package org.apache.dubbo.rpc.protocol.tri.transport;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.netty.handler.codec.http.HttpScheme.HTTPS;

class AbstractH2TransportListenerTest {

    @Test
    void headersToMap() {
        AbstractH2TransportListener listener = new AbstractH2TransportListener() {
            @Override
            public void onHeader(Http2Headers headers, boolean endStream) {

            }

            @Override
            public void onData(ByteBuf data, boolean endStream) {

            }

            @Override
            public void cancelByRemote(long errorCode) {

            }
        };
        DefaultHttp2Headers headers = new DefaultHttp2Headers();
        headers.scheme(HTTPS.name())
            .path("/foo.bar")
            .method(HttpMethod.POST.asciiName());
        headers.set("foo", "bar");
        final Map<String, Object> map = listener.headersToMap(headers, () -> null);
        Assertions.assertEquals(4, map.size());
    }

    @Test
    void filterReservedHeaders() {
    }
}
