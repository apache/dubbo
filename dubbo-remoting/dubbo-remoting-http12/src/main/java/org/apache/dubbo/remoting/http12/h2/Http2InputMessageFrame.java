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

import java.io.InputStream;

public class Http2InputMessageFrame implements Http2InputMessage {

    private long id;

    private final InputStream body;

    private final boolean endStream;

    public Http2InputMessageFrame(InputStream body) {
        this(body, false);
    }

    public Http2InputMessageFrame(boolean endStream) {
        this(null, endStream);
    }

    public Http2InputMessageFrame(InputStream body, boolean endStream) {
        this.body = body;
        this.endStream = endStream;
    }

    public void setId(long id) {
        this.id = id;
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
        return id;
    }

    @Override
    public boolean isEndStream() {
        return endStream;
    }
}
