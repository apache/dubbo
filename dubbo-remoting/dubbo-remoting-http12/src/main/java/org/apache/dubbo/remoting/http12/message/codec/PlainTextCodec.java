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

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Activate
public class PlainTextCodec implements HttpMessageCodec {

    private final String contentType;

    public PlainTextCodec(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void encode(OutputStream outputStream, Object data) throws EncodeException {
        throw new EncodeException("PlainTextCodec does not support encode.");
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType) throws DecodeException {
        try {
            if (!String.class.equals(targetType)) {
                throw new DecodeException("Plain text content only supports String as method param.");
            }
            Charset charset;
            if (contentType.contains("charset=")) {
                try {
                    charset = Charset.forName(contentType.substring(contentType.indexOf("charset=") + 8));
                } catch (Exception e) {
                    throw new DecodeException("Unsupported charset:" + e.getMessage());
                }
                if (!charset.equals(StandardCharsets.UTF_8) && !charset.equals(StandardCharsets.US_ASCII)) {
                    String origin = toByteArrayStream(inputStream).toString(charset.name());
                    return new String(origin.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                }
            }
            return toByteArrayStream(inputStream).toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new DecodeException(e);
        }
    }

    @Override
    public MediaType mediaType() {
        return MediaType.TEXT_PLAIN;
    }
}
