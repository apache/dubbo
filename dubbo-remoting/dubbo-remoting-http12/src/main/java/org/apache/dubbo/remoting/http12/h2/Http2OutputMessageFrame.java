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

import java.io.IOException;
import java.io.OutputStream;

import io.netty.buffer.ByteBufOutputStream;

public class Http2OutputMessageFrame implements Http2OutputMessage {

    private final OutputStream body;

    private final boolean endStream;

    public Http2OutputMessageFrame(OutputStream body) {
        this(body, false);
    }

    public Http2OutputMessageFrame(boolean endStream) {
        this(null, endStream);
    }

    public Http2OutputMessageFrame(OutputStream body, boolean endStream) {
        this.body = body;
        this.endStream = endStream;
    }

    @Override
    public OutputStream getBody() {
        return body;
    }

    @Override
    public void close() throws IOException {
        if (body instanceof ByteBufOutputStream) {
            ((ByteBufOutputStream) body).buffer().release();
        }
        body.close();
    }

    @Override
    public boolean isEndStream() {
        return endStream;
    }
}
