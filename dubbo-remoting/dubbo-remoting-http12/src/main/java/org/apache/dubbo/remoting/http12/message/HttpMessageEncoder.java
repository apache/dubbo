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

import java.io.OutputStream;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface HttpMessageEncoder extends CodecMediaType {

    void encode(OutputStream outputStream, Object data, Charset charset) throws EncodeException;

    @Deprecated
    default ByteBuf encode(Object data, ByteBufAllocator allocator) throws EncodeException {
        ByteBuf buffer = allocator.buffer();
        try (ByteBufOutputStream os = new ByteBufOutputStream(buffer)) {
            encode(os, data, UTF_8);
        } catch (Exception e) {
            buffer.release();
            if (e instanceof EncodeException) {
                throw (EncodeException) e;
            }
            throw new EncodeException(e);
        }
        return buffer;
    }

    /**
     * Zero-copy streaming encode for multiple objects
     */
    default void encode(OutputStream outputStream, Object[] data, Charset charset) throws EncodeException {
        encode(outputStream, ArrayUtils.first(data), charset);
    }

    /**
     * Zero-copy streaming encode with UTF-8 charset
     */
    default void encode(OutputStream outputStream, Object data) throws EncodeException {
        encode(outputStream, data, UTF_8);
    }

    /**
     * Zero-copy streaming encode for multiple objects with UTF-8 charset
     */
    default void encode(OutputStream outputStream, Object[] data) throws EncodeException {
        encode(outputStream, ArrayUtils.first(data), UTF_8);
    }

    default String contentType() {
        return mediaType().getName();
    }
}
