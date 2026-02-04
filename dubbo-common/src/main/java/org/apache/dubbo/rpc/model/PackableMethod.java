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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * A packable method is used to customize serialization for methods. It can provide a common wrapper
 * for RESP / Protobuf.
 */
public interface PackableMethod {

    default Object parseRequest(byte[] data) throws Exception {
        return getRequestUnpack().unpack(data);
    }

    default Object parseResponse(byte[] data) throws Exception {
        return parseResponse(data, false);
    }

    default Object parseResponse(byte[] data, boolean isReturnTriException) throws Exception {
        UnPack unPack = getResponseUnpack();
        if (unPack instanceof WrapperUnPack) {
            return ((WrapperUnPack) unPack).unpack(data, isReturnTriException);
        }
        return unPack.unpack(data);
    }

    /**
     * Parse response from InputStream.
     * Default implementation reads all bytes and delegates to byte[] version.
     *
     * @param inputStream the input stream containing the response data
     * @param isReturnTriException whether the response is a Triple exception
     * @return the parsed response object
     * @throws Exception if parsing fails
     */
    default Object parseResponse(InputStream inputStream, boolean isReturnTriException) throws Exception {
        // Read all bytes from InputStream
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int len;
        while ((len = inputStream.read(tmp)) != -1) {
            buffer.write(tmp, 0, len);
        }
        byte[] data = buffer.toByteArray();
        return parseResponse(data, isReturnTriException);
    }

    default byte[] packRequest(Object request) throws Exception {
        return getRequestPack().pack(request);
    }

    default byte[] packResponse(Object response) throws Exception {
        return getResponsePack().pack(response);
    }

    default boolean needWrapper() {
        return false;
    }

    Pack getRequestPack();

    Pack getResponsePack();

    UnPack getResponseUnpack();

    UnPack getRequestUnpack();
}
