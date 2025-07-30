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
package org.apache.dubbo.rpc.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * A packable method is used to customize serialization for methods. It can provide a common wrapper
 * for RESP / Protobuf.
 */
public interface PackableMethod {

    /**
     * Zero-copy parse ByteBuf to request object
     */
    default Object parseRequest(ByteBuf data) throws Exception {
        return getRequestUnpack().unpack(data);
    }

    /**
     * Zero-copy parse ByteBuf to response object
     */
    default Object parseResponse(ByteBuf data) throws Exception {
        return parseResponse(data, false);
    }

    /**
     * Zero-copy parse ByteBuf to response object with exception handling
     */
    default Object parseResponse(ByteBuf data, boolean isReturnTriException) throws Exception {
        UnPack unPack = getResponseUnpack();
        if (unPack instanceof WrapperUnPack) {
            return ((WrapperUnPack) unPack).unpack(data, isReturnTriException);
        }
        return unPack.unpack(data);
    }

    /**
     * Zero-copy pack request to ByteBuf
     */
    default ByteBuf packRequest(Object request, ByteBufAllocator allocator) throws Exception {
        return getRequestPack().pack(request, allocator);
    }

    /**
     * Zero-copy pack response to ByteBuf
     */
    default ByteBuf packResponse(Object response, ByteBufAllocator allocator) throws Exception {
        return getResponsePack().pack(response, allocator);
    }

    /**
     * @deprecated Use {@link #parseRequest(ByteBuf)} for zero-copy processing
     */
    @Deprecated
    default Object parseRequest(byte[] data) throws Exception {
        ByteBuf buf = io.netty.buffer.Unpooled.wrappedBuffer(data);
        try {
            return parseRequest(buf);
        } finally {
            buf.release();
        }
    }

    /**
     * @deprecated Use {@link #parseResponse(ByteBuf)} for zero-copy processing
     */
    @Deprecated
    default Object parseResponse(byte[] data) throws Exception {
        return parseResponse(data, false);
    }

    /**
     * @deprecated Use {@link #parseResponse(ByteBuf, boolean)} for zero-copy processing
     */
    @Deprecated
    default Object parseResponse(byte[] data, boolean isReturnTriException) throws Exception {
        ByteBuf buf = io.netty.buffer.Unpooled.wrappedBuffer(data);
        try {
            return parseResponse(buf, isReturnTriException);
        } finally {
            buf.release();
        }
    }

    /**
     * @deprecated Use {@link #packRequest(Object, ByteBufAllocator)} for zero-copy processing
     */
    @Deprecated
    default byte[] packRequest(Object request) throws Exception {
        ByteBuf buf = packRequest(request, ByteBufAllocator.DEFAULT);
        try {
            byte[] result = new byte[buf.readableBytes()];
            buf.readBytes(result);
            return result;
        } finally {
            buf.release();
        }
    }

    /**
     * @deprecated Use {@link #packResponse(Object, ByteBufAllocator)} for zero-copy processing
     */
    @Deprecated
    default byte[] packResponse(Object response) throws Exception {
        ByteBuf buf = packResponse(response, ByteBufAllocator.DEFAULT);
        try {
            byte[] result = new byte[buf.readableBytes()];
            buf.readBytes(result);
            return result;
        } finally {
            buf.release();
        }
    }

    /**
     * @deprecated Use {@link #packRequest(Object, ByteBufAllocator)} instead
     */
    @Deprecated
    Pack pack(Object[] arguments);

    default boolean needWrapper() {
        return false;
    }

    Pack getRequestPack();

    Pack getResponsePack();

    UnPack getResponseUnpack();

    UnPack getRequestUnpack();
}
