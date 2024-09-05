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
package org.apache.dubbo.remoting.http12.h2;

import org.apache.dubbo.remoting.http12.HttpHeaders;

import io.netty.handler.codec.http2.Http2Headers.PseudoHeaderName;

public final class Http2MetadataFrame implements Http2Header {

    private final long streamId;

    private final HttpHeaders headers;

    private final boolean endStream;

    public Http2MetadataFrame(HttpHeaders headers) {
        this(-1L, headers, false);
    }

    public Http2MetadataFrame(HttpHeaders headers, boolean endStream) {
        this(-1L, headers, endStream);
    }

    public Http2MetadataFrame(long streamId, HttpHeaders headers, boolean endStream) {
        this.streamId = streamId;
        this.headers = headers;
        this.endStream = endStream;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public String method() {
        return headers.getFirst(PseudoHeaderName.METHOD.value());
    }

    @Override
    public String path() {
        return headers.getFirst(PseudoHeaderName.PATH.value());
    }

    @Override
    public long id() {
        return streamId;
    }

    @Override
    public boolean isEndStream() {
        return endStream;
    }

    @Override
    public String toString() {
        return "Http2MetadataFrame{method='" + method() + '\'' + ", path='" + path() + '\'' + ", contentType='"
                + contentType() + "', streamId=" + streamId + ", endStream="
                + endStream + '}';
    }
}
