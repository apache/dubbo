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
import org.apache.dubbo.rpc.protocol.rest.pair.MessageCodecResultPair;

import java.io.OutputStream;
import java.util.Set;

public class HttpMessageCodecManager {
    private static final Set<HttpMessageCodec> httpMessageCodecs =
        FrameworkModel.defaultModel().getExtensionLoader(HttpMessageCodec.class).getSupportedExtensionInstances();


    public static Object httpMessageDecode(byte[] body, Class<?> type, MediaType mediaType) throws Exception {
        if (body == null || body.length == 0) {
            return null;
        }

        for (HttpMessageCodec httpMessageCodec : httpMessageCodecs) {
            if (httpMessageCodec.contentTypeSupport(mediaType, type) || typeJudge(mediaType, type, httpMessageCodec)) {
                return httpMessageCodec.decode(body, type);
            }
        }
        throw new UnSupportContentTypeException("UnSupport content-type :" + mediaType.value);
    }

    public static MessageCodecResultPair httpMessageEncode(OutputStream outputStream, Object unSerializedBody, URL url, MediaType mediaType, Class<?> bodyType) throws Exception {


        if (unSerializedBody == null) {
            for (HttpMessageCodec httpMessageCodec : httpMessageCodecs) {
                if (httpMessageCodec.contentTypeSupport(mediaType, bodyType) || typeJudge(mediaType, bodyType, httpMessageCodec)) {
                    return MessageCodecResultPair.pair(false, httpMessageCodec.contentType());
                }
            }
        }

        for (HttpMessageCodec httpMessageCodec : httpMessageCodecs) {
            if (httpMessageCodec.contentTypeSupport(mediaType, bodyType) || typeJudge(mediaType, bodyType, httpMessageCodec)) {
                httpMessageCodec.encode(outputStream, unSerializedBody, url);
                return MessageCodecResultPair.pair(true, httpMessageCodec.contentType());
            }
        }


        throw new UnSupportContentTypeException("UnSupport content-type :" + mediaType.value);
    }

    /**
     * if content-type is null or  all ,will judge media type by class type
     *
     * @param mediaType
     * @param bodyType
     * @param httpMessageCodec
     * @return
     */
    private static boolean typeJudge(MediaType mediaType, Class<?> bodyType, HttpMessageCodec httpMessageCodec) {
        return (MediaType.ALL_VALUE.equals(mediaType) || mediaType == null)
            && bodyType != null && httpMessageCodec.typeSupport(bodyType);
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
