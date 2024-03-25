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

import org.apache.dubbo.remoting.http12.ErrorResponse;
import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.exception.HttpResultPayloadException;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBufOutputStream;

public class Http1ServerUnaryChannelObserver extends Http1ServerChannelObserver {

    public Http1ServerUnaryChannelObserver(HttpChannel httpChannel) {
        super(httpChannel);
    }

    @Override
    public void onNext(Object data) {
        try {
            String status = HttpStatus.OK.getStatusString();
            Map<String, List<String>> additionalHeaders = null;
            if (data instanceof HttpResult) {
                HttpResult<?> result = (HttpResult<?>) data;
                data = result.getBody();
                status = String.valueOf(result.getStatus());
                additionalHeaders = result.getHeaders();
            }
            HttpOutputMessage outputMessage = encodeHttpOutputMessage(data);
            preOutputMessage(outputMessage);
            getResponseEncoder().encode(outputMessage.getBody(), data);
            if (!headerSent) {
                doSendHeaders(status, buildAdditionalHeaders(outputMessage, additionalHeaders));
            }
            getHttpChannel().writeMessage(outputMessage);
            postOutputMessage(outputMessage);
        } catch (Throwable e) {
            onError(e);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (throwable instanceof HttpResultPayloadException) {
            onNext(((HttpResultPayloadException) throwable).getResult());
            return;
        }
        int httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
        if (throwable instanceof HttpStatusException) {
            httpStatusCode = ((HttpStatusException) throwable).getStatusCode();
        }
        try {
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setStatus(String.valueOf(httpStatusCode));
            errorResponse.setMessage(throwable.getMessage());
            getErrorResponseCustomizer().accept(errorResponse, throwable);
            HttpOutputMessage httpOutputMessage = encodeHttpOutputMessage(errorResponse);
            getResponseEncoder().encode(httpOutputMessage.getBody(), errorResponse);
            if (!headerSent) {
                doSendHeaders(String.valueOf(httpStatusCode), buildAdditionalHeaders(httpOutputMessage, null));
            }
            getHttpChannel().writeMessage(httpOutputMessage);
        } catch (Throwable ex) {
            throwable = new EncodeException(ex);
        } finally {
            doOnCompleted(throwable);
        }
    }

    private Map<String, List<String>> buildAdditionalHeaders(
            HttpOutputMessage outputMessage, Map<String, List<String>> additionalHeaders) {
        int contentLength = ((ByteBufOutputStream) outputMessage.getBody()).writtenBytes();
        if (additionalHeaders == null) {
            additionalHeaders = new HashMap<>();
        }
        additionalHeaders.put(
                HttpHeaderNames.CONTENT_LENGTH.getName(), Collections.singletonList(String.valueOf(contentLength)));
        return additionalHeaders;
    }
}
