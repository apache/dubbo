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

import com.google.protobuf.Message;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * compatible low version.
 * version < 3.3
 *
 * @since 3.3
 */
@Activate
public class GrpcCompositeCodec implements HttpMessageCodec {

    private static final MediaType MEDIA_TYPE = new MediaType("application", "grpc");

    private final ProtobufHttpMessageCodec protobufHttpMessageCodec;

    private final WrapperHttpMessageCodec wrapperHttpMessageCodec;

    public GrpcCompositeCodec(ProtobufHttpMessageCodec protobufHttpMessageCodec,
                              WrapperHttpMessageCodec wrapperHttpMessageCodec) {
        this.protobufHttpMessageCodec = protobufHttpMessageCodec;
        this.wrapperHttpMessageCodec = wrapperHttpMessageCodec;
    }

    public void setEncodeTypes(Class<?>[] encodeTypes) {
        this.wrapperHttpMessageCodec.setEncodeTypes(encodeTypes);
    }

    public void setDecodeTypes(Class<?>[] decodeTypes) {
        this.wrapperHttpMessageCodec.setDecodeTypes(decodeTypes);
    }


    @Override
    public void encode(OutputStream outputStream, Object data) throws EncodeException {
        //protobuf
        //TODO int compressed = Identity.MESSAGE_ENCODING.equals(requestMetadata.compressor.getMessageEncoding()) ? 0 : 1;
        try {
            int compressed = 0;
            outputStream.write(compressed);
            if (data instanceof Message) {
                int serializedSize = ((Message) data).getSerializedSize();
                //write length
                writeLength(outputStream, serializedSize);
                protobufHttpMessageCodec.encode(outputStream, data);
                return;
            }
            //wrapper
            wrapperHttpMessageCodec.encode(outputStream, data);
        } catch (IOException e) {
            throw new EncodeException(e);
        }
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType) throws DecodeException {
        if (isProtobuf(targetType)) {
            return protobufHttpMessageCodec.decode(inputStream, targetType);
        }
        return wrapperHttpMessageCodec.decode(inputStream, targetType);
    }

    @Override
    public Object[] decode(InputStream inputStream, Class<?>[] targetTypes) throws DecodeException {
        if (targetTypes.length > 1) {
            return wrapperHttpMessageCodec.decode(inputStream, targetTypes);
        }
        return HttpMessageCodec.super.decode(inputStream, targetTypes);
    }

    private boolean isProtobuf(Class<?> targetType) {
        if (targetType == null) {
            return false;
        }
        return Message.class.isAssignableFrom(targetType);
    }

    private static void writeLength(OutputStream outputStream, int length) {
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
    public MediaType contentType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean support(String contentType) {
        return contentType.startsWith(MEDIA_TYPE.getName());
    }
}
