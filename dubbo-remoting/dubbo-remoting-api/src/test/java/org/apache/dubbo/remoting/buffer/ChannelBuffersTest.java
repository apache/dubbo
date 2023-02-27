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
package org.apache.dubbo.remoting.buffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.apache.dubbo.remoting.buffer.ChannelBuffers.DEFAULT_CAPACITY;
import static org.apache.dubbo.remoting.buffer.ChannelBuffers.EMPTY_BUFFER;

/**
 * {@link ChannelBuffers}
 */
class ChannelBuffersTest {
    @Test
    void testDynamicBuffer() {
        ChannelBuffer channelBuffer = ChannelBuffers.dynamicBuffer();
        Assertions.assertTrue(channelBuffer instanceof DynamicChannelBuffer);
        Assertions.assertEquals(channelBuffer.capacity(), DEFAULT_CAPACITY);

        channelBuffer = ChannelBuffers.dynamicBuffer(32, DirectChannelBufferFactory.getInstance());
        Assertions.assertTrue(channelBuffer instanceof DynamicChannelBuffer);
        Assertions.assertTrue(channelBuffer.isDirect());
        Assertions.assertEquals(channelBuffer.capacity(), 32);
    }

    @Test
    void testPrefixEquals(){
        ChannelBuffer bufA = ChannelBuffers.wrappedBuffer("abcedfaf".getBytes());
        ChannelBuffer bufB = ChannelBuffers.wrappedBuffer("abcedfaa".getBytes());
        Assertions.assertTrue(ChannelBuffers.equals(bufA, bufB));
        Assertions.assertTrue(ChannelBuffers.prefixEquals(bufA, bufB, 7));
        Assertions.assertFalse(ChannelBuffers.prefixEquals(bufA, bufB, 8));
    }

    @Test
    void testBuffer() {
        ChannelBuffer channelBuffer = ChannelBuffers.buffer(DEFAULT_CAPACITY);
        Assertions.assertTrue(channelBuffer instanceof HeapChannelBuffer);
        channelBuffer = ChannelBuffers.buffer(0);
        Assertions.assertEquals(channelBuffer, EMPTY_BUFFER);
    }

    @Test
    void testWrappedBuffer() {
        byte[] bytes = new byte[16];
        ChannelBuffer channelBuffer = ChannelBuffers.wrappedBuffer(bytes, 0, 15);
        Assertions.assertTrue(channelBuffer instanceof HeapChannelBuffer);
        Assertions.assertEquals(channelBuffer.capacity(), 15);

        channelBuffer = ChannelBuffers.wrappedBuffer(new byte[]{});
        Assertions.assertEquals(channelBuffer, EMPTY_BUFFER);

        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        channelBuffer = ChannelBuffers.wrappedBuffer(byteBuffer);
        Assertions.assertTrue(channelBuffer instanceof HeapChannelBuffer);

        byteBuffer = ByteBuffer.allocateDirect(16);
        channelBuffer = ChannelBuffers.wrappedBuffer(byteBuffer);
        Assertions.assertTrue(channelBuffer instanceof ByteBufferBackedChannelBuffer);

        byteBuffer.position(byteBuffer.limit());
        channelBuffer = ChannelBuffers.wrappedBuffer(byteBuffer);
        Assertions.assertEquals(channelBuffer, EMPTY_BUFFER);
    }

    @Test
    void testDirectBuffer() {
        ChannelBuffer channelBuffer = ChannelBuffers.directBuffer(0);
        Assertions.assertEquals(channelBuffer, EMPTY_BUFFER);

        channelBuffer = ChannelBuffers.directBuffer(16);
        Assertions.assertTrue(channelBuffer instanceof ByteBufferBackedChannelBuffer);
    }

    @Test
    void testEqualsHashCodeCompareMethod() {
        ChannelBuffer buffer1 = ChannelBuffers.buffer(4);
        byte[] bytes1 = new byte[]{1, 2, 3, 4};
        buffer1.writeBytes(bytes1);

        ChannelBuffer buffer2 = ChannelBuffers.buffer(4);
        byte[] bytes2 = new byte[]{1, 2, 3, 4};
        buffer2.writeBytes(bytes2);

        ChannelBuffer buffer3 = ChannelBuffers.buffer(3);
        byte[] bytes3 = new byte[]{1, 2, 3};
        buffer3.writeBytes(bytes3);

        ChannelBuffer buffer4 = ChannelBuffers.buffer(4);
        byte[] bytes4 = new byte[]{1, 2, 3, 5};
        buffer4.writeBytes(bytes4);

        Assertions.assertTrue(ChannelBuffers.equals(buffer1, buffer2));
        Assertions.assertFalse(ChannelBuffers.equals(buffer1, buffer3));
        Assertions.assertFalse(ChannelBuffers.equals(buffer1, buffer4));

        Assertions.assertTrue(ChannelBuffers.compare(buffer1, buffer2) == 0);
        Assertions.assertTrue(ChannelBuffers.compare(buffer1, buffer3) > 0);
        Assertions.assertTrue(ChannelBuffers.compare(buffer1, buffer4) < 0);

        Assertions.assertEquals(ChannelBuffers.hasCode(buffer1), ChannelBuffers.hasCode(buffer2));
        Assertions.assertNotEquals(ChannelBuffers.hasCode(buffer1), ChannelBuffers.hasCode(buffer3));
        Assertions.assertNotEquals(ChannelBuffers.hasCode(buffer1), ChannelBuffers.hasCode(buffer4));
    }
}
