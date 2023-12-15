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
import org.apache.dubbo.remoting.http12.exception.UnsupportedMediaTypeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoderFactory;
import org.apache.dubbo.remoting.http12.message.HttpMessageEncoder;
import org.apache.dubbo.remoting.http12.message.HttpMessageEncoderFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CodecUtils {

    private FrameworkModel frameworkModel;

    private final List<HttpMessageDecoderFactory> decoders;

    private final List<HttpMessageEncoderFactory> encoders;

    public CodecUtils(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        this.decoders = frameworkModel
                .getExtensionLoader(HttpMessageDecoderFactory.class)
                .getActivateExtensions();
        this.encoders = frameworkModel
                .getExtensionLoader(HttpMessageEncoderFactory.class)
                .getActivateExtensions();
    }

    public HttpMessageDecoder determineHttpMessageDecoder(FrameworkModel frameworkModel, String contentType, URL url) {
        return determineHttpMessageDecoderFactory(contentType).createCodec(url, frameworkModel, contentType);
    }

    public HttpMessageEncoder determineHttpMessageEncoder(FrameworkModel frameworkModel, HttpHeaders headers, URL url) {
        String mediaType = getEncodeMediaType(headers);
        return determineHttpMessageEncoderFactory(mediaType).createCodec(url, frameworkModel, mediaType);
    }

    public HttpMessageDecoderFactory determineHttpMessageDecoderFactory(String mediaType) {
        ;
        for (HttpMessageDecoderFactory decoderFactory : decoders) {
            if (mediaType.startsWith(decoderFactory.mediaType().getName())) {
                return decoderFactory;
            }
        }
        throw new UnsupportedMediaTypeException(mediaType);
    }

    public HttpMessageEncoderFactory determineHttpMessageEncoderFactory(String mediaType) {
        for (HttpMessageEncoderFactory encoderFactory : encoders) {
            if (mediaType.startsWith(encoderFactory.mediaType().getName())) {
                return encoderFactory;
            }
        }
        throw new UnsupportedMediaTypeException(mediaType);
    }

    public List<HttpMessageDecoderFactory> getDecoders() {
        return decoders;
    }

    public List<HttpMessageEncoderFactory> getEncoders() {
        return encoders;
    }

    public static String getEncodeMediaType(HttpHeaders headers) {
        String mediaType = headers.getAccept();
        if (mediaType == null) {
            mediaType = headers.getContentType();
        }
        return mediaType;
    }

    public static ByteArrayOutputStream toByteArrayStream(InputStream in) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result;
    }
}
