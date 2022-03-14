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

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class GenericUnpack {
    public final MultipleSerialization serialization;
    private final URL url;

    public GenericUnpack(MultipleSerialization serialization, URL url) {
        this.serialization = serialization;
        this.url = url;
    }

    public Object unpack(byte[] data, String serializeType, String clz) throws ClassNotFoundException, IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(data);
        return serialization.deserialize(url, convertHessianFromWrapper(serializeType), clz, bais);
    }

    protected String convertHessianFromWrapper(String serializeType) {
        if (TripleConstant.HESSIAN4.equals(serializeType)) {
            return TripleConstant.HESSIAN2;
        }
        return serializeType;
    }
}
