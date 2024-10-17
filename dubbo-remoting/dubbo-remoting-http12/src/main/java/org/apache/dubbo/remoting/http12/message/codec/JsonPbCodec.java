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
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.util.JsonFormat;

public final class JsonPbCodec extends JsonCodec {

    public static final JsonPbCodec INSTANCE = new JsonPbCodec();

    private JsonPbCodec() {}

    public JsonPbCodec(FrameworkModel frameworkModel) {
        super(frameworkModel);
    }

    @Override
    public void encode(OutputStream os, Object data, Charset charset) throws EncodeException {
        try {
            if (data instanceof Message) {
                String jsonString =
                        JsonFormat.printer().omittingInsignificantWhitespace().print((Message) data);
                os.write(jsonString.getBytes(charset));
                return;
            }
        } catch (IOException e) {
            throw new EncodeException("Error encoding jsonPb", e);
        }
        super.encode(os, data, charset);
    }

    @Override
    public Object decode(InputStream is, Class<?> targetType, Charset charset) throws DecodeException {
        try {
            if (isProtobuf(targetType)) {
                Builder newBuilder = (Builder)
                        MethodUtils.findMethod(targetType, "newBuilder").invoke(null);
                JsonFormat.parser().ignoringUnknownFields().merge(StreamUtils.toString(is, charset), newBuilder);
                return newBuilder.build();
            }
        } catch (HttpStatusException e) {
            throw e;
        } catch (Throwable e) {
            throw new DecodeException("Error decoding jsonPb", e);
        }
        return super.decode(is, targetType, charset);
    }

    @Override
    public Object decode(InputStream is, Type targetType, Charset charset) throws DecodeException {
        return targetType instanceof Class
                ? decode(is, (Class<?>) targetType, charset)
                : super.decode(is, targetType, charset);
    }

    @Override
    public Object[] decode(InputStream is, Class<?>[] targetTypes, Charset charset) throws DecodeException {
        try {
            if (hasProtobuf(targetTypes)) {
                // protobuf only support one parameter
                return new Object[] {decode(is, targetTypes[0], charset)};
            }
        } catch (HttpStatusException e) {
            throw e;
        } catch (Throwable e) {
            throw new DecodeException("Error decoding jsonPb", e);
        }
        return super.decode(is, targetTypes, charset);
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
