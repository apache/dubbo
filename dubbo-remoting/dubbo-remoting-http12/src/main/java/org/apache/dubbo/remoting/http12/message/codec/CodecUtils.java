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
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoderFactory;
import org.apache.dubbo.remoting.http12.message.HttpMessageEncoder;
import org.apache.dubbo.remoting.http12.message.HttpMessageEncoderFactory;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.handler.codec.CodecException;

public class CodecUtils {

    private final FrameworkModel frameworkModel;

    private final List<HttpMessageDecoderFactory> decoders;

    private final List<HttpMessageEncoderFactory> encoders;

    private final Map<String, HttpMessageEncoderFactory> encoderCache;

    private final Map<String, HttpMessageDecoderFactory> decoderCache;

    public CodecUtils(FrameworkModel frameworkModel) {
        this.encoderCache = new ConcurrentHashMap<>(1);
        this.decoderCache = new ConcurrentHashMap<>(1);
        this.frameworkModel = frameworkModel;
        this.decoders = frameworkModel
                .getExtensionLoader(HttpMessageDecoderFactory.class)
                .getActivateExtensions();
        this.encoders = frameworkModel
                .getExtensionLoader(HttpMessageEncoderFactory.class)
                .getActivateExtensions();
        decoders.forEach(
                decoderFactory -> decoderCache.put(decoderFactory.mediaType().getName(), decoderFactory));
        encoders.forEach(
                encoderFactory -> encoderCache.put(encoderFactory.mediaType().getName(), encoderFactory));
    }

    public HttpMessageDecoder determineHttpMessageDecoder(FrameworkModel frameworkModel, String contentType, URL url) {
        return determineHttpMessageDecoderFactory(contentType).createCodec(url, frameworkModel, contentType);
    }

    public HttpMessageEncoder determineHttpMessageEncoder(FrameworkModel frameworkModel, HttpHeaders headers, URL url) {
        String mediaType = getEncodeMediaType(headers);
        return determineHttpMessageEncoderFactory(mediaType).createCodec(url, frameworkModel, mediaType);
    }

    public HttpMessageDecoderFactory determineHttpMessageDecoderFactory(String mediaType) {
        HttpMessageDecoderFactory factory = decoderCache.computeIfAbsent(mediaType, k -> {
            for (HttpMessageDecoderFactory decoderFactory : decoders) {
                if (decoderFactory.supports(mediaType)) {
                    return decoderFactory;
                }
            }
            return new UnsupportedCodecFactory();
        });
        if (factory instanceof UnsupportedCodecFactory) {
            throw new UnsupportedMediaTypeException(mediaType);
        }
        return factory;
    }

    public HttpMessageEncoderFactory determineHttpMessageEncoderFactory(String mediaType) {
        HttpMessageEncoderFactory factory = encoderCache.computeIfAbsent(mediaType, k -> {
            for (HttpMessageEncoderFactory encoderFactory : encoders) {
                if (encoderFactory.supports(mediaType)) {
                    return encoderFactory;
                }
            }
            return new UnsupportedCodecFactory();
        });
        if (factory instanceof UnsupportedCodecFactory) {
            throw new UnsupportedMediaTypeException(mediaType);
        }
        return factory;
    }

    public List<HttpMessageDecoderFactory> getDecoders() {
        return decoders;
    }

    public List<HttpMessageEncoderFactory> getEncoders() {
        return encoders;
    }

    static class UnsupportedCodecFactory implements HttpMessageEncoderFactory, HttpMessageDecoderFactory {
        @Override
        public MediaType mediaType() {
            throw new CodecException();
        }

        @Override
        public boolean supports(String mediaType) {
            throw new CodecException();
        }

        @Override
        public HttpMessageCodec createCodec(URL url, FrameworkModel frameworkModel, String mediaType) {
            throw new CodecException();
        }
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
