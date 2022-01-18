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

public class WrapReqUnPack implements Unpack<Object[]> {
    private final GenericUnpack genericPack;
    private final PbUnpack<TripleWrapper.TripleRequestWrapper> pbUnpack;

    public WrapReqUnPack(GenericUnpack genericPack, PbUnpack<TripleWrapper.TripleRequestWrapper> pbUnpack) {
        this.genericPack = genericPack;
        this.pbUnpack = pbUnpack;
    }

    @Override
    public Object[] unpack(byte[] data) throws IOException, ClassNotFoundException {
        final TripleWrapper.TripleRequestWrapper wrapper = pbUnpack.unpack(data);
        Object[] arguments = new Object[wrapper.getArgsCount()];
        for (int i = 0; i < arguments.length; i++) {
            byte[] argument = wrapper.getArgs(i).toByteArray();
            arguments[i] = genericPack.unpack(argument, wrapper.getSerializeType(), wrapper.getArgTypes(i));
        }
        return arguments;
    }
}
