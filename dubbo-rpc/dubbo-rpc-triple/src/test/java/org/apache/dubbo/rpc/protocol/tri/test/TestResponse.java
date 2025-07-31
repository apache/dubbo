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
package org.apache.dubbo.rpc.protocol.tri.test;

import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http2.Http2Headers.PseudoHeaderName;

import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("unchecked")
public class TestResponse {

    private final HttpHeaders headers;
    private final List<ByteBuf> byteBufs;
    private final HttpMessageDecoder decoder;

    private List<Object> bodies;

    public TestResponse(HttpHeaders headers, List<ByteBuf> byteBufs, HttpMessageDecoder decoder) {
        this.headers = headers;
        this.byteBufs = byteBufs;
        this.decoder = decoder;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public String getHeader(String name) {
        return headers.getFirst(name);
    }

    public int getStatus() {
        return Integer.parseInt(headers.getFirst(PseudoHeaderName.STATUS.value()));
    }

    public boolean isOk() {
        return getStatus() == 200;
    }

    public String getContentType() {
        return headers.getFirst(HttpHeaderNames.CONTENT_TYPE.getKey());
    }

    public <T> T getBody(Class<T> type) {
        if (type != String.class) {
            int status = getStatus();
            if (status >= 400) {
                List<String> bodies = getBodies(String.class);
                String message = bodies.isEmpty() ? null : bodies.get(0);
                throw new HttpStatusException(status, "body=" + message);
            }
        }
        List<T> bodies = getBodies(type);
        return bodies.isEmpty() ? null : bodies.get(0);
    }

    public <T> List<T> getBodies(Class<T> type) {
        List<T> bodies = (List<T>) this.bodies;
        if (bodies == null) {
            bodies = new ArrayList<>(byteBufs.size());
            boolean isTextEvent = MediaType.TEXT_EVENT_STREAM.getName().equals(getContentType());
            for (int i = 0, size = byteBufs.size(); i < size; i++) {
                ByteBuf byteBuf = byteBufs.get(i);
                if (isTextEvent) {
                    byte[] data = new byte[byteBuf.readableBytes()];
                    byteBuf.getBytes(byteBuf.readerIndex(), data);
                    String dataStr = new String(data, UTF_8);
                    if (dataStr.startsWith("data:")) {
                        String body = dataStr.substring(5, dataStr.length() - 2);
                        bodies.add((T) decoder.decode(new ByteArrayInputStream(body.getBytes(UTF_8)), type));
                    }
                    continue;
                }
                if (byteBuf.readableBytes() == 0) {
                    bodies.add(null);
                } else {
                    byte[] data = new byte[byteBuf.readableBytes()];
                    byteBuf.getBytes(byteBuf.readerIndex(), data);
                    bodies.add((T) decoder.decode(new ByteArrayInputStream(data), type));
                }
            }
            this.bodies = (List<Object>) bodies;
        }
        return bodies;
    }

    public String getValue() {
        return getBody(String.class);
    }

    public List<String> getValues() {
        return getBodies(String.class);
    }

    public Integer getIntValue() {
        return getBody(Integer.class);
    }

    public List<Integer> getIntValues() {
        return getBodies(Integer.class);
    }

    public Long getLongValue() {
        return getBody(Long.class);
    }

    public List<Long> getLongValues() {
        return getBodies(Long.class);
    }

    public void close() {
        if (byteBufs != null) {
            for (ByteBuf byteBuf : byteBufs) {
                if (byteBuf != null && byteBuf.refCnt() > 0) {
                    byteBuf.release();
                }
            }
        }
    }
}
