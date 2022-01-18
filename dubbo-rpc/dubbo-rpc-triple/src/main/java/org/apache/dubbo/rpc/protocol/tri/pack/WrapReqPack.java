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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class WrapReqPack implements Pack {
    private final Class<?>[] parameterTypes;
    private final GenericPack genericPack;
    private final PbPack pbPack;

    public WrapReqPack(Class<?>[] parameterTypes, GenericPack genericPack, PbPack genericPbPack) {
        this.parameterTypes = parameterTypes;
        this.genericPack = genericPack;
        this.pbPack = genericPbPack;
    }

    @Override
    public byte[] pack(Object obj) throws IOException {
        final TripleWrapper.TripleRequestWrapper.Builder builder = TripleWrapper.TripleRequestWrapper.newBuilder()
            .setSerializeType(genericPack.serializationName);
        Object[] arguments = (Object[]) obj;
        for (int i = 0; i < arguments.length; i++) {
            builder.addArgTypes(parameterTypes[i].getName());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(genericPack.pack(arguments[i]));
            builder.addArgs(ByteString.copyFrom(bos.toByteArray()));
        }
        return pbPack.pack(builder.build());
    }
}
