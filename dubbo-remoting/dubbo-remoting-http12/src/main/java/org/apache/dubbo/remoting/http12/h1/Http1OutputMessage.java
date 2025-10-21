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
package org.apache.dubbo.remoting.http12.h1;

import org.apache.dubbo.remoting.http12.HttpOutputMessage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;

public final class Http1OutputMessage<T> implements HttpOutputMessage<T> {

    private final T body;

    public Http1OutputMessage(T body) {
        this.body = body;
    }

    @Override
    public T getBody() {
        return body;
    }

    @Override
    public void close() throws Exception {
        if (body instanceof AutoCloseable) {
            ((AutoCloseable) body).close();
        }
        if (body instanceof ByteBufOutputStream) {
            ((ByteBufOutputStream) body).buffer().release();
        }
        if (body instanceof ByteBuf) {
            ((ByteBuf) body).release();
        }
    }
}
