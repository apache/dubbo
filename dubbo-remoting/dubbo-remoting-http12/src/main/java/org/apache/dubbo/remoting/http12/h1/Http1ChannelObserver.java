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
import org.apache.dubbo.remoting.http12.HttpChannelObserver;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMessage;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.SimpleHttpMessage;
import org.apache.dubbo.remoting.http12.SimpleHttpMetadata;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Http1ChannelObserver implements HttpChannelObserver {

    private final HttpChannel httpChannel;

    private HttpMessageCodec httpMessageCodec;

    private boolean headerSent;

    public Http1ChannelObserver(HttpChannel httpChannel) {
        this.httpChannel = httpChannel;
    }

    public Http1ChannelObserver(HttpChannel httpChannel, HttpMessageCodec httpMessageCodec) {
        this.httpChannel = httpChannel;
        this.httpMessageCodec = httpMessageCodec;
    }

    public void setHttpMessageCodec(HttpMessageCodec httpMessageCodec) {
        this.httpMessageCodec = httpMessageCodec;
    }

    @Override
    public void onNext(Object data) {
        if (!headerSent) {
            HttpMetadata httpMetadata = encodeHttpHeaders(data);
            this.httpChannel.writeHeader(httpMetadata);
            headerSent = true;
        }
        try {
            HttpMessage httpMessage = encodeHttpMessage(data);
            prepareWriteMessage(httpMessage);
            this.httpChannel.writeMessage(httpMessage);
            postWriteMessage(httpMessage);
        } catch (IOException e) {
            onError(e);
        }
    }

    protected HttpMetadata encodeHttpHeaders(Object data) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE.getName(), httpMessageCodec.contentType().getName());
        httpHeaders.set(HttpHeaderNames.TRANSFER_ENCODING.getName(), "chunked");
        return new SimpleHttpMetadata(httpHeaders);
    }

    protected HttpMessage encodeHttpMessage(Object data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        this.httpMessageCodec.encode(bos, data);
        byte[] dataBytes = bos.toByteArray();
        return new SimpleHttpMessage(new ByteArrayInputStream(dataBytes));
    }

    protected void postWriteMessage(HttpMessage httpMessage) {
    }

    protected void prepareWriteMessage(HttpMessage httpMessage) {
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onCompleted() {
        this.getHttpChannel().writeMessage(HttpMessage.EMPTY_MESSAGE);
    }

    @Override
    public HttpChannel getHttpChannel() {
        return this.httpChannel;
    }
}
