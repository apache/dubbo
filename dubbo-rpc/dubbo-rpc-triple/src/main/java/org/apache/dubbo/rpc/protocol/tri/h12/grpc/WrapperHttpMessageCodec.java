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
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleCustomerProtocolWapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class WrapperHttpMessageCodec implements HttpMessageCodec {

    private static final MediaType MEDIA_TYPE = new MediaType("application", "triple+wrapper");

    private static final String DEFAULT_SERIALIZE_TYPE = "fastjson2";

    private final MultipleSerialization serialization;

    private final URL url;

    private Class<?>[] encodeTypes;

    private Class<?>[] decodeTypes;

    private String serializeType = DEFAULT_SERIALIZE_TYPE;

    public WrapperHttpMessageCodec(URL url, FrameworkModel frameworkModel) {
        this.url = url;
        this.serialization = frameworkModel
                .getExtensionLoader(MultipleSerialization.class)
                .getExtension(url.getParameter(Constants.MULTI_SERIALIZATION_KEY, CommonConstants.DEFAULT_KEY));
    }

    public void setSerializeType(String serializeType) {
        this.serializeType = serializeType;
    }

    public void setEncodeTypes(Class<?>[] encodeTypes) {
        this.encodeTypes = encodeTypes;
    }

    public void setDecodeTypes(Class<?>[] decodeTypes) {
        this.decodeTypes = decodeTypes;
    }

    @Override
    public void encode(OutputStream outputStream, Object data, Charset charset) throws EncodeException {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            serialization.serialize(url, serializeType, encodeTypes[0], data, bos);
            byte[] encoded = TripleCustomerProtocolWapper.TripleResponseWrapper.Builder.newBuilder()
                    .setSerializeType(serializeType)
                    .setType(encodeTypes[0].getName())
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
    public void encode(OutputStream outputStream, Object[] data, Charset charset) throws EncodeException {
        // TODO
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType, Charset charset) throws DecodeException {
        Object[] decode = this.decode(inputStream, new Class[] {targetType}, charset);
        if (decode == null || decode.length == 0) {
            return null;
        }
        return decode[0];
    }

    @Override
    public Object[] decode(InputStream inputStream, Class<?>[] targetTypes, Charset charset) throws DecodeException {
        try {
            int len;
            byte[] data = new byte[4096];
            ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
            while ((len = inputStream.read(data)) != -1) {
                bos.write(data, 0, len);
            }
            TripleCustomerProtocolWapper.TripleRequestWrapper wrapper =
                    TripleCustomerProtocolWapper.TripleRequestWrapper.parseFrom(bos.toByteArray());
            final String serializeType = convertHessianFromWrapper(wrapper.getSerializeType());
            CodecSupport.checkSerialization(serializeType, url);
            setSerializeType(wrapper.getSerializeType());
            Object[] ret = new Object[wrapper.getArgs().size()];
            for (int i = 0; i < wrapper.getArgs().size(); i++) {
                ByteArrayInputStream in =
                        new ByteArrayInputStream(wrapper.getArgs().get(i));
                try {
                    ret[i] = this.serialization.deserialize(url, wrapper.getSerializeType(), targetTypes[i], in);
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
    public MediaType mediaType() {
        return MEDIA_TYPE;
    }

    private static void writeLength(OutputStream outputStream, int length) throws IOException {
        outputStream.write(((length >> 24) & 0xFF));
        outputStream.write(((length >> 16) & 0xFF));
        outputStream.write(((length >> 8) & 0xFF));
        outputStream.write((length & 0xFF));
    }

    private static String convertHessianFromWrapper(String serializeType) {
        if (TripleConstant.HESSIAN4.equals(serializeType)) {
            return TripleConstant.HESSIAN2;
        }
        return serializeType;
    }
}
