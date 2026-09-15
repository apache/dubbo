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
package org.apache.dubbo.remoting.transport.netty4.ssl;

import javax.net.ssl.SSLHandshakeException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verify that TLS handlers properly close channels on exceptions,
 * preventing half-open connections that are TCP-alive but application-dead.
 */
class SslHandlerExceptionTest {

    @Test
    void serverTlsHandler_exceptionCaught_shouldCloseChannel() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);

        SslServerTlsHandler handler = new SslServerTlsHandler(null, true);
        NoClassDefFoundError error =
                new NoClassDefFoundError("Could not initialize class io.netty.buffer.PooledUnsafeDirectByteBuf");

        handler.exceptionCaught(ctx, error);

        verify(ctx).close();
    }

    @Test
    void clientTlsHandler_handshakeFailure_shouldCloseChannel() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);

        SslClientTlsHandler handler = new SslClientTlsHandler(mock(SslContext.class));
        SslHandshakeCompletionEvent failureEvent =
                new SslHandshakeCompletionEvent(new SSLHandshakeException("TLS handshake timeout"));

        handler.userEventTriggered(ctx, failureEvent);

        verify(ctx).close();
    }
}
