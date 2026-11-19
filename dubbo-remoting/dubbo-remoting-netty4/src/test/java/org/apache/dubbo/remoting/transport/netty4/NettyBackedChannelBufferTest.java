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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.remoting.buffer.ChannelBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NettyBackedChannelBufferTest {

    private static final int CAPACITY = 4096;

    private ChannelBuffer buffer;

    @BeforeEach
    public void init() {
        buffer = new NettyBackedChannelBuffer(Unpooled.buffer(CAPACITY, CAPACITY * 2));
    }

    @AfterEach
    public void dispose() {
        buffer = null;
    }

    @Test
    void testBufferTransfer() {
        byte[] tmp1 = {1, 2};
        byte[] tmp2 = {3, 4};
        ChannelBuffer source = new NettyBackedChannelBuffer(Unpooled.buffer(2, 4));
        source.writeBytes(tmp1);
        buffer.writeBytes(tmp2);

        assertEquals(2, buffer.readableBytes());
        source.setBytes(0, tmp1, 0, 2);

        buffer.setBytes(0, source, 0, 2);
        assertEquals(2, buffer.readableBytes());

        byte[] actual = new byte[2];
        buffer.getBytes(0, actual);
        assertEquals(1, actual[0]);
        assertEquals(2, actual[1]);
    }

    @Test
    void testBufferTransfer_directToDirect() {
        ByteBuf srcDirect = Unpooled.directBuffer(4);
        ByteBuf dstDirect = Unpooled.directBuffer(4);

        try {
            ChannelBuffer source = new NettyBackedChannelBuffer(srcDirect);
            ChannelBuffer target = new NettyBackedChannelBuffer(dstDirect);

            byte[] data = {10, 20, 30, 40};
            source.writeBytes(data);

            target.setBytes(0, source, 0, 4);

            byte[] actual = new byte[4];
            target.getBytes(0, actual);

            assertArrayEquals(data, actual);
            assertEquals(0, target.readerIndex(), "setBytes should not move readerIndex");
        } finally {
            srcDirect.release();
            dstDirect.release();
        }
    }

    @Test
    void testReadBytes_directBuffer() {
        byte[] data = {1, 2, 3, 4};
        buffer.writeBytes(data);

        byte[] actual = new byte[4];
        buffer.readBytes(actual);

        assertArrayEquals(data, actual);
        assertEquals(4, buffer.readerIndex());
    }

    @Test
    void testGetBytes_directBuffer_shouldNotMoveIndex() {
        buffer.writeBytes(new byte[] {5, 6, 7, 8});

        int before = buffer.readerIndex();

        byte[] actual = new byte[2];
        buffer.getBytes(1, actual);

        assertEquals(before, buffer.readerIndex());
        assertArrayEquals(new byte[] {6, 7}, actual);
    }
}
