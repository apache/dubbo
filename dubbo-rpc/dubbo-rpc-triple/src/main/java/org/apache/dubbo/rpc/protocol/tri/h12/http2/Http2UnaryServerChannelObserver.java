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
package org.apache.dubbo.rpc.protocol.tri.h12.http2;

import org.apache.dubbo.common.logger.FluentLogger;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2MetadataFrame;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpHeaders;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;

import io.netty.handler.codec.http2.DefaultHttp2Headers;

public class Http2UnaryServerChannelObserver extends Http2StreamServerChannelObserver {

    private static final FluentLogger LOGGER = FluentLogger.of(Http2UnaryServerChannelObserver.class);

    public Http2UnaryServerChannelObserver(FrameworkModel frameworkModel, H2StreamChannel h2StreamChannel) {
        super(frameworkModel, h2StreamChannel);
    }

    @Override
    protected void doOnNext(Object data) throws Throwable {
        int statusCode = resolveStatusCode(data);
        HttpOutputMessage message = buildMessage(statusCode, data);
        HttpMetadata metadata = buildMetadata(statusCode, data, null, message);
        customizeTrailers(metadata.headers(), null);
        sendMetadata(metadata);
        sendMessage(message);
    }

    @Override
    protected void doOnError(Throwable throwable) throws Throwable {
        int statusCode = resolveErrorStatusCode(throwable);
        Object data = buildErrorResponse(statusCode, throwable);
        HttpOutputMessage message;
        try {
            message = buildMessage(statusCode, data);
        } catch (Throwable t) {
            LOGGER.internalError("Failed to build message", t);
            message = encodeHttpOutputMessage(data);
        }
        HttpMetadata metadata = buildMetadata(statusCode, data, throwable, message);
        customizeTrailers(metadata.headers(), throwable);
        sendMetadata(metadata);
        sendMessage(message);
    }

    @Override
    protected void customizeTrailers(HttpHeaders headers, Throwable throwable) {
        StreamUtils.putHeaders(headers, getResponseAttachments(), TripleProtocol.CONVERT_NO_LOWER_HEADER);
        super.customizeTrailers(headers, throwable);
    }

    @Override
    protected void doOnCompleted(Throwable throwable) {}

    @Override
    protected HttpOutputMessage encodeHttpOutputMessage(Object data) {
        return getHttpChannel().newOutputMessage(true);
    }

    @Override
    protected HttpMetadata encodeHttpMetadata(boolean endStream) {
        return new Http2MetadataFrame(new NettyHttpHeaders<>(new DefaultHttp2Headers(false, 8)), endStream);
    }
}
