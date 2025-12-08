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

import org.apache.dubbo.remoting.http12.exception.EncodeException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ServerSentEventEncoderTest {

    static class DummyJsonEncoder implements HttpMessageEncoder {
        @Override
        public void encode(OutputStream outputStream, Object data, java.nio.charset.Charset charset)
                throws EncodeException {
            try {
                if (data instanceof byte[]) {
                    outputStream.write((byte[]) data);
                } else {
                    outputStream.write(String.valueOf(data).getBytes(charset));
                }
            } catch (Exception e) {
                throw new EncodeException("encode error", e);
            }
        }

        @Override
        public MediaType mediaType() {
            return MediaType.APPLICATION_JSON;
        }

        @Override
        public boolean supports(String mediaType) {
            return true;
        }
    }

    @Test
    void shouldUseTextEventStreamContentType() {
        ServerSentEventEncoder sse = new ServerSentEventEncoder(new DummyJsonEncoder());
        Assertions.assertEquals(MediaType.TEXT_EVENT_STREAM, sse.mediaType());
        Assertions.assertEquals(MediaType.TEXT_EVENT_STREAM.getName(), sse.contentType());
    }

    @Test
    void shouldEncodeServerSentEventFormat() {
        ServerSentEventEncoder sse = new ServerSentEventEncoder(new DummyJsonEncoder());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        sse.encode(
                bos,
                ServerSentEvent.builder()
                        .event("message")
                        .data("{\"a\":1}")
                        .id("1")
                        .build(),
                StandardCharsets.UTF_8);
        byte[] bytes = bos.toByteArray();
        String text = new String(bytes, StandardCharsets.UTF_8);
        Assertions.assertTrue(text.contains("id:1\n"));
        Assertions.assertTrue(text.contains("event:message\n"));
        Assertions.assertTrue(text.contains("data:{\"a\":1}\n"));
        Assertions.assertTrue(text.endsWith("\n"));
    }
}
