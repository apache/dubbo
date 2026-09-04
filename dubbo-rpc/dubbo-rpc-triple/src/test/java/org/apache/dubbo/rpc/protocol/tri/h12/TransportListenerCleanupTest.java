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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.RpcInvocationBuildContext;
import org.apache.dubbo.rpc.protocol.tri.TripleConstants;
import org.apache.dubbo.rpc.protocol.tri.h12.http1.DefaultHttp11ServerTransportListener;
import org.apache.dubbo.rpc.protocol.tri.h12.http2.GenericHttp2ServerTransportListener;
import org.apache.dubbo.rpc.protocol.tri.test.MockH2StreamChannel;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransportListenerCleanupTest {

    private static final URL TEST_URL = URL.valueOf("tri://127.0.0.1:20880");

    @Test
    void closesRequestWhenHttp11TransportFails() throws ReflectiveOperationException {
        FrameworkModel frameworkModel = new FrameworkModel();
        try {
            HttpChannel httpChannel = mock(HttpChannel.class);
            when(httpChannel.writeHeader(any())).thenReturn(CompletableFuture.completedFuture(null));
            when(httpChannel.writeMessage(any())).thenReturn(CompletableFuture.completedFuture(null));
            TestHttp11ServerTransportListener listener =
                    new TestHttp11ServerTransportListener(httpChannel, frameworkModel);
            HttpRequest request = mock(HttpRequest.class);
            setRequestContext(listener, request);

            listener.reportError(new RuntimeException("test"));

            verify(request).close();
        } finally {
            frameworkModel.destroy();
        }
    }

    @Test
    void closesRequestWhenHttp2TransportEndsBeforeCallStarts() {
        FrameworkModel frameworkModel = new FrameworkModel();
        try {
            GenericHttp2ServerTransportListener cancelledListener =
                    new GenericHttp2ServerTransportListener(new MockH2StreamChannel(), TEST_URL, frameworkModel);
            cancelledListener.cancelByRemote(1);

            GenericHttp2ServerTransportListener closedListener =
                    new GenericHttp2ServerTransportListener(new MockH2StreamChannel(), TEST_URL, frameworkModel);
            closedListener.close();
        } finally {
            frameworkModel.destroy();
        }
    }

    @Test
    void supportsDefaultRequestClose() {
        HttpRequest request = mock(HttpRequest.class, CALLS_REAL_METHODS);

        assertDoesNotThrow(request::close);
    }

    private static void setRequestContext(AbstractServerTransportListener<?, ?> listener, HttpRequest request)
            throws ReflectiveOperationException {
        RpcInvocationBuildContext context = mock(RpcInvocationBuildContext.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(TripleConstants.HTTP_REQUEST_KEY, request);
        when(context.getAttributes()).thenReturn(attributes);

        Field contextField = AbstractServerTransportListener.class.getDeclaredField("context");
        contextField.setAccessible(true);
        contextField.set(listener, context);
    }

    private static class TestHttp11ServerTransportListener extends DefaultHttp11ServerTransportListener {

        TestHttp11ServerTransportListener(HttpChannel httpChannel, FrameworkModel frameworkModel) {
            super(httpChannel, TEST_URL, frameworkModel);
        }

        void reportError(Throwable throwable) {
            onError(throwable);
        }
    }
}
