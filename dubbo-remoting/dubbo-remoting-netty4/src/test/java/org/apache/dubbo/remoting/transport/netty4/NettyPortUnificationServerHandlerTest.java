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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.api.ProtocolDetector;
import org.apache.dubbo.remoting.api.WireProtocol;
import org.apache.dubbo.remoting.api.pu.ChannelOperator;
import org.apache.dubbo.remoting.api.ssl.ContextOperator;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class NettyPortUnificationServerHandlerTest {

    @Test
    void shouldDetectPlaintextProtocolWhenAuthPolicyIsNone() {
        AtomicInteger detectCount = new AtomicInteger();
        FrameworkModel frameworkModel = new FrameworkModel();
        try {
            ApplicationModel applicationModel = frameworkModel.newApplication();
            URL url = URL.valueOf("dubbo://127.0.0.1:20880?codec=default&pu.test.cert=true")
                    .setScopeModel(applicationModel);
            ChannelHandler handler = mock(ChannelHandler.class);
            WireProtocol protocol = new CountingWireProtocol(detectCount);
            NettyPortUnificationServerHandler puHandler = new NettyPortUnificationServerHandler(
                    url,
                    true,
                    Collections.singletonMap("dubbo", protocol),
                    handler,
                    Collections.singletonMap("dubbo", url),
                    Collections.emptyMap());
            EmbeddedChannel channel = new EmbeddedChannel(puHandler);

            channel.writeInbound(Unpooled.wrappedBuffer(new byte[] {(byte) 0xda, (byte) 0xbb, 0, 0, 0}));

            assertEquals(1, detectCount.get());
            assertTrue(channel.isOpen());
            channel.finishAndReleaseAll();
        } finally {
            frameworkModel.destroy();
        }
    }

    private static final class CountingWireProtocol implements WireProtocol {
        private final AtomicInteger detectCount;

        private CountingWireProtocol(AtomicInteger detectCount) {
            this.detectCount = detectCount;
        }

        @Override
        public ProtocolDetector detector() {
            return in -> {
                detectCount.incrementAndGet();
                return ProtocolDetector.Result.needMoreData();
            };
        }

        @Override
        public void configServerProtocolHandler(URL url, ChannelOperator operator) {}

        @Override
        public void configClientPipeline(URL url, ChannelOperator operator, ContextOperator contextOperator) {}

        @Override
        public void close() {}
    }
}
