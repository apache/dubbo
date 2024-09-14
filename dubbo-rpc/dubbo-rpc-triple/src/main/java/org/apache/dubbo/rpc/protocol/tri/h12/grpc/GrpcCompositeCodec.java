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
package org.apache.dubbo.rpc.protocol.tri.h12.grpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.PackableMethod;
import org.apache.dubbo.rpc.model.PackableMethodFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PACKABLE_METHOD_FACTORY;

public class GrpcCompositeCodec implements HttpMessageCodec {

    private static final String PACKABLE_METHOD_CACHE = "PACKABLE_METHOD_CACHE";

    private final URL url;

    private final FrameworkModel frameworkModel;

    private final String mediaType;

    private PackableMethod packableMethod;

    public GrpcCompositeCodec(URL url, FrameworkModel frameworkModel, String mediaType) {
        this.url = url;
        this.frameworkModel = frameworkModel;
        this.mediaType = mediaType;
    }

    public void loadPackableMethod(MethodDescriptor methodDescriptor) {
        if (methodDescriptor instanceof PackableMethod) {
            packableMethod = (PackableMethod) methodDescriptor;
            return;
        }

        packableMethod = UrlUtils.computeServiceAttribute(
                        url, PACKABLE_METHOD_CACHE, k -> new ConcurrentHashMap<MethodDescriptor, PackableMethod>())
                .computeIfAbsent(methodDescriptor, md -> frameworkModel
                        .getExtensionLoader(PackableMethodFactory.class)
                        .getExtension(ConfigurationUtils.getGlobalConfiguration(url.getApplicationModel())
                                .getString(DUBBO_PACKABLE_METHOD_FACTORY, DEFAULT_KEY))
                        .create(methodDescriptor, url, mediaType));
    }

    @Override
    public void encode(OutputStream outputStream, Object data, Charset charset) throws EncodeException {
        // protobuf
        // TODO int compressed = Identity.MESSAGE_ENCODING.equals(requestMetadata.compressor.getMessageEncoding()) ? 0 :
        // 1;
        try {
            int compressed = 0;
            outputStream.write(compressed);
            byte[] bytes = packableMethod.packResponse(data);
            writeLength(outputStream, bytes.length);
            outputStream.write(bytes);
        } catch (HttpStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new EncodeException(e);
        }
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType, Charset charset) throws DecodeException {
        try {
            byte[] data = StreamUtils.readBytes(inputStream);
            return packableMethod.parseRequest(data);
        } catch (HttpStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new DecodeException(e);
        }
    }

    @Override
    public Object[] decode(InputStream inputStream, Class<?>[] targetTypes, Charset charset) throws DecodeException {
        Object message = decode(inputStream, ArrayUtils.isEmpty(targetTypes) ? null : targetTypes[0], charset);
        if (message instanceof Object[]) {
            return (Object[]) message;
        }
        return new Object[] {message};
    }

    private void writeLength(OutputStream outputStream, int length) {
        try {
            outputStream.write(((length >> 24) & 0xFF));
            outputStream.write(((length >> 16) & 0xFF));
            outputStream.write(((length >> 8) & 0xFF));
            outputStream.write((length & 0xFF));
        } catch (IOException e) {
            throw new EncodeException(e);
        }
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_GRPC;
    }
}
