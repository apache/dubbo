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
package org.apache.dubbo.rpc.protocol.tri.h12.http1;

import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;

import java.io.ByteArrayOutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;

public final class Http1UnaryServerChannelObserver<OUTPUT> extends Http1ServerChannelObserver<OUTPUT> {

    public Http1UnaryServerChannelObserver(
            FrameworkModel frameworkModel, HttpChannel<OUTPUT> httpChannel, Class<OUTPUT> outputType) {
        super(frameworkModel, httpChannel, outputType);
    }

    @Override
    protected void doOnNext(Object data) throws Throwable {
        int statusCode = resolveStatusCode(data);
        HttpOutputMessage<OUTPUT> message = buildMessage(statusCode, data);
        sendMetadata(buildMetadata(statusCode, data, null, message));
        sendMessage(message);
    }

    @Override
    protected void doOnError(Throwable throwable) throws Throwable {
        int statusCode = resolveErrorStatusCode(throwable);
        Object data = buildErrorResponse(statusCode, throwable);
        HttpOutputMessage<OUTPUT> message = buildMessage(statusCode, data);
        sendMetadata(buildMetadata(statusCode, data, throwable, message));
        sendMessage(message);
    }

    @Override
    protected void customizeHeaders(HttpHeaders headers, Throwable throwable, HttpOutputMessage<OUTPUT> message) {
        super.customizeHeaders(headers, throwable, message);
        int contentLength = 0;
        if (message != null) {
            OUTPUT body = message.getBody();
            if (body instanceof ByteBuf) {
                contentLength = ((ByteBuf) body).readableBytes();
            } else if (body instanceof ByteBufOutputStream) {
                contentLength = ((ByteBufOutputStream) body).writtenBytes();
            } else if (body instanceof ByteArrayOutputStream) {
                contentLength = ((ByteArrayOutputStream) body).size();
            } else {
                throw new IllegalArgumentException("Unsupported body type: " + body.getClass());
            }
        }
        headers.set(HttpHeaderNames.CONTENT_LENGTH.getKey(), String.valueOf(contentLength));
    }

    @Override
    protected Throwable customizeError(Throwable throwable) {
        throwable = super.customizeError(throwable);
        if (throwable == null) {
            doOnCompleted(null);
        }
        return throwable;
    }

    @Override
    protected String getDisplayMessage(Throwable throwable) {
        return TripleProtocol.VERBOSE_ENABLED
                ? ExceptionUtils.buildVerboseMessage(throwable)
                : super.getDisplayMessage(throwable);
    }
}
