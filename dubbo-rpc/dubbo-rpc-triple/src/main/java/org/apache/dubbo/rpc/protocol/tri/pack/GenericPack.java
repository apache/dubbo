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

package org.apache.dubbo.rpc.protocol.tri.pack;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class GenericPack implements Pack {
    public final String serializationName;
    private final MultipleSerialization serialization;
    private final String innerSerializationName;
    private final URL url;

    public GenericPack(MultipleSerialization serialization, String serializationName, URL url) {
        this.serialization = serialization;
        this.serializationName = serializationName;
        this.innerSerializationName = convertHessianFromWrapper(serializationName);
        this.url = url;
    }

    @Override
    public byte[] pack(Object obj) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serialization.serialize(url, innerSerializationName, null, obj, baos);
        return baos.toByteArray();
    }



    /**
     * Convert hessian version from Dubbo's SPI version(hessian2) to wrapper API version (hessian4)
     *
     * @param serializeType literal type
     * @return hessian4 if the param is hessian2, otherwise return the param
     */
    private String convertHessianToWrapper(String serializeType) {
        if (TripleConstant.HESSIAN2.equals(serializeType)) {
            return TripleConstant.HESSIAN4;
        }
        return serializeType;
    }

    protected String convertHessianFromWrapper(String serializeType) {
        if (TripleConstant.HESSIAN4.equals(serializeType)) {
            return TripleConstant.HESSIAN2;
        }
        return serializeType;
    }
}
