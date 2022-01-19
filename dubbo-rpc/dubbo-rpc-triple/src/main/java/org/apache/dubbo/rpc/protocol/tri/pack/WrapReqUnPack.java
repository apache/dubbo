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

import java.io.IOException;

import static org.apache.dubbo.remoting.Constants.DEFAULT_REMOTING_SERIALIZATION;

public class WrapReqUnPack implements Unpack<Object[]> {
    private final GenericUnpack genericPack;
    public String serializeType = DEFAULT_REMOTING_SERIALIZATION;

    public WrapReqUnPack(GenericUnpack genericPack) {
        this.genericPack = genericPack;
    }

    @Override
    public Object[] unpack(byte[] data) throws IOException, ClassNotFoundException {
        final TripleWrapper.TripleRequestWrapper wrapper = PbUnpack.REQ_PB_UNPACK.unpack(data);
        Object[] arguments = new Object[wrapper.getArgsCount()];
        this.serializeType = wrapper.getSerializeType();
        for (int i = 0; i < arguments.length; i++) {
            byte[] argument = wrapper.getArgs(i).toByteArray();
            arguments[i] = genericPack.unpack(argument, wrapper.getSerializeType(), wrapper.getArgTypes(i));
        }
        return arguments;
    }
}
