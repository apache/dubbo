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
package org.apache.dubbo.remoting.http12.message;

import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpConstants;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.h1.Http1RequestMetadata;
import org.apache.dubbo.remoting.http12.h2.Http2MetadataFrame;

import java.net.InetSocketAddress;

import io.netty.handler.codec.http2.Http2Headers.PseudoHeaderName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * HTTP 请求的主机地址解析测试。
 *
 * @date 2026-10-27
 */
class DefaultHttpRequestTest {

    @ParameterizedTest
    @CsvSource({
        "127.0.0.1:8013, 127.0.0.1, 8013",
        "localhost:8013, localhost, 8013",
        "[2001:db8::1]:8013, [2001:db8::1], 8013",
        "[2001:db8::1], [2001:db8::1], -1",
        "localhost, localhost, -1"
    })
    void parsesHostAuthority(String host, String expectedName, int expectedPort) {
        HttpHeaders headers = HttpHeaders.create();
        headers.set(HttpHeaderNames.HOST.getKey(), host);
        DefaultHttpRequest request = new DefaultHttpRequest(new Http1RequestMetadata(headers, "GET", "/"), null);

        assertEquals(expectedName, request.serverName());
        assertEquals(expectedPort, request.serverPort());
    }

    @Test
    void forwardedPortTakesPrecedenceOverHostPort() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set(HttpHeaderNames.HOST.getKey(), "localhost:8013");
        headers.set(HttpConstants.X_FORWARDED_PORT, "9090");
        DefaultHttpRequest request = new DefaultHttpRequest(new Http1RequestMetadata(headers, "GET", "/"), null);

        assertEquals(9090, request.serverPort());
    }

    @Test
    void parsesHttp2Authority() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set(PseudoHeaderName.AUTHORITY.value(), "[2001:db8::1]:8443");
        DefaultHttpRequest request = new DefaultHttpRequest(new Http2MetadataFrame(headers), null);

        assertEquals("[2001:db8::1]", request.serverName());
        assertEquals(8443, request.serverPort());
    }

    @Test
    void usesLocalPortWhenHostIsMissing() {
        HttpChannel channel = mock(HttpChannel.class);
        when(channel.localAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8013));
        DefaultHttpRequest request =
                new DefaultHttpRequest(new Http1RequestMetadata(HttpHeaders.create(), "GET", "/"), channel);

        assertEquals(8013, request.serverPort());
    }
}
