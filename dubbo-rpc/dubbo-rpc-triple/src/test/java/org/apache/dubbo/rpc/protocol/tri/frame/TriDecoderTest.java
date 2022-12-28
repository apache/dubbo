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

package org.apache.dubbo.rpc.protocol.tri.frame;

import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TriDecoderTest {

    @Test
    void decode() {
        final RecordListener listener = new RecordListener();
        TriDecoder decoder = new TriDecoder(DeCompressor.NONE, listener);
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0);
        buf.writeInt(1);
        buf.writeByte(2);
        decoder.deframe(buf);
        final ByteBuf buf2 = Unpooled.buffer();
        buf2.writeByte(0);
        buf2.writeInt(2);
        buf2.writeByte(2);
        buf2.writeByte(3);
        decoder.deframe(buf2);
        Assertions.assertEquals(0, listener.dataCount);
        decoder.request(1);
        Assertions.assertEquals(1, listener.dataCount);
        Assertions.assertEquals(1, listener.lastData.length);
        decoder.request(1);
        Assertions.assertEquals(2, listener.dataCount);
        Assertions.assertEquals(2, listener.lastData.length);
        decoder.close();
    }

}
