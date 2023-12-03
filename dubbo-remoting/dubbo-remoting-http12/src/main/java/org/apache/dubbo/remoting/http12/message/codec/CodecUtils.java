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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodecFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;

public class CodecUtils {

    public static HttpMessageCodec determineHttpMessageCodec(
            FrameworkModel frameworkModel, HttpHeaders headers, URL url, boolean decode) {
        String mediaType = headers.getContentType();
        if (decode && headers.getAccept() != null) {
            mediaType = headers.getAccept();
        }
        HttpMessageCodecFactory factory = determineHttpMessageCodecFactory(frameworkModel, mediaType, decode);
        if (factory != null) {
            return factory.createCodec(url, frameworkModel, mediaType);
        }
        return null;
    }

    public static HttpMessageCodecFactory determineHttpMessageCodecFactory(
            FrameworkModel frameworkModel, String mediaType, boolean decode) {
        frameworkModel.getExtensionLoader(HttpMessageCodecFactory.class).getSupportedExtensions();
        for (HttpMessageCodecFactory httpMessageCodecFactory :
                frameworkModel.getExtensionLoader(HttpMessageCodecFactory.class).getActivateExtensions()) {
            if (decode) {
                if (httpMessageCodecFactory.codecSupport().supportDecode(mediaType)) {
                    return httpMessageCodecFactory;
                }
            } else {
                // encode
                if (httpMessageCodecFactory.codecSupport().supportEncode(mediaType)) {
                    return httpMessageCodecFactory;
                }
            }
        }
        return null;
    }
}
