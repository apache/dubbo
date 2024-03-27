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
package org.apache.dubbo.remoting.http12;

import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.exception.HttpResultPayloadException;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.message.HttpMessageEncoder;

public abstract class AbstractServerHttpChannelObserver implements CustomizableHttpChannelObserver<Object> {

    private HeadersCustomizer headersCustomizer = HeadersCustomizer.NO_OP;

    private TrailersCustomizer trailersCustomizer = TrailersCustomizer.NO_OP;

    private ErrorResponseCustomizer errorResponseCustomizer = ErrorResponseCustomizer.NO_OP;

    private final HttpChannel httpChannel;

    private boolean headerSent;

    private HttpMessageEncoder responseEncoder;

    protected AbstractServerHttpChannelObserver(HttpChannel httpChannel) {
        this.httpChannel = httpChannel;
    }

    public void setResponseEncoder(HttpMessageEncoder responseEncoder) {
        this.responseEncoder = responseEncoder;
    }

    public HttpMessageEncoder getResponseEncoder() {
        return responseEncoder;
    }

    @Override
    public void setHeadersCustomizer(HeadersCustomizer headersCustomizer) {
        this.headersCustomizer = headersCustomizer;
    }

    @Override
    public void setTrailersCustomizer(TrailersCustomizer trailersCustomizer) {
        this.trailersCustomizer = trailersCustomizer;
    }

    @Override
    public void setErrorResponseCustomizer(ErrorResponseCustomizer errorResponseCustomizer) {
        this.errorResponseCustomizer = errorResponseCustomizer;
    }

    protected HeadersCustomizer getHeadersCustomizer() {
        return headersCustomizer;
    }

    protected TrailersCustomizer getTrailersCustomizer() {
        return trailersCustomizer;
    }

    @Override
    public void onNext(Object data) {
        try {
            doOnNext(data);
        } catch (Throwable e) {
            onError(e);
        }
    }

    protected void doOnNext(Object data) throws Throwable {
        if (!headerSent) {
            HttpMetadata httpMetadata = buildMetadata(httpStatusCode(data, false));
            sendHeader(httpMetadata, data);
        }
        HttpOutputMessage outputMessage = encodeData(data);
        sendData(outputMessage);
    }

    protected void preOutputMessage(HttpOutputMessage outputMessage) throws Throwable {}

    protected void postOutputMessage(HttpOutputMessage outputMessage) throws Throwable {}

    protected abstract HttpMetadata encodeHttpMetadata();

    protected HttpOutputMessage encodeHttpOutputMessage(Object data) {
        return getHttpChannel().newOutputMessage();
    }

    protected HttpMetadata encodeTrailers(Throwable throwable) {
        return null;
    }

    @Override
    public void onError(Throwable throwable) {
        if (throwable instanceof HttpResultPayloadException) {
            onNext(((HttpResultPayloadException) throwable).getResult());
            return;
        }
        try {
            doOnError(throwable);
        } catch (Throwable ex) {
            throwable = new EncodeException(ex);
        } finally {
            doOnCompleted(throwable);
        }
    }

    protected void doOnError(Throwable throwable) throws Throwable {
        String httpStatusCode = httpStatusCode(throwable, true);
        if (!headerSent) {
            HttpMetadata httpMetadata = buildMetadata(httpStatusCode);
            sendHeader(httpMetadata, null);
        }
        HttpOutputMessage httpOutputMessage = encodeData(buildErrorResponse(httpStatusCode, throwable));
        sendData(httpOutputMessage);
    }

    @Override
    public void onCompleted() {
        doOnCompleted(null);
    }

    @Override
    public HttpChannel getHttpChannel() {
        return httpChannel;
    }

    protected HttpOutputMessage encodeData(Object data) throws Throwable {
        if (data instanceof HttpResult) {
            data = ((HttpResult<?>) data).getBody();
        }
        HttpOutputMessage outputMessage = encodeHttpOutputMessage(data);
        preOutputMessage(outputMessage);
        responseEncoder.encode(outputMessage.getBody(), data);
        return outputMessage;
    }

    protected void sendData(HttpOutputMessage outputMessage) throws Throwable {
        getHttpChannel().writeMessage(outputMessage);
        postOutputMessage(outputMessage);
    }

    protected void sendHeader(HttpMetadata httpMetadata, Object data) {
        if (data instanceof HttpResult) {
            HttpResult<?> result = (HttpResult<?>) data;
            if (result.getHeaders() != null) {
                httpMetadata.headers().putAll(result.getHeaders());
            }
        }
        getHttpChannel().writeHeader(httpMetadata);
        headerSent = true;
    }

    protected String httpStatusCode(Object data, boolean isError) {
        String httpStatusCode = HttpStatus.OK.getStatusString();
        if (data instanceof HttpResult) {
            httpStatusCode = String.valueOf(((HttpResult<?>) data).getStatus());
        } else if (isError) {
            Throwable throwable = (Throwable) data;
            int errorCode = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
            if (throwable instanceof HttpStatusException) {
                errorCode = ((HttpStatusException) throwable).getStatusCode();
            }
            httpStatusCode = String.valueOf(errorCode);
        }
        return httpStatusCode;
    }

    protected ErrorResponse buildErrorResponse(String statusCode, Throwable throwable) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(statusCode);
        errorResponse.setMessage(throwable.getMessage());
        errorResponseCustomizer.accept(errorResponse, throwable);
        return errorResponse;
    }

    protected HttpMetadata buildMetadata(String statusCode) {
        HttpMetadata httpMetadata = encodeHttpMetadata();
        HttpHeaders headers = httpMetadata.headers();
        headers.set(HttpHeaderNames.STATUS.getName(), statusCode);
        headers.set(HttpHeaderNames.CONTENT_TYPE.getName(), responseEncoder.contentType());
        headersCustomizer.accept(headers);
        return httpMetadata;
    }

    protected void doOnCompleted(Throwable throwable) {
        HttpMetadata httpMetadata = encodeTrailers(throwable);
        if (httpMetadata == null) {
            return;
        }
        trailersCustomizer.accept(httpMetadata.headers(), throwable);
        getHttpChannel().writeHeader(httpMetadata);
    }
}
