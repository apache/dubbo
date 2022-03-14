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

import org.apache.dubbo.rpc.protocol.tri.SingleProtobufUtils;
import org.apache.dubbo.triple.TripleWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class PbUnpack<T> {
    public static PbUnpack<TripleWrapper.TripleResponseWrapper> RESP_PB_UNPACK = new PbUnpack<>(TripleWrapper.TripleResponseWrapper.class);
    public static PbUnpack<TripleWrapper.TripleRequestWrapper> REQ_PB_UNPACK = new PbUnpack<>(TripleWrapper.TripleRequestWrapper.class);
    private final Class<?> clz;

    public PbUnpack(Class<?> clz) {
        this.clz = clz;
    }

    public T unpack(byte[] data) throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(data);
        return (T) SingleProtobufUtils.deserialize(bais, clz);
    }
}
