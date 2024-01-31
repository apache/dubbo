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
import org.apache.dubbo.rpc.protocol.rest.message.MediaTypeMatcher;
import org.apache.dubbo.rpc.protocol.rest.util.DataParseUtils;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 *  content-type is text/html
 */
@Activate("text")
public class TextCodec implements HttpMessageCodec<byte[], OutputStream> {

    @Override
    public Object decode(byte[] body, Class<?> targetType, Type type) throws Exception {
        return DataParseUtils.stringTypeConvert(targetType, new String(body, StandardCharsets.UTF_8));
    }

    @Override
    public boolean contentTypeSupport(MediaType mediaType, Class<?> targetType) {
        return MediaTypeMatcher.TEXT_PLAIN.mediaSupport(mediaType);
    }

    @Override
    public boolean typeSupport(Class<?> targetType) {
        return DataParseUtils.isTextType(targetType);
    }

    @Override
    public MediaType contentType() {
        return MediaType.TEXT_PLAIN;
    }

    @Override
    public void encode(OutputStream outputStream, Object unSerializedBody, URL url) throws Exception {
        DataParseUtils.writeTextContent(unSerializedBody, outputStream);
    }
}
