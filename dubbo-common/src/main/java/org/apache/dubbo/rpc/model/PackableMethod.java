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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A packable method is used to customize serialization for methods. It can provide a common wrapper
 * for RESP / Protobuf.
 */
public interface PackableMethod {

    /**
     * Stream-based parse InputStream to request object
     */
    default Object parseRequest(InputStream input) throws IOException {
        return getRequestUnpack().unpack(input);
    }

    /**
     * Stream-based parse InputStream to response object
     */
    default Object parseResponse(InputStream input) throws IOException {
        return parseResponse(input, false);
    }

    /**
     * Stream-based parse InputStream to response object with exception handling
     */
    default Object parseResponse(InputStream input, boolean isReturnTriException) throws IOException {
        UnPack unPack = getResponseUnpack();
        if (unPack instanceof WrapperUnPack) {
            return ((WrapperUnPack) unPack).unpack(input, isReturnTriException);
        }
        return unPack.unpack(input);
    }

    /**
     * Stream-based pack request to OutputStream
     */
    default void packRequest(Object request, OutputStream output) throws IOException {
        getRequestPack().pack(request, output);
    }

    /**
     * Stream-based pack response to OutputStream
     */
    default void packResponse(Object response, OutputStream output) throws IOException {
        getResponsePack().pack(response, output);
    }

    /**
     * @deprecated Use {@link #parseRequest(InputStream)} for stream-based processing
     */
    @Deprecated
    default Object parseRequest(byte[] data) throws IOException {
        return parseRequest(new ByteArrayInputStream(data));
    }

    /**
     * @deprecated Use {@link #parseResponse(InputStream)} for stream-based processing
     */
    @Deprecated
    default Object parseResponse(byte[] data) throws IOException {
        return parseResponse(data, false);
    }

    /**
     * @deprecated Use {@link #parseResponse(InputStream, boolean)} for stream-based processing
     */
    @Deprecated
    default Object parseResponse(byte[] data, boolean isReturnTriException) throws IOException {
        return parseResponse(new ByteArrayInputStream(data), isReturnTriException);
    }

    /**
     * @deprecated Use {@link #packRequest(Object, OutputStream)} for stream-based processing
     */
    @Deprecated
    default byte[] packRequest(Object request) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        packRequest(request, baos);
        return baos.toByteArray();
    }

    /**
     * @deprecated Use {@link #packResponse(Object, OutputStream)} for stream-based processing
     */
    @Deprecated
    default byte[] packResponse(Object response) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        packResponse(response, baos);
        return baos.toByteArray();
    }

    /**
     * @deprecated Use {@link #packRequest(Object, OutputStream)} instead
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
