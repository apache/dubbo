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

import org.apache.dubbo.rpc.model.Pack;

import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

public class PbArrayPacker implements Pack {

    private final boolean singleArgument;

    public PbArrayPacker(boolean singleArgument) {
        this.singleArgument = singleArgument;
    }

    @Override
    public ByteBuf pack(Object obj, ByteBufAllocator allocator) throws Exception {
        if (!singleArgument) {
            obj = ((Object[]) obj)[0];
        }
        Message message = (Message) obj;

        // Zero-copy: serialize directly to ByteBuf
        ByteBuf buffer = allocator.buffer(message.getSerializedSize());
        try (ByteBufOutputStream outputStream = new ByteBufOutputStream(buffer)) {
            message.writeTo(outputStream);
            return buffer;
        } catch (Exception e) {
            buffer.release();
            throw e;
        }
    }

    @Override
    @Deprecated
    public byte[] pack(Object obj) throws Exception {
        ByteBuf buffer = pack(obj, Unpooled.buffer().alloc());
        try {
            byte[] result = new byte[buffer.readableBytes()];
            buffer.readBytes(result);
            return result;
        } finally {
            buffer.release();
        }
    }
}
