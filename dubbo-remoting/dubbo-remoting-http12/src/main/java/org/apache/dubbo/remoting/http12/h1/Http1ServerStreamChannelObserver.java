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

import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpMessage;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.SimpleHttpMessage;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.io.ByteArrayInputStream;

public class Http1ServerStreamChannelObserver extends Http1ChannelObserver {

    private static final String SERVER_SENT_EVENT_DATA_PREFIX = "data:";
    private static final String SERVER_SENT_EVENT_LF = "\n\n";

    private static final byte[] SERVER_SENT_EVENT_DATA_PREFIX_BYTES = SERVER_SENT_EVENT_DATA_PREFIX.getBytes();
    private static final byte[] SERVER_SENT_EVENT_LF_BYTES = SERVER_SENT_EVENT_LF.getBytes();

    public Http1ServerStreamChannelObserver(HttpChannel httpChannel) {
        super(httpChannel);
    }

    public Http1ServerStreamChannelObserver(HttpChannel httpChannel, HttpMessageCodec httpMessageCodec) {
        super(httpChannel, httpMessageCodec);
    }

    @Override
    protected HttpMetadata encodeHttpHeaders(Object data) {
        HttpMetadata httpMetadata = super.encodeHttpHeaders(data);
        httpMetadata.headers().set(HttpHeaderNames.CONTENT_TYPE.getName(), MediaType.TEXT_EVENT_STREAM_VALUE.getName());
        return httpMetadata;
    }

    @Override
    protected void prepareWriteMessage(HttpMessage httpMessage) {
        this.getHttpChannel().writeMessage(new SimpleHttpMessage(new ByteArrayInputStream(SERVER_SENT_EVENT_DATA_PREFIX_BYTES)));
    }

    @Override
    protected void postWriteMessage(HttpMessage httpMessage) {
        this.getHttpChannel().writeMessage(new SimpleHttpMessage(new ByteArrayInputStream(SERVER_SENT_EVENT_LF_BYTES)));
    }
}
