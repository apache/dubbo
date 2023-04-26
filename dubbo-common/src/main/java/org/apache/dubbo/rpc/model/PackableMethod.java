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

import java.io.IOException;

/**
 * A packable method is used to customize serialization for methods. It can provide a common wrapper
 * for RESP / Protobuf.
 */
public interface PackableMethod {

    interface Pack {

        /**
         * @param obj instance
         * @return byte array
         * @throws IOException when error occurs
         */
        byte[] pack(Object obj) throws IOException;
    }

    interface WrapperUnPack extends UnPack {

        default Object unpack(byte[] data) throws IOException, ClassNotFoundException {
            return unpack(data, false);
        }

        Object unpack(byte[] data, boolean isReturnTriException) throws IOException, ClassNotFoundException;


    }

    interface UnPack {

        /**
         * @param data byte array
         * @return object instance
         * @throws IOException            IOException
         * @throws ClassNotFoundException when no class found
         */
        Object unpack(byte[] data) throws IOException, ClassNotFoundException;

    }

    default Object parseRequest(byte[] data) throws IOException, ClassNotFoundException {
        return getRequestUnpack().unpack(data);
    }

    default Object parseResponse(byte[] data) throws IOException, ClassNotFoundException {
        return parseResponse(data, false);
    }

    default Object parseResponse(byte[] data, boolean isReturnTriException) throws IOException, ClassNotFoundException {
        UnPack unPack = getResponseUnpack();
        if (unPack instanceof WrapperUnPack) {
            return ((WrapperUnPack) unPack).unpack(data, isReturnTriException);
        }
        return unPack.unpack(data);
    }

    default byte[] packRequest(Object request) throws IOException {
        return getRequestPack().pack(request);
    }

    default byte[] packResponse(Object response) throws IOException {
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
