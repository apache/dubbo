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
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodec;
import org.apache.dubbo.rpc.protocol.rest.message.MediaTypeMatcher;
import org.apache.dubbo.rpc.protocol.rest.util.DataParseUtils;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 *  body is json
 */
@Activate("json")
public class JsonCodec implements HttpMessageCodec<byte[], OutputStream> {
    private static final Set<Class> unSupportClasses = new HashSet<>();

    static {

        unSupportClasses.add(byte[].class);
        unSupportClasses.add(String.class);

    }

    @Override
    public Object decode(byte[] body, Class<?> targetType) throws Exception {
        return DataParseUtils.jsonConvert(targetType, body);
    }

    @Override
    public boolean contentTypeSupport(MediaType mediaType, Class<?> targetType) {
        return MediaTypeMatcher.APPLICATION_JSON.mediaSupport(mediaType) && !unSupportClasses.contains(targetType);
    }

    @Override
    public boolean typeSupport(Class<?> targetType) {
        return !unSupportClasses.contains(targetType) && !DataParseUtils.isTextType(targetType);
    }

    @Override
    public MediaType contentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }


    @Override
    public void encode(OutputStream outputStream, Object unSerializedBody, URL url) throws Exception {
        outputStream.write(JsonUtils.toJson(unSerializedBody).getBytes(StandardCharsets.UTF_8));
    }
}
