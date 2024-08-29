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
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.rpc.model.FrameworkModel;

public class Http2ServerUnaryChannelObserver extends Http2ServerCallToObserverAdapter {

    private static final FluentLogger LOGGER = FluentLogger.of(Http2ServerUnaryChannelObserver.class);

    public Http2ServerUnaryChannelObserver(FrameworkModel frameworkModel, H2StreamChannel h2StreamChannel) {
        super(frameworkModel, h2StreamChannel);
    }

    @Override
    protected void doOnNext(Object data) throws Throwable {
        HttpOutputMessage httpOutputMessage = buildMessage(data);
        sendHeader(buildMetadata(resolveStatusCode(data), data, httpOutputMessage));
        sendMessage(httpOutputMessage);
    }

    @Override
    protected void doOnError(Throwable throwable) throws Throwable {
        String statusCode = resolveStatusCode(throwable);
        Object data = buildErrorResponse(statusCode, throwable);
        HttpOutputMessage httpOutputMessage;
        try {
            httpOutputMessage = buildMessage(data);
        } catch (Throwable t) {
            LOGGER.internalError("Failed to build message", t);
            httpOutputMessage = encodeHttpOutputMessage(data);
        }
        sendHeader(buildMetadata(statusCode, data, httpOutputMessage));
        sendMessage(httpOutputMessage);
    }

    @Override
    protected void doOnCompleted(Throwable throwable) {}

    @Override
    protected HttpOutputMessage encodeHttpOutputMessage(Object data) {
        return getHttpChannel().newOutputMessage(true);
    }
}
