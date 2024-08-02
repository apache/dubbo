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
package org.apache.dubbo.rpc.protocol.tri.h12;

import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

public final class HttpMessageDecoderWrapper implements HttpMessageDecoder {

    private final Charset charset;
    private final HttpMessageDecoder httpMessageDecoder;

    public HttpMessageDecoderWrapper(Charset charset, HttpMessageDecoder httpMessageDecoder) {
        this.charset = charset;
        this.httpMessageDecoder = httpMessageDecoder;
    }

    @Override
    public boolean supports(String mediaType) {
        return httpMessageDecoder.supports(mediaType);
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType, Charset charset) throws DecodeException {
        return httpMessageDecoder.decode(inputStream, targetType, this.charset);
    }

    @Override
    public Object decode(InputStream inputStream, Type targetType, Charset charset) throws DecodeException {
        return httpMessageDecoder.decode(inputStream, targetType, this.charset);
    }

    @Override
    public Object[] decode(InputStream inputStream, Class<?>[] targetTypes, Charset charset) throws DecodeException {
        return httpMessageDecoder.decode(inputStream, targetTypes, this.charset);
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType) throws DecodeException {
        return httpMessageDecoder.decode(inputStream, targetType, charset);
    }

    @Override
    public Object decode(InputStream inputStream, Type targetType) throws DecodeException {
        return httpMessageDecoder.decode(inputStream, targetType, charset);
    }

    @Override
    public Object[] decode(InputStream inputStream, Class<?>[] targetTypes) throws DecodeException {
        return httpMessageDecoder.decode(inputStream, targetTypes, charset);
    }

    @Override
    public MediaType mediaType() {
        return httpMessageDecoder.mediaType();
    }
}
