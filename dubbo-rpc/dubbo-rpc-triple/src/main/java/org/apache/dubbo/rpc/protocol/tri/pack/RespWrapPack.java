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

import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.ByteString;

import java.io.IOException;

public class RespWrapPack implements Pack {
    private final MultipleGenericPack genericPack;
    private final PbPack genericPbPack;
    private final Class<?> returnType;

    public RespWrapPack(MultipleGenericPack genericPack, PbPack genericPbPack, Class<?> returnType) {
        this.genericPack = genericPack;
        this.genericPbPack = genericPbPack;
        this.returnType = returnType;
    }

    @Override
    public byte[] pack(Object obj) throws IOException {
        final TripleWrapper.TripleResponseWrapper.Builder builder = TripleWrapper.TripleResponseWrapper.newBuilder()
            .setType(returnType.getName())
            .setSerializeType(genericPack.serializationName);
        builder.setData(ByteString.copyFrom(genericPack.pack(obj)));
        return genericPbPack.pack(builder.build());
    }

}
