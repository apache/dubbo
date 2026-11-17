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

import org.apache.dubbo.remoting.http12.ExceptionHandler;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.RequestMetadata;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;

import java.util.Collections;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersDecoder;
import io.netty.handler.codec.http2.DefaultHttp2HeadersEncoder;
import io.netty.handler.codec.http2.Http2Headers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that framework error trailers preserve Unicode through HPACK.
 * @date 2026-11-10
 */
class CompositeExceptionHandlerTest {
    @ParameterizedTest
    @ValueSource(strings = {"业务校验失败", "错误的token", "emoji 😀", "50% + plus", "%E4%B8%AD", "line1\nline2", "ascii error"})
    void preservesMessageAcrossHttp2Trailers(String message) throws Exception {
        verifyMessage(message, false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"业务校验失败", "50% + plus"})
    void preservesVerboseMessageAcrossHttp2Trailers(String message) throws Exception {
        verifyMessage(message, true);
    }

    @Test
    void leavesExtensionOwnedEncodedTrailersUnchanged() {
        FrameworkModel framework = mock(FrameworkModel.class);
        when(framework.getActivateExtensions(ExceptionHandler.class))
                .thenReturn(Collections.singletonList(new EncodedHandler()));
        HttpHeaders headers = HttpHeaders.create();
        new CompositeExceptionHandler(framework)
                .resolveGrpcStatus(new IllegalArgumentException("ignored"), headers, null, null);
        assertEquals("%E4%B8%AD", headers.getFirst("grpc-message"));
        assertEquals("3", headers.getFirst("grpc-status"));
    }

    @Test
    void handlesMissingExceptionMessage() throws Exception {
        verifyMessage(null, false);
    }

    private static class EncodedHandler implements ExceptionHandler<IllegalArgumentException, Object> {
        @Override
        public boolean resolveGrpcStatus(
                IllegalArgumentException error,
                HttpHeaders headers,
                RequestMetadata metadata,
                MethodDescriptor descriptor) {
            headers.set("grpc-status", "3");
            headers.set("grpc-message", "%E4%B8%AD");
            return true;
        }
    }

    private void verifyMessage(String message, boolean verbose) throws Exception {
        FrameworkModel framework = mock(FrameworkModel.class);
        when(framework.getActivateExtensions(ExceptionHandler.class)).thenReturn(Collections.emptyList());
        CompositeExceptionHandler handler = new CompositeExceptionHandler(framework);
        IllegalArgumentException error = new IllegalArgumentException(message);
        boolean previous = TripleProtocol.VERBOSE_ENABLED;
        try {
            TripleProtocol.VERBOSE_ENABLED = verbose;
            String expected = verbose ? ExceptionUtils.buildVerboseMessage(error) : (message == null ? "" : message);
            HttpHeaders headers = HttpHeaders.create();
            assertTrue(handler.resolveGrpcStatus(error, headers, null, null));
            String raw = headers.getFirst("grpc-message");
            assertTrue(raw.chars().allMatch(c -> c >= 0x20 && c <= 0x7e), "wire value must be printable ASCII");
            ByteBuf block = Unpooled.buffer();
            try {
                Http2Headers trailers = new DefaultHttp2Headers()
                        .set("grpc-status", headers.getFirst("grpc-status"))
                        .set("grpc-message", raw);
                new DefaultHttp2HeadersEncoder().encodeHeaders(1, trailers, block);
                Http2Headers received = new DefaultHttp2HeadersDecoder().decodeHeaders(1, block);
                assertEquals(
                        expected,
                        TriRpcStatus.decodeMessage(received.get("grpc-message").toString()));
                assertEquals(
                        headers.getFirst("grpc-status"),
                        received.get("grpc-status").toString());
            } finally {
                block.release();
            }
        } finally {
            TripleProtocol.VERBOSE_ENABLED = previous;
        }
    }
}
