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
package org.apache.dubbo.remoting.http12.message.codec;

import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.remoting.http12.HttpJsonUtils;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public final class PlainTextCodec implements HttpMessageCodec {

    public static final PlainTextCodec INSTANCE = new PlainTextCodec();

    private final HttpJsonUtils httpJsonUtils;

    private PlainTextCodec() {
        this(FrameworkModel.defaultModel());
    }

    public PlainTextCodec(FrameworkModel frameworkModel) {
        httpJsonUtils = frameworkModel.getBeanFactory().getOrRegisterBean(HttpJsonUtils.class);
    }

    @Override
    public void encode(OutputStream os, Object data, Charset charset) throws EncodeException {
        if (data == null) {
            return;
        }
        try {
            if (data instanceof CharSequence) {
                os.write((data.toString()).getBytes(charset));
                return;
            }
            os.write(httpJsonUtils.toJson(data).getBytes(charset));
        } catch (HttpStatusException e) {
            throw e;
        } catch (Throwable t) {
            throw new EncodeException("Error encoding plain text", t);
        }
    }

    @Override
    public Object decode(InputStream is, Class<?> targetType, Charset charset) throws DecodeException {
        try {
            if (targetType == String.class) {
                return StreamUtils.toString(is, charset);
            }
        } catch (HttpStatusException e) {
            throw e;
        } catch (Throwable t) {
            throw new EncodeException("Error decoding plain text", t);
        }
        throw new DecodeException("'text/plain' media-type only supports String as method param.");
    }

    @Override
    public MediaType mediaType() {
        return MediaType.TEXT_PLAIN;
    }
}
