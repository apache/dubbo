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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.exception.HttpOverPayloadException;

import java.io.OutputStream;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;

public class Http1ServerUnaryChannelObserver extends Http1ServerChannelObserver {

    private static final ErrorTypeAwareLogger log =
            LoggerFactory.getErrorTypeAwareLogger(Http1ServerUnaryChannelObserver.class);

    private static final FullHttpResponse RESPONSE_TOO_LARGE = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, new HttpResponseStatus(500, "Response Entity Too Large"), Unpooled.EMPTY_BUFFER);

    static {
        RESPONSE_TOO_LARGE.headers().set(CONTENT_LENGTH, 0);
    }

    public Http1ServerUnaryChannelObserver(HttpChannel httpChannel) {
        super(httpChannel);
    }

    @Override
    protected void doOnNext(Object data) throws Throwable {
        HttpOutputMessage httpOutputMessage = buildMessage(data);
        sendHeader(buildMetadata(resolveStatusCode(data), data, httpOutputMessage));
        sendMessage(httpOutputMessage);
    }

    @Override
    protected void doOnError(Throwable throwable) throws Throwable {
        if (throwable instanceof HttpOverPayloadException) {
            handleOverPayloadMessage((HttpOverPayloadException) throwable);
            return;
        }
        String statusCode = resolveStatusCode(throwable);
        Object data = buildErrorResponse(statusCode, throwable);
        HttpOutputMessage httpOutputMessage = buildMessage(data);
        sendHeader(buildMetadata(statusCode, data, httpOutputMessage));
        sendMessage(httpOutputMessage);
    }

    @Override
    protected void preMetadata(HttpMetadata httpMetadata, HttpOutputMessage outputMessage) {
        OutputStream body = outputMessage.getBody();
        if (body instanceof ByteBufOutputStream) {
            int contentLength = ((ByteBufOutputStream) body).writtenBytes();
            httpMetadata.headers().set(HttpHeaderNames.CONTENT_LENGTH.getName(), String.valueOf(contentLength));
        }
    }

    protected void handleOverPayloadMessage(HttpOverPayloadException exception) {
        getHttpChannel().writeFullHttpMessage(RESPONSE_TOO_LARGE).whenComplete((unused, throwable) -> {
            if (throwable != null && log.isDebugEnabled()) {
                log.debug("Failed to send a 500 Response Entity Too Large.", throwable);
            }
        });
    }
}
