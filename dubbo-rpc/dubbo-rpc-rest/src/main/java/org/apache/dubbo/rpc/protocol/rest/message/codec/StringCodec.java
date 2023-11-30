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
package org.apache.dubbo.rpc.protocol.rest.message.codec;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodec;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 *  body is string
 */
@Activate("string")
public class StringCodec implements HttpMessageCodec<byte[], OutputStream> {

    @Override
    public Object decode(byte[] body, Class<?> targetType, Type type) throws Exception {
        if (body == null || body.length == 0) {
            return null;
        }
        return new String(body);
    }

    @Override
    public boolean contentTypeSupport(MediaType mediaType, Class<?> targetType) {
        return String.class.equals(targetType);
    }

    @Override
    public boolean typeSupport(Class<?> targetType) {
        return String.class.equals(targetType);
    }

    @Override
    public MediaType contentType() {
        return MediaType.TEXT_PLAIN;
    }

    @Override
    public void encode(OutputStream outputStream, Object unSerializedBody, URL url) throws Exception {
        outputStream.write(((String) unSerializedBody).getBytes(StandardCharsets.UTF_8));
    }
}
