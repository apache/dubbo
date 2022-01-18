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

import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.rpc.protocol.tri.SingleProtobufUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class GenericPbPack implements Pack {
    @Override
    public byte[] pack(Object obj) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SingleProtobufUtils.serialize(obj, baos);
        return baos.toByteArray();
    }

    @Override
    public Object unpack(byte[] data, Class<?> clz) throws IOException {
        return SingleProtobufUtils.deserialize(new ByteArrayInputStream(data), clz);
    }

    @Override
    public Object unpack(byte[] data, String clz) throws ClassNotFoundException, IOException {
        return SingleProtobufUtils.deserialize(new ByteArrayInputStream(data), ClassUtils.forName(clz));
    }
}
