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
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class PlainTextCodec implements HttpMessageCodec {

    private final String contentType;

    public PlainTextCodec(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void encode(OutputStream outputStream, Object data) throws EncodeException {
        try {
            if (data instanceof String) {
                outputStream.write(((String) data).getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new EncodeException(e);
        }
        throw new EncodeException("PlainText media-type only supports String as return type.");
    }

    @Override
    public MediaType mediaType() {
        return MediaType.TEXT_PLAIN;
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType) throws DecodeException {
        try {
            if (!String.class.equals(targetType)) {
                throw new DecodeException("Plain text content only supports String as method param.");
            }
            int pos = contentType.indexOf("charset=");
            if (pos > 0) {
                try {
                    Charset charset = Charset.forName(contentType.substring(pos + 8));
                    return StreamUtils.toString(inputStream, charset);
                } catch (Exception e) {
                    throw new DecodeException("Unsupported charset:" + e.getMessage());
                }
            }
            return StreamUtils.toString(inputStream);
        } catch (Exception e) {
            throw new DecodeException(e);
        }
    }
}
