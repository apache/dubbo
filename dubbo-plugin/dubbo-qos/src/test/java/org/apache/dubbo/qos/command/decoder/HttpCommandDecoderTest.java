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
package org.apache.dubbo.qos.command.decoder;

import org.apache.dubbo.qos.command.CommandContext;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpCommandDecoderTest {
    @Test
    public void decodeGet() throws Exception {
        HttpRequest request = mock(HttpRequest.class);
        when(request.uri()).thenReturn("localhost:80/test");
        when(request.method()).thenReturn(HttpMethod.GET);
        CommandContext context = HttpCommandDecoder.decode(request);
        assertThat(context.getCommandName(), equalTo("test"));
        assertThat(context.isHttp(), is(true));
        when(request.uri()).thenReturn("localhost:80/test?a=b&c=d");
        context = HttpCommandDecoder.decode(request);
        assertThat(context.getArgs(), arrayContaining("b", "d"));
    }

    @Test
    public void decodePost() throws Exception {
        FullHttpRequest request = mock(FullHttpRequest.class);
        when(request.uri()).thenReturn("localhost:80/test");
        when(request.method()).thenReturn(HttpMethod.POST);
        when(request.headers()).thenReturn(HttpHeaders.EMPTY_HEADERS);
        ByteBuf buf = Unpooled.copiedBuffer("a=b&c=d", StandardCharsets.UTF_8);
        when(request.content()).thenReturn(buf);
        CommandContext context = HttpCommandDecoder.decode(request);
        assertThat(context.getCommandName(), equalTo("test"));
        assertThat(context.isHttp(), is(true));
        assertThat(context.getArgs(), arrayContaining("b", "d"));
    }
}
