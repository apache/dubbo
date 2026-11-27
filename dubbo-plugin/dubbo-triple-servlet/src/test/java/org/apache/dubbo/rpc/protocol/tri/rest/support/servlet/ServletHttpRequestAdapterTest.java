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
package org.apache.dubbo.rpc.protocol.tri.rest.support.servlet;

import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpConstants;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.h1.Http1RequestMetadata;
import org.apache.dubbo.rpc.model.FrameworkModel;

import javax.servlet.ServletInputStream;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServletHttpRequestAdapterTest {

    @Test
    void contextPathOfRootContextIsEmpty() {
        DummyServletContext servletContext = new DummyServletContext(FrameworkModel.defaultModel());
        ServletHttpRequestAdapter request = new ServletHttpRequestAdapter(
                new Http1RequestMetadata(HttpHeaders.create(), "GET", "/orders/1?x=1"), null, servletContext, null);

        assertEquals("", request.getContextPath());
        assertEquals("", servletContext.getContextPath());
    }

    @Test
    void remoteHostReturnsRemoteHostNameInsteadOfPort() {
        InetSocketAddress remoteAddress = InetSocketAddress.createUnresolved("client.example.com", 54321);
        HttpChannel channel = new StubHttpChannel(remoteAddress, null);
        ServletHttpRequestAdapter request = new ServletHttpRequestAdapter(
                new Http1RequestMetadata(HttpHeaders.create(), "GET", "/orders/1"), channel, null, null);

        assertEquals("client.example.com", request.getRemoteHost());
        assertEquals(54321, request.getRemotePort());
    }

    @ParameterizedTest
    @CsvSource({
        "http, example.com, 80, http://example.com/orders/1",
        "https, example.com, 443, https://example.com/orders/1",
        "http, example.com:80, 80, http://example.com/orders/1",
        "http, example.com:8080, 8080, http://example.com:8080/orders/1",
        "https, example.com:443, 443, https://example.com/orders/1",
        "https, example.com:8443, 8443, https://example.com:8443/orders/1"
    })
    void requestUrlAndServerPortNormalizeDefaultPorts(
            String scheme, String host, int expectedPort, String expectedUrl) {
        HttpHeaders headers = HttpHeaders.create();
        headers.set(HttpHeaderNames.HOST.getKey(), host);
        headers.set(HttpConstants.X_FORWARDED_PROTO, scheme);
        ServletHttpRequestAdapter request =
                new ServletHttpRequestAdapter(new Http1RequestMetadata(headers, "GET", "/orders/1"), null, null, null);

        assertEquals(expectedPort, request.getServerPort());
        assertEquals(expectedUrl, request.getRequestURL().toString());
    }

    @Test
    void inputStreamReadLineStopsAtNewline() throws IOException {
        ServletHttpRequestAdapter request = new ServletHttpRequestAdapter(
                new Http1RequestMetadata(HttpHeaders.create(), "POST", "/upload"), null, null, null);
        request.setInputStream(new ByteArrayInputStream("first\nsecond\n".getBytes(StandardCharsets.UTF_8)));

        ServletInputStream inputStream = request.getInputStream();
        byte[] buf = new byte[32];

        int firstLen = inputStream.readLine(buf, 0, buf.length);
        assertEquals(6, firstLen);
        assertEquals("first\n", new String(buf, 0, firstLen, StandardCharsets.UTF_8));

        int secondLen = inputStream.readLine(buf, 0, buf.length);
        assertEquals(7, secondLen);
        assertEquals("second\n", new String(buf, 0, secondLen, StandardCharsets.UTF_8));

        assertEquals(-1, inputStream.readLine(buf, 0, buf.length));
    }

    @Test
    void inputStreamReadWithOffsetDelegatesToUnderlyingStream() throws IOException {
        AtomicInteger bulkReadCalls = new AtomicInteger();
        AtomicInteger singleByteReadCalls = new AtomicInteger();
        FilterInputStream trackingStream =
                new FilterInputStream(new ByteArrayInputStream("abcdef".getBytes(StandardCharsets.UTF_8))) {
                    @Override
                    public int read() throws IOException {
                        singleByteReadCalls.incrementAndGet();
                        return super.read();
                    }

                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        bulkReadCalls.incrementAndGet();
                        return super.read(b, off, len);
                    }
                };

        ServletHttpRequestAdapter request = new ServletHttpRequestAdapter(
                new Http1RequestMetadata(HttpHeaders.create(), "POST", "/upload"), null, null, null);
        request.setInputStream(trackingStream);

        byte[] buf = new byte[8];
        int n = request.getInputStream().read(buf, 2, 4);
        assertEquals(4, n);
        assertEquals("abcd", new String(buf, 2, 4, StandardCharsets.UTF_8));
        assertEquals(1, bulkReadCalls.get());
        assertEquals(0, singleByteReadCalls.get());
    }

    private static final class StubHttpChannel implements HttpChannel {
        private final SocketAddress remoteAddress;
        private final SocketAddress localAddress;

        private StubHttpChannel(SocketAddress remoteAddress, SocketAddress localAddress) {
            this.remoteAddress = remoteAddress;
            this.localAddress = localAddress;
        }

        @Override
        public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public HttpOutputMessage newOutputMessage() {
            return null;
        }

        @Override
        public SocketAddress remoteAddress() {
            return remoteAddress;
        }

        @Override
        public SocketAddress localAddress() {
            return localAddress;
        }

        @Override
        public void flush() {}
    }
}
