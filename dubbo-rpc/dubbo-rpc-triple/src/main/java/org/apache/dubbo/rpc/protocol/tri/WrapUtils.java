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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class WrapUtils {

    public static Object getResponse(GenericUnpack genericUnpack,
                                     TripleWrapper.TripleResponseWrapper wrapper) throws IOException, ClassNotFoundException {
        return genericUnpack.unpack(wrapper.getData().toByteArray(), wrapper.getSerializeType(), wrapper.getType());
    }

    public static Object[] getRequest(GenericUnpack genericUnpack,
                                      TripleWrapper.TripleRequestWrapper wrapper) throws IOException, ClassNotFoundException {
        Object[] arguments = new Object[wrapper.getArgsCount()];
        for (int i = 0; i < arguments.length; i++) {
            byte[] argument = wrapper.getArgs(i).toByteArray();
            arguments[i] = genericUnpack.unpack(argument, wrapper.getSerializeType(), wrapper.getArgTypes(i));
        }
        return arguments;
    }

    public static Message getRequest(GenericPack genericPack, MethodDescriptor md, Object[] arguments) throws IOException {
        if (md.isNeedWrap()) {
            final TripleWrapper.TripleRequestWrapper.Builder builder = TripleWrapper.TripleRequestWrapper.newBuilder()
                .setSerializeType(genericPack.serializationName);
            for (int i = 0; i < arguments.length; i++) {
                builder.addArgTypes(md.getParameterClasses()[i].getName());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bos.write(genericPack.pack(arguments[i]));
                builder.addArgs(ByteString.copyFrom(bos.toByteArray()));
            }
            return builder.build();
        } else {
            return (Message) arguments[0];
        }
    }

    public static Message getResponse(GenericPack genericPack, Object response) throws IOException {
        final TripleWrapper.TripleResponseWrapper.Builder builder = TripleWrapper.TripleResponseWrapper.newBuilder()
            .setSerializeType(genericPack.serializationName)
            .setData(ByteString.copyFrom(genericPack.pack(response)));
        return builder.build();
    }
}
