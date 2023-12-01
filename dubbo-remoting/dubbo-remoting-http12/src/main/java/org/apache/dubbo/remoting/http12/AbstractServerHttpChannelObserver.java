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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodecFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.List;

public abstract class AbstractServerHttpChannelObserver implements CustomizableHttpChannelObserver<Object> {

    private HeadersCustomizer headersCustomizer = HeadersCustomizer.NO_OP;

    private TrailersCustomizer trailersCustomizer = TrailersCustomizer.NO_OP;

    private ErrorResponseCustomizer errorResponseCustomizer = ErrorResponseCustomizer.NO_OP;

    private final HttpChannel httpChannel;

    private boolean headerSent;

    private HttpMessageCodec responseEncoder;

    public AbstractServerHttpChannelObserver(HttpChannel httpChannel) {
        this.httpChannel = httpChannel;
    }

    public void setResponseEncoder(HttpMessageCodec responseEncoder) {
        this.responseEncoder = responseEncoder;
    }

    public HttpMessageCodec getResponseEncoder() {
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
            if (!headerSent) {
                doSendHeaders(HttpStatus.OK.getStatusString());
            }
            HttpOutputMessage outputMessage = encodeHttpOutputMessage(data);
            preOutputMessage(outputMessage);
            this.responseEncoder.encode(outputMessage.getBody(), data);
            getHttpChannel().writeMessage(outputMessage);
            postOutputMessage(outputMessage);
        } catch (Throwable e) {
            onError(e);
        }
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
        int httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
        if (throwable instanceof HttpStatusException) {
            httpStatusCode = ((HttpStatusException) throwable).getStatusCode();
        }
        if (!headerSent) {
            doSendHeaders(String.valueOf(httpStatusCode));
        }
        try {
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setStatus(String.valueOf(httpStatusCode));
            errorResponse.setMessage(throwable.getMessage());
            this.errorResponseCustomizer.accept(errorResponse, throwable);
            HttpOutputMessage httpOutputMessage = encodeHttpOutputMessage(errorResponse);
            this.responseEncoder.encode(httpOutputMessage.getBody(), errorResponse);
            getHttpChannel().writeMessage(httpOutputMessage);
        } catch (Throwable ex) {
            throwable = new EncodeException(ex);
        } finally {
            doOnCompleted(throwable);
        }
    }

    @Override
    public void onCompleted() {
        doOnCompleted(null);
    }

    @Override
    public HttpChannel getHttpChannel() {
        return httpChannel;
    }

    private void doSendHeaders(String statusCode) {
        HttpMetadata httpMetadata = encodeHttpMetadata();
        httpMetadata.headers().set(HttpHeaderNames.STATUS.getName(), statusCode);
        httpMetadata
                .headers()
                .set(
                        HttpHeaderNames.CONTENT_TYPE.getName(),
                        responseEncoder.mediaType().getName());
        this.headersCustomizer.accept(httpMetadata.headers());
        getHttpChannel().writeHeader(httpMetadata);
        this.headerSent = true;
    }

    protected void doOnCompleted(Throwable throwable) {
        HttpMetadata httpMetadata = encodeTrailers(throwable);
        if (httpMetadata == null) {
            return;
        }
        this.trailersCustomizer.accept(httpMetadata.headers(), throwable);
        getHttpChannel().writeHeader(httpMetadata);
    }

    public void findAndSetEncoder(URL url, String acceptEncoding, FrameworkModel frameworkModel) {
        List<HttpMessageCodecFactory> factories =
                frameworkModel.getExtensionLoader(HttpMessageCodecFactory.class).getActivateExtensions();
        this.responseEncoder = findCodecFactoryForEncode(acceptEncoding, factories).createCodec(url, frameworkModel, acceptEncoding);
    }

    public HttpMessageCodecFactory findCodecFactoryForEncode(
            String acceptEncoding, List<HttpMessageCodecFactory> candidates) {
        for (HttpMessageCodecFactory factory : candidates) {
            if (factory.codecSupport().supportEncode(acceptEncoding)) {
                return factory;
            }
        }
        throw new DecodeException("Can not find encoder for:" + acceptEncoding);
    }
}
