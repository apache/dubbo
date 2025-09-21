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
package org.apache.dubbo.remoting.http12.message;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.remoting.http12.exception.EncodeException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface HttpMessageEncoder extends CodecMediaType {

    /**
     * Zero-copy encode using ByteBuf allocator - primary API for zero-copy
     */
    default ByteBuf encode(Object data, ByteBufAllocator allocator) throws EncodeException {
        try (java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream()) {
            encode(os, data, UTF_8);
            byte[] bytes = os.toByteArray();
            ByteBuf buffer = allocator.buffer(bytes.length);
            buffer.writeBytes(bytes);
            return buffer;
        } catch (java.io.IOException e) {
            throw new EncodeException("Error encoding to ByteBuf", e);
        }
    }

    /**
     * @deprecated Use {@link #encode(Object, ByteBufAllocator)} for zero-copy processing
     */
    @Deprecated
    default void encode(OutputStream outputStream, Object data, Charset charset) throws EncodeException {
        ByteBuf buffer = encode(data, null);
        try {
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            outputStream.write(bytes);
        } catch (IOException e) {
            throw new EncodeException(e);
        } finally {
            buffer.release();
        }
    }

    /**
     * @deprecated Use {@link #encode(Object, ByteBufAllocator)} for zero-copy processing
     */
    @Deprecated
    default void encode(OutputStream outputStream, Object[] data, Charset charset) throws EncodeException {
        encode(outputStream, ArrayUtils.first(data), charset);
    }

    /**
     * @deprecated Use {@link #encode(Object, ByteBufAllocator)} for zero-copy processing
     */
    @Deprecated
    default void encode(OutputStream outputStream, Object data) throws EncodeException {
        encode(outputStream, data, UTF_8);
    }

    /**
     * @deprecated Use {@link #encode(Object, ByteBufAllocator)} for zero-copy processing
     */
    @Deprecated
    default void encode(OutputStream outputStream, Object[] data) throws EncodeException {
        encode(outputStream, ArrayUtils.first(data), UTF_8);
    }

    default String contentType() {
        return mediaType().getName();
    }
}
