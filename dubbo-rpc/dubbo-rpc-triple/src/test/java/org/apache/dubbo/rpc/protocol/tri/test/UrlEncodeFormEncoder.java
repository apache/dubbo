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
package org.apache.dubbo.rpc.protocol.tri.test;

import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.message.HttpMessageEncoder;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class UrlEncodeFormEncoder implements HttpMessageEncoder {

    public static final UrlEncodeFormEncoder INSTANCE = new UrlEncodeFormEncoder();

    @Override
    public void encode(OutputStream os, Object data, Charset charset) throws EncodeException {
        try {
            if (data instanceof String) {
                os.write(((String) data).getBytes());
                return;
            }
            if (data instanceof Map) {
                StringBuilder sb = new StringBuilder(64);
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) data).entrySet()) {
                    sb.append(encode(entry.getKey()))
                            .append('=')
                            .append(encode(entry.getValue()))
                            .append('&');
                }
                int len = sb.length();
                if (len > 1) {
                    os.write(sb.substring(0, len - 1).getBytes(charset));
                }
                return;
            }
        } catch (HttpStatusException e) {
            throw e;
        } catch (Throwable t) {
            throw new EncodeException("Error encoding form-urlencoded", t);
        }
        throw new DecodeException("Only supports String or Map as return type.");
    }

    private static String encode(Object value) throws UnsupportedEncodingException {
        return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8.name());
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_FROM_URLENCODED;
    }
}
