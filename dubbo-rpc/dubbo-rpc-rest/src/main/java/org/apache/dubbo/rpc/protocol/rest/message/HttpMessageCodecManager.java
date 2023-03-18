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
package org.apache.dubbo.rpc.protocol.rest.message;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.rest.exception.UnSupportContentTypeException;
import org.apache.dubbo.rpc.protocol.rest.util.Pair;

import java.io.OutputStream;
import java.util.Set;

public class HttpMessageCodecManager {
    private static final Set<HttpMessageCodec> httpMessageCodecs =
        FrameworkModel.defaultModel().getExtensionLoader(HttpMessageCodec.class).getSupportedExtensionInstances();


    public static Object httpMessageDecode(byte[] body, Class type, MediaType mediaType) throws Exception {
        for (HttpMessageCodec httpMessageCodec : httpMessageCodecs) {
            if (httpMessageCodec.contentTypeSupport(mediaType, type)) {
                return httpMessageCodec.decode(body, type);
            }
        }
        throw new UnSupportContentTypeException("UnSupport content-type :" + mediaType.value);
    }

    public static Pair<Boolean, MediaType> httpMessageEncode(OutputStream outputStream, Object unSerializedBody, URL url, MediaType mediaType, Class bodyType) throws Exception {


        if (unSerializedBody == null) {
            for (HttpMessageCodec httpMessageCodec : httpMessageCodecs) {
                if (httpMessageCodec.contentTypeSupport(mediaType, bodyType) || httpMessageCodec.typeSupport(bodyType)) {
                    return Pair.make(false, httpMessageCodec.contentType());
                }
            }
        }

        for (HttpMessageCodec httpMessageCodec : httpMessageCodecs) {
            if (httpMessageCodec.contentTypeSupport(mediaType, bodyType) || httpMessageCodec.typeSupport(bodyType)) {
                httpMessageCodec.encode(outputStream, unSerializedBody, url);
                return Pair.make(true, httpMessageCodec.contentType());
            }
        }


        throw new UnSupportContentTypeException("UnSupport content-type :" + mediaType.value);
    }

    public static MediaType typeSupport(Class<?> type) {
        for (HttpMessageCodec httpMessageCodec : httpMessageCodecs) {

            if (httpMessageCodec.typeSupport(type)) {
                return httpMessageCodec.contentType();
            }

        }

        return null;
    }


}
