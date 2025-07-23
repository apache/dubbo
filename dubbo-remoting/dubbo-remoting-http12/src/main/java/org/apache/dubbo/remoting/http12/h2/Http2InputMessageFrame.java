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

import org.apache.dubbo.common.utils.ClassUtils;

import java.io.InputStream;

public final class Http2InputMessageFrame implements Http2InputMessage {

    private final long streamId;

    private final InputStream body;

    private final boolean endStream;

    public Http2InputMessageFrame(InputStream body, boolean endStream) {
        this(-1L, body, endStream);
    }

    public Http2InputMessageFrame(long streamId, InputStream body, boolean endStream) {
        this.streamId = streamId;
        this.body = body;
        this.endStream = endStream;
    }

    @Override
    public InputStream getBody() {
        return body;
    }

    @Override
    public String name() {
        return "DATA";
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
        return "Http2InputMessageFrame{body=" + ClassUtils.toShortString(body) + ", body=" + streamId + ", endStream="
                + endStream + '}';
    }
}
