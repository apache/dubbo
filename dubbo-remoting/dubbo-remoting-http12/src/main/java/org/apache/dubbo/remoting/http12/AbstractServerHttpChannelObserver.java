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

    private final HttpChannel httpChannel;

    private HeadersCustomizer headersCustomizer = HeadersCustomizer.NO_OP;

    private TrailersCustomizer trailersCustomizer = TrailersCustomizer.NO_OP;

    private ErrorResponseCustomizer errorResponseCustomizer = ErrorResponseCustomizer.NO_OP;

    private HttpMessageEncoder responseEncoder;

    private String altSvc;

    private boolean headerSent;

    private boolean completed;

    private boolean closed;

    protected AbstractServerHttpChannelObserver(HttpChannel httpChannel) {
        this.httpChannel = httpChannel;
    }

    @Override
    public HttpChannel getHttpChannel() {
        return httpChannel;
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

    public void setAltSvc(String altSvc) {
        this.altSvc = altSvc;
    }

    public HttpMessageEncoder getResponseEncoder() {
        return responseEncoder;
    }

    public void setResponseEncoder(HttpMessageEncoder responseEncoder) {
        this.responseEncoder = responseEncoder;
    }

    @Override
    public final void onNext(Object data) {
        if (closed) {
            return;
        }
        try {
            doOnNext(data);
        } catch (Throwable e) {
            onError(e);
        }
    }

    protected void doOnNext(Object data) throws Throwable {
        if (!headerSent) {
            sendHeader(buildMetadata(resolveStatusCode(data), data, null));
        }
        sendMessage(buildMessage(data));
    }

    @Override
    public final void onError(Throwable throwable) {
        if (closed) {
            return;
        }
        if (throwable instanceof HttpResultPayloadException) {
            onNext(((HttpResultPayloadException) throwable).getResult());
            onCompleted(null);
            return;
        }
        try {
            doOnError(throwable);
        } catch (Throwable ex) {
            throwable = new EncodeException(ex);
        } finally {
            onCompleted(throwable);
        }
    }

    protected void doOnError(Throwable throwable) throws Throwable {
        String statusCode = resolveStatusCode(throwable);
        Object data = buildErrorResponse(statusCode, throwable);
        if (!headerSent) {
            sendHeader(buildMetadata(statusCode, data, null));
        }
        sendMessage(buildMessage(data));
    }

    @Override
    public final void onCompleted() {
        if (closed) {
            return;
        }
        onCompleted(null);
    }

    private void onCompleted(Throwable throwable) {
        if (!completed) {
            doOnCompleted(throwable);
            completed = true;
        }
    }

    protected void doOnCompleted(Throwable throwable) {
        HttpMetadata httpMetadata = encodeTrailers(throwable);
        if (httpMetadata == null) {
            return;
        }
        if (!headerSent) {
            HttpHeaders headers = httpMetadata.headers();
            headers.set(HttpHeaderNames.STATUS.getName(), resolveStatusCode(throwable));
            headers.set(HttpHeaderNames.CONTENT_TYPE.getName(), responseEncoder.contentType());
        }
        trailersCustomizer.accept(httpMetadata.headers(), throwable);
        getHttpChannel().writeHeader(httpMetadata);
    }

    protected HttpMetadata encodeTrailers(Throwable throwable) {
        return null;
    }

    protected HttpOutputMessage encodeHttpOutputMessage(Object data) {
        return getHttpChannel().newOutputMessage();
    }

    protected abstract HttpMetadata encodeHttpMetadata();

    protected void preOutputMessage(HttpOutputMessage outputMessage) throws Throwable {}

    protected void postOutputMessage(HttpOutputMessage outputMessage) throws Throwable {}

    protected void preMetadata(HttpMetadata httpMetadata, HttpOutputMessage outputMessage) {}

    protected final String resolveStatusCode(Object data) {
        return data instanceof HttpResult
                ? String.valueOf(((HttpResult<?>) data).getStatus())
                : HttpStatus.OK.getStatusString();
    }

    protected final String resolveStatusCode(Throwable throwable) {
        return throwable instanceof HttpStatusException
                ? String.valueOf(((HttpStatusException) throwable).getStatusCode())
                : HttpStatus.INTERNAL_SERVER_ERROR.getStatusString();
    }

    protected final ErrorResponse buildErrorResponse(String statusCode, Throwable throwable) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(statusCode);
        errorResponse.setMessage(throwable.getMessage());
        errorResponseCustomizer.accept(errorResponse, throwable);
        return errorResponse;
    }

    protected final HttpOutputMessage buildMessage(Object data) throws Throwable {
        if (data instanceof HttpResult) {
            data = ((HttpResult<?>) data).getBody();
        }
        HttpOutputMessage outputMessage = encodeHttpOutputMessage(data);
        try {
            preOutputMessage(outputMessage);
            responseEncoder.encode(outputMessage.getBody(), data);
        } catch (Throwable t) {
            outputMessage.close();
            throw t;
        }
        return outputMessage;
    }

    protected final void sendMessage(HttpOutputMessage outputMessage) throws Throwable {
        getHttpChannel().writeMessage(outputMessage);
        postOutputMessage(outputMessage);
    }

    protected final HttpMetadata buildMetadata(String statusCode, Object data, HttpOutputMessage httpOutputMessage) {
        HttpMetadata httpMetadata = encodeHttpMetadata();
        HttpHeaders headers = httpMetadata.headers();
        headers.set(HttpHeaderNames.STATUS.getName(), statusCode);
        headers.set(HttpHeaderNames.CONTENT_TYPE.getName(), responseEncoder.contentType());
        if (altSvc != null) {
            headers.set(HttpHeaderNames.ALT_SVC.getName(), altSvc);
        }
        if (data instanceof HttpResult) {
            HttpResult<?> result = (HttpResult<?>) data;
            if (result.getHeaders() != null) {
                headers.putAll(result.getHeaders());
            }
        }
        preMetadata(httpMetadata, httpOutputMessage);
        headersCustomizer.accept(headers);
        return httpMetadata;
    }

    protected final void sendHeader(HttpMetadata httpMetadata) {
        getHttpChannel().writeHeader(httpMetadata);
        headerSent = true;
    }

    @Override
    public void close() throws Exception {
        closed();
    }

    protected final void closed() {
        closed = true;
    }
}
