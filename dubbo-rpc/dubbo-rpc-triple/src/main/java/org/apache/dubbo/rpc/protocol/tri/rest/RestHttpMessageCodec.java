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
package org.apache.dubbo.rpc.protocol.tri.rest;

import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import java.io.InputStream;

public final class RestHttpMessageCodec implements HttpMessageDecoder {

    private final HttpRequest request;
    private final HttpResponse response;
    private final ParameterMeta[] parameters;
    private final ArgumentResolver argumentResolver;
    private final ArgumentConverter<?> argumentConverter;

    public RestHttpMessageCodec(
            HttpRequest request,
            HttpResponse response,
            ParameterMeta[] parameters,
            ArgumentResolver argumentResolver,
            ArgumentConverter<?> argumentConverter) {
        this.request = request;
        this.response = response;
        this.parameters = parameters;
        this.argumentResolver = argumentResolver;
        this.argumentConverter = argumentConverter;
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType) throws DecodeException {
        return decode(inputStream, new Class<?>[] {targetType});
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object[] decode(InputStream inputStream, Class<?>[] targetTypes) throws DecodeException {
        request.setInputStream(inputStream);
        int len = parameters.length;
        Object[] args = new Object[len];
        for (int i = 0; i < len; i++) {
            ParameterMeta parameter = parameters[i];
            Object arg = argumentResolver.resolve(parameter, request, response);
            if (arg == null || parameter.getType().isInstance(arg)) {
                args[i] = arg;
            } else {
                args[i] = argumentConverter.convert(arg, (Class) parameter.getType(), parameter);
            }
        }
        return args;
    }

    @Override
    public MediaType mediaType() {
        return MediaType.ALL_VALUE;
    }
}
