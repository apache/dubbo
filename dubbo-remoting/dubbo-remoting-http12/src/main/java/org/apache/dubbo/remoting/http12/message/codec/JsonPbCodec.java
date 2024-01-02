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

import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.utils.MethodUtils;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

public class JsonPbCodec extends JsonCodec {

    @Override
    public void encode(OutputStream outputStream, Object unSerializedBody) throws EncodeException {
        try {
            if (unSerializedBody instanceof Message) {
                String jsonString = JsonFormat.printer().print((Message) unSerializedBody);
                outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
                return;
            }
        } catch (IOException e) {
            throw new EncodeException(e);
        }
        super.encode(outputStream, unSerializedBody);
    }

    @Override
    public void encode(OutputStream outputStream, Object[] data) throws EncodeException {
        super.encode(outputStream, data);
    }

    @Override
    public Object decode(InputStream body, Class<?> targetType) throws DecodeException {
        try {
            if (isProtobuf(targetType)) {
                Message.Builder newBuilder = (Message.Builder)
                        MethodUtils.findMethod(targetType, "newBuilder").invoke(null);
                JsonFormat.parser().ignoringUnknownFields().merge(StreamUtils.toString(body), newBuilder);
                return newBuilder.build();
            }
        } catch (Throwable e) {
            throw new DecodeException(e);
        }
        return super.decode(body, targetType);
    }

    @Override
    public Object[] decode(InputStream dataInputStream, Class<?>[] targetTypes) throws DecodeException {
        try {
            if (hasProtobuf(targetTypes)) {
                // protobuf only support one parameter
                return new Object[] {decode(dataInputStream, targetTypes[0])};
            }
        } catch (Throwable e) {
            throw new DecodeException(e);
        }
        return super.decode(dataInputStream, targetTypes);
    }

    private static boolean isProtobuf(Class<?> targetType) {
        if (targetType == null) {
            return false;
        }
        return Message.class.isAssignableFrom(targetType);
    }

    private static boolean hasProtobuf(Class<?>[] classes) {
        for (Class<?> clazz : classes) {
            if (isProtobuf(clazz)) {
                return true;
            }
        }
        return false;
    }
}
