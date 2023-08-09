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

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleCustomerProtocolWapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WrapperHttpMessageCodec implements HttpMessageCodec {

    private static final MediaType MEDIA_TYPE = new MediaType("application", "triple+wrapper");

    private static final String DEFAULT_SERIALIZE_TYPE = "fastjson2";

    private final Map<String, Serialization> serializations;

    public WrapperHttpMessageCodec() {
        this.serializations = initSerializations();
    }

    private Map<String, Serialization> initSerializations() {
        Map<String, Serialization> map = new HashMap<>();
        ExtensionLoader<Serialization> extensionLoader = FrameworkModel.defaultModel().getExtensionLoader(Serialization.class);
        Set<String> loadedExtensions = extensionLoader.getLoadedExtensions();
        for (String name : loadedExtensions) {
            Serialization serialization = extensionLoader.getExtension(name);
            map.put(name, serialization);
        }
        return map;
    }

    @Override
    public void encode(OutputStream outputStream, Object data) throws EncodeException {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Serialization serialization = serializations.get(DEFAULT_SERIALIZE_TYPE);
            ObjectOutput serialize = serialization.serialize(null, bos);
            serialize.writeObject(data);
            serialize.flushBuffer();
            String type = data == null ? null : data.getClass().getName();
            byte[] encoded = TripleCustomerProtocolWapper.TripleResponseWrapper.Builder.newBuilder()
                .setSerializeType(DEFAULT_SERIALIZE_TYPE)
                .setType(type)
                .setData(bos.toByteArray())
                .build()
                .toByteArray();
            writeLength(outputStream, encoded.length);
            outputStream.write(encoded);
        } catch (IOException e) {
            throw new EncodeException(e);
        }
    }

    @Override
    public void encode(OutputStream outputStream, Object[] data) throws EncodeException {
        //TODO
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType) throws DecodeException {
        return this.decode(inputStream, new Class[]{targetType})[0];
    }

    @Override
    public Object[] decode(InputStream inputStream, Class<?>[] targetTypes) throws DecodeException {
        try {
            int len;
            byte[] data = new byte[4096];
            ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
            while ((len = inputStream.read(data)) != -1) {
                bos.write(data, 0, len);
            }
            TripleCustomerProtocolWapper.TripleRequestWrapper wrapper = TripleCustomerProtocolWapper.TripleRequestWrapper.parseFrom(
                bos.toByteArray());
            String wrapperSerializeType = convertHessianFromWrapper(wrapper.getSerializeType());
            Serialization serialization = serializations.get(wrapperSerializeType);
            Object[] ret = new Object[wrapper.getArgs().size()];
            for (int i = 0; i < wrapper.getArgs().size(); i++) {
                ByteArrayInputStream bais = new ByteArrayInputStream(
                    wrapper.getArgs().get(i));
                try {
                    ret[i] = serialization.deserialize(null, bais).readObject(targetTypes[i]);
                } catch (ClassNotFoundException e) {
                    throw new DecodeException(e);
                }
            }
            return ret;
        } catch (IOException e) {
            throw new DecodeException(e);
        }
    }

    @Override
    public MediaType contentType() {
        return MEDIA_TYPE;
    }

    private static String convertHessianFromWrapper(String serializeType) {
        if (TripleConstant.HESSIAN4.equals(serializeType)) {
            return TripleConstant.HESSIAN2;
        }
        return serializeType;
    }

    private static void writeLength(OutputStream outputStream, int length) throws IOException {
        outputStream.write(((length >> 24) & 0xFF));
        outputStream.write(((length >> 16) & 0xFF));
        outputStream.write(((length >> 8) & 0xFF));
        outputStream.write((length & 0xFF));
    }
}
