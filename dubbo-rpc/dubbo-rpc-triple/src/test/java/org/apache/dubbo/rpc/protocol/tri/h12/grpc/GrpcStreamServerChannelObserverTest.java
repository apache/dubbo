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

import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.h12.ExceptionCustomizerWrapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Verifies the HTTP and gRPC statuses sent by Triple observers.
 *
 * @date 2026-11-27
 */
class GrpcStreamServerChannelObserverTest {
    private final FrameworkModel frameworkModel = new FrameworkModel();
    private final H2StreamChannel channel = mock(H2StreamChannel.class);
    private final ExceptionCustomizerWrapper customizer = new ExceptionCustomizerWrapper(frameworkModel);

    @AfterEach
    void destroyFramework() {
        frameworkModel.destroy();
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 5, 16})
    void trailersOnlyErrorUsesHttpOk(int status) {
        GrpcUnaryServerChannelObserver observer = new GrpcUnaryServerChannelObserver(frameworkModel, channel);
        observer.addTrailersCustomizer(customizer::customizeGrpcStatus);

        observer.onError(
                TriRpcStatus.fromCode(status).withDescription("service error").asException());

        HttpMetadata metadata = captureMetadata(1).getValue();
        assertEquals("200", metadata.headers().getFirst(HttpHeaderNames.STATUS.getKey()));
        assertEquals(Integer.toString(status), metadata.headers().getFirst(GrpcHeaderNames.GRPC_STATUS.getName()));
    }

    @Test
    void errorAfterHeadersDoesNotRepeatHttpStatus() {
        TestStreamObserver observer = new TestStreamObserver(frameworkModel, channel);
        observer.addTrailersCustomizer(customizer::customizeGrpcStatus);
        observer.sendInitialHeaders();

        observer.onError(TriRpcStatus.fromCode(5).asException());

        HttpMetadata trailers = captureMetadata(2).getValue();
        assertNull(trailers.headers().getFirst(HttpHeaderNames.STATUS.getKey()));
        assertEquals("5", trailers.headers().getFirst(GrpcHeaderNames.GRPC_STATUS.getName()));
    }

    private ArgumentCaptor<HttpMetadata> captureMetadata(int calls) {
        ArgumentCaptor<HttpMetadata> captor = ArgumentCaptor.forClass(HttpMetadata.class);
        verify(channel, times(calls)).writeHeader(captor.capture());
        return captor;
    }

    private static final class TestStreamObserver extends GrpcStreamServerChannelObserver {
        TestStreamObserver(FrameworkModel frameworkModel, H2StreamChannel channel) {
            super(frameworkModel, channel);
        }

        void sendInitialHeaders() {
            HttpMetadata metadata = encodeHttpMetadata(false);
            metadata.headers().set(HttpHeaderNames.STATUS.getKey(), "200");
            sendMetadata(metadata);
        }
    }
}
