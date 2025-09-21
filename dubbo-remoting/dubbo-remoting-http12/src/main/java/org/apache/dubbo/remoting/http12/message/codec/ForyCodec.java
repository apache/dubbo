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

import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import org.apache.fory.Fory;
import org.apache.fory.config.Language;
import org.apache.fory.io.ForyStreamReader;
import org.apache.fory.memory.MemoryBuffer;

public class ForyCodec implements HttpMessageCodec {

    private static final Fory fory = Fory.builder()
            .withLanguage(Language.JAVA)
            .withRefTracking(true)
            .requireClassRegistration(false)
            .build();

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_GRPC;
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType, Charset charset) throws DecodeException {
        return fory.deserialize(ForyStreamReader.of(inputStream));
    }

    @Override
    public Object decode(ByteBuf buffer, Class<?> targetType, Charset charset) throws DecodeException {
        MemoryBuffer memoryBuffer = MemoryBuffer.fromByteBuffer(buffer.nioBuffer());
        return fory.deserialize(memoryBuffer);
    }

    @Override
    public void encode(OutputStream outputStream, Object data, Charset charset) throws EncodeException {
        fory.serialize(outputStream, data);
    }

    @Override
    public void encode(ByteBuf buffer, Object data, Charset charset) throws EncodeException {
        //        MemoryBuffer memoryBuffer = MemoryBuffer.fromByteBuffer(buffer);
        //        fory.serialize(memoryBuffer, data);
    }
}
