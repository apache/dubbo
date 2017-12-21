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
package com.alibaba.dubbo.remoting.buffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import static com.alibaba.dubbo.remoting.buffer.ChannelBuffers.directBuffer;
import static com.alibaba.dubbo.remoting.buffer.ChannelBuffers.wrappedBuffer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 *
 */
public abstract class AbstractChannelBufferTest {

    private static final int CAPACITY = 4096; // Must be even
    private static final int BLOCK_SIZE = 128;

    private long seed;
    private Random random;
    private ChannelBuffer buffer;

    protected abstract ChannelBuffer newBuffer(int capacity);

    protected abstract ChannelBuffer[] components();

    protected boolean discardReadBytesDoesNotMoveWritableBytes() {
        return true;
    }


    @Before
    public void init() {
        buffer = newBuffer(CAPACITY);
        seed = System.currentTimeMillis();
        random = new Random(seed);
    }

    @After
    public void dispose() {
        buffer = null;
    }

    @Test
    public void initialState() {
        assertEquals(CAPACITY, buffer.capacity());
        assertEquals(0, buffer.readerIndex());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void readerIndexBoundaryCheck1() {
        try {
            buffer.writerIndex(0);
        } catch (IndexOutOfBoundsException e) {
            fail();
        }
        buffer.readerIndex(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void readerIndexBoundaryCheck2() {
        try {
            buffer.writerIndex(buffer.capacity());
        } catch (IndexOutOfBoundsException e) {
            fail();
        }
        buffer.readerIndex(buffer.capacity() + 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void readerIndexBoundaryCheck3() {
        try {
            buffer.writerIndex(CAPACITY / 2);
        } catch (IndexOutOfBoundsException e) {
            fail();
        }
        buffer.readerIndex(CAPACITY * 3 / 2);
    }

    @Test
    public void readerIndexBoundaryCheck4() {
        buffer.writerIndex(0);
        buffer.readerIndex(0);
        buffer.writerIndex(buffer.capacity());
        buffer.readerIndex(buffer.capacity());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void writerIndexBoundaryCheck1() {
        buffer.writerIndex(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void writerIndexBoundaryCheck2() {
        try {
            buffer.writerIndex(CAPACITY);
            buffer.readerIndex(CAPACITY);
        } catch (IndexOutOfBoundsException e) {
            fail();
        }
        buffer.writerIndex(buffer.capacity() + 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void writerIndexBoundaryCheck3() {
        try {
            buffer.writerIndex(CAPACITY);
            buffer.readerIndex(CAPACITY / 2);
        } catch (IndexOutOfBoundsException e) {
            fail();
        }
        buffer.writerIndex(CAPACITY / 4);
    }

    @Test
    public void writerIndexBoundaryCheck4() {
        buffer.writerIndex(0);
        buffer.readerIndex(0);
        buffer.writerIndex(CAPACITY);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getByteBoundaryCheck1() {
        buffer.getByte(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getByteBoundaryCheck2() {
        buffer.getByte(buffer.capacity());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getByteArrayBoundaryCheck1() {
        buffer.getBytes(-1, new byte[0]);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getByteArrayBoundaryCheck2() {
        buffer.getBytes(-1, new byte[0], 0, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getByteBufferBoundaryCheck() {
        buffer.getBytes(-1, ByteBuffer.allocate(0));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void copyBoundaryCheck1() {
        buffer.copy(-1, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void copyBoundaryCheck2() {
        buffer.copy(0, buffer.capacity() + 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void copyBoundaryCheck3() {
        buffer.copy(buffer.capacity() + 1, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void copyBoundaryCheck4() {
        buffer.copy(buffer.capacity(), 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void setIndexBoundaryCheck1() {
        buffer.setIndex(-1, CAPACITY);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void setIndexBoundaryCheck2() {
        buffer.setIndex(CAPACITY / 2, CAPACITY / 4);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void setIndexBoundaryCheck3() {
        buffer.setIndex(0, CAPACITY + 1);
    }

    @Test
    public void getByteBufferState() {
        ByteBuffer dst = ByteBuffer.allocate(4);
        dst.position(1);
        dst.limit(3);

        buffer.setByte(0, (byte) 1);
        buffer.setByte(1, (byte) 2);
        buffer.setByte(2, (byte) 3);
        buffer.setByte(3, (byte) 4);
        buffer.getBytes(1, dst);

        assertEquals(3, dst.position());
        assertEquals(3, dst.limit());

        dst.clear();
        assertEquals(0, dst.get(0));
        assertEquals(2, dst.get(1));
        assertEquals(3, dst.get(2));
        assertEquals(0, dst.get(3));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getDirectByteBufferBoundaryCheck() {
        buffer.getBytes(-1, ByteBuffer.allocateDirect(0));
    }

    @Test
    public void getDirectByteBufferState() {
        ByteBuffer dst = ByteBuffer.allocateDirect(4);
        dst.position(1);
        dst.limit(3);

        buffer.setByte(0, (byte) 1);
        buffer.setByte(1, (byte) 2);
        buffer.setByte(2, (byte) 3);
        buffer.setByte(3, (byte) 4);
        buffer.getBytes(1, dst);

        assertEquals(3, dst.position());
        assertEquals(3, dst.limit());

        dst.clear();
        assertEquals(0, dst.get(0));
        assertEquals(2, dst.get(1));
        assertEquals(3, dst.get(2));
        assertEquals(0, dst.get(3));
    }

    @Test
    public void testRandomByteAccess() {
        for (int i = 0; i < buffer.capacity(); i++) {
            byte value = (byte) random.nextInt();
            buffer.setByte(i, value);
        }

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity(); i++) {
            byte value = (byte) random.nextInt();
            assertEquals(value, buffer.getByte(i));
        }
    }

    @Test
    public void testSequentialByteAccess() {
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity(); i++) {
            byte value = (byte) random.nextInt();
            assertEquals(i, buffer.writerIndex());
            assertTrue(buffer.writable());
            buffer.writeByte(value);
        }

        assertEquals(0, buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.writable());

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity(); i++) {
            byte value = (byte) random.nextInt();
            assertEquals(i, buffer.readerIndex());
            assertTrue(buffer.readable());
            assertEquals(value, buffer.readByte());
        }

        assertEquals(buffer.capacity(), buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.readable());
        assertFalse(buffer.writable());
    }

    @Test
    public void testByteArrayTransfer() {
        byte[] value = new byte[BLOCK_SIZE * 2];
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(value);
            buffer.setBytes(i, value, random.nextInt(BLOCK_SIZE), BLOCK_SIZE);
        }

        random.setSeed(seed);
        byte[] expectedValue = new byte[BLOCK_SIZE * 2];
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValue);
            int valueOffset = random.nextInt(BLOCK_SIZE);
            buffer.getBytes(i, value, valueOffset, BLOCK_SIZE);
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j++) {
                assertEquals(expectedValue[j], value[j]);
            }
        }
    }

    @Test
    public void testRandomByteArrayTransfer1() {
        byte[] value = new byte[BLOCK_SIZE];
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(value);
            buffer.setBytes(i, value);
        }

        random.setSeed(seed);
        byte[] expectedValueContent = new byte[BLOCK_SIZE];
        ChannelBuffer expectedValue = wrappedBuffer(expectedValueContent);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValueContent);
            buffer.getBytes(i, value);
            for (int j = 0; j < BLOCK_SIZE; j++) {
                assertEquals(expectedValue.getByte(j), value[j]);
            }
        }
    }

    @Test
    public void testRandomByteArrayTransfer2() {
        byte[] value = new byte[BLOCK_SIZE * 2];
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(value);
            buffer.setBytes(i, value, random.nextInt(BLOCK_SIZE), BLOCK_SIZE);
        }

        random.setSeed(seed);
        byte[] expectedValueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer expectedValue = wrappedBuffer(expectedValueContent);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValueContent);
            int valueOffset = random.nextInt(BLOCK_SIZE);
            buffer.getBytes(i, value, valueOffset, BLOCK_SIZE);
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j++) {
                assertEquals(expectedValue.getByte(j), value[j]);
            }
        }
    }

    @Test
    public void testRandomHeapBufferTransfer1() {
        byte[] valueContent = new byte[BLOCK_SIZE];
        ChannelBuffer value = wrappedBuffer(valueContent);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(valueContent);
            value.setIndex(0, BLOCK_SIZE);
            buffer.setBytes(i, value);
            assertEquals(BLOCK_SIZE, value.readerIndex());
            assertEquals(BLOCK_SIZE, value.writerIndex());
        }

        random.setSeed(seed);
        byte[] expectedValueContent = new byte[BLOCK_SIZE];
        ChannelBuffer expectedValue = wrappedBuffer(expectedValueContent);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValueContent);
            value.clear();
            buffer.getBytes(i, value);
            assertEquals(0, value.readerIndex());
            assertEquals(BLOCK_SIZE, value.writerIndex());
            for (int j = 0; j < BLOCK_SIZE; j++) {
                assertEquals(expectedValue.getByte(j), value.getByte(j));
            }
        }
    }

    @Test
    public void testRandomHeapBufferTransfer2() {
        byte[] valueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer value = wrappedBuffer(valueContent);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(valueContent);
            buffer.setBytes(i, value, random.nextInt(BLOCK_SIZE), BLOCK_SIZE);
        }

        random.setSeed(seed);
        byte[] expectedValueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer expectedValue = wrappedBuffer(expectedValueContent);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValueContent);
            int valueOffset = random.nextInt(BLOCK_SIZE);
            buffer.getBytes(i, value, valueOffset, BLOCK_SIZE);
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j++) {
                assertEquals(expectedValue.getByte(j), value.getByte(j));
            }
        }
    }

    @Test
    public void testRandomDirectBufferTransfer() {
        byte[] tmp = new byte[BLOCK_SIZE * 2];
        ChannelBuffer value = directBuffer(BLOCK_SIZE * 2);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(tmp);
            value.setBytes(0, tmp, 0, value.capacity());
            buffer.setBytes(i, value, random.nextInt(BLOCK_SIZE), BLOCK_SIZE);
        }

        random.setSeed(seed);
        ChannelBuffer expectedValue = directBuffer(BLOCK_SIZE * 2);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(tmp);
            expectedValue.setBytes(0, tmp, 0, expectedValue.capacity());
            int valueOffset = random.nextInt(BLOCK_SIZE);
            buffer.getBytes(i, value, valueOffset, BLOCK_SIZE);
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j++) {
                assertEquals(expectedValue.getByte(j), value.getByte(j));
            }
        }
    }

    @Test
    public void testRandomByteBufferTransfer() {
        ByteBuffer value = ByteBuffer.allocate(BLOCK_SIZE * 2);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(value.array());
            value.clear().position(random.nextInt(BLOCK_SIZE));
            value.limit(value.position() + BLOCK_SIZE);
            buffer.setBytes(i, value);
        }

        random.setSeed(seed);
        ByteBuffer expectedValue = ByteBuffer.allocate(BLOCK_SIZE * 2);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValue.array());
            int valueOffset = random.nextInt(BLOCK_SIZE);
            value.clear().position(valueOffset).limit(valueOffset + BLOCK_SIZE);
            buffer.getBytes(i, value);
            assertEquals(valueOffset + BLOCK_SIZE, value.position());
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j++) {
                assertEquals(expectedValue.get(j), value.get(j));
            }
        }
    }

    @Test
    public void testSequentialByteArrayTransfer1() {
        byte[] value = new byte[BLOCK_SIZE];
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(value);
            assertEquals(0, buffer.readerIndex());
            assertEquals(i, buffer.writerIndex());
            buffer.writeBytes(value);
        }

        random.setSeed(seed);
        byte[] expectedValue = new byte[BLOCK_SIZE];
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValue);
            assertEquals(i, buffer.readerIndex());
            assertEquals(CAPACITY, buffer.writerIndex());
            buffer.readBytes(value);
            for (int j = 0; j < BLOCK_SIZE; j++) {
                assertEquals(expectedValue[j], value[j]);
            }
        }
    }

    @Test
    public void testSequentialByteArrayTransfer2() {
        byte[] value = new byte[BLOCK_SIZE * 2];
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(value);
            assertEquals(0, buffer.readerIndex());
            assertEquals(i, buffer.writerIndex());
            int readerIndex = random.nextInt(BLOCK_SIZE);
            buffer.writeBytes(value, readerIndex, BLOCK_SIZE);
        }

        random.setSeed(seed);
        byte[] expectedValue = new byte[BLOCK_SIZE * 2];
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValue);
            int valueOffset = random.nextInt(BLOCK_SIZE);
            assertEquals(i, buffer.readerIndex());
            assertEquals(CAPACITY, buffer.writerIndex());
            buffer.readBytes(value, valueOffset, BLOCK_SIZE);
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j++) {
                assertEquals(expectedValue[j], value[j]);
            }
        }
    }

    @Test
    public void testSequentialHeapBufferTransfer1() {
        byte[] valueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer value = wrappedBuffer(valueContent);
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(valueContent);
            assertEquals(0, buffer.readerIndex());
            assertEquals(i, buffer.writerIndex());
            buffer.writeBytes(value, random.nextInt(BLOCK_SIZE), BLOCK_SIZE);
            assertEquals(0, value.readerIndex());
            assertEquals(valueContent.length, value.writerIndex());
        }

        random.setSeed(seed);
        byte[] expectedValueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer expectedValue = wrappedBuffer(expectedValueContent);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValueContent);
            int valueOffset = random.nextInt(BLOCK_SIZE);
            assertEquals(i, buffer.readerIndex());
            assertEquals(CAPACITY, buffer.writerIndex());
            buffer.readBytes(value, valueOffset, BLOCK_SIZE);
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j++) {
                assertEquals(expectedValue.getByte(j), value.getByte(j));
            }
            assertEquals(0, value.readerIndex());
            assertEquals(valueContent.length, value.writerIndex());
        }
    }

    @Test
    public void testSequentialHeapBufferTransfer2() {
        byte[] valueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer value = wrappedBuffer(valueContent);
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(valueContent);
            assertEquals(0, buffer.readerIndex());
            assertEquals(i, buffer.writerIndex());
            int readerIndex = random.nextInt(BLOCK_SIZE);
            value.readerIndex(readerIndex);
            value.writerIndex(readerIndex + BLOCK_SIZE);
            buffer.writeBytes(value);
            assertEquals(readerIndex + BLOCK_SIZE, value.writerIndex());
            assertEquals(value.writerIndex(), value.readerIndex());
        }

        random.setSeed(seed);
        byte[] expectedValueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer expectedValue = wrappedBuffer(expectedValueContent);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValueContent);
            int valueOffset = random.nextInt(BLOCK_SIZE);
            assertEquals(i, buffer.readerIndex());
            assertEquals(CAPACITY, buffer.writerIndex());
            value.readerIndex(valueOffset);
            value.writerIndex(valueOffset);
            buffer.readBytes(value, BLOCK_SIZE);
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j++) {
                assertEquals(expectedValue.getByte(j), value.getByte(j));
            }
            assertEquals(valueOffset, value.readerIndex());
            assertEquals(valueOffset + BLOCK_SIZE, value.writerIndex());
        }
    }

    @Test
    public void testSequentialDirectBufferTransfer1() {
        byte[] valueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer value = directBuffer(BLOCK_SIZE * 2);
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(valueContent);
            value.setBytes(0, valueContent);
            assertEquals(0, buffer.readerIndex());
            assertEquals(i, buffer.writerIndex());
            buffer.writeBytes(value, random.nextInt(BLOCK_SIZE), BLOCK_SIZE);
            assertEquals(0, value.readerIndex());
            assertEquals(0, value.writerIndex());
        }

        random.setSeed(seed);
        byte[] expectedValueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer expectedValue = wrappedBuffer(expectedValueContent);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValueContent);
            int valueOffset = random.nextInt(BLOCK_SIZE);
            value.setBytes(0, valueContent);
            assertEquals(i, buffer.readerIndex());
            assertEquals(CAPACITY, buffer.writerIndex());
            buffer.readBytes(value, valueOffset, BLOCK_SIZE);
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j++) {
                assertEquals(expectedValue.getByte(j), value.getByte(j));
            }
            assertEquals(0, value.readerIndex());
            assertEquals(0, value.writerIndex());
        }
    }

    @Test
    public void testSequentialDirectBufferTransfer2() {
        byte[] valueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer value = directBuffer(BLOCK_SIZE * 2);
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(valueContent);
            value.setBytes(0, valueContent);
            assertEquals(0, buffer.readerIndex());
            assertEquals(i, buffer.writerIndex());
            int readerIndex = random.nextInt(BLOCK_SIZE);
            value.readerIndex(0);
            value.writerIndex(readerIndex + BLOCK_SIZE);
            value.readerIndex(readerIndex);
            buffer.writeBytes(value);
            assertEquals(readerIndex + BLOCK_SIZE, value.writerIndex());
            assertEquals(value.writerIndex(), value.readerIndex());
        }

        random.setSeed(seed);
        byte[] expectedValueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer expectedValue = wrappedBuffer(expectedValueContent);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValueContent);
            value.setBytes(0, valueContent);
            int valueOffset = random.nextInt(BLOCK_SIZE);
            assertEquals(i, buffer.readerIndex());
            assertEquals(CAPACITY, buffer.writerIndex());
            value.readerIndex(valueOffset);
            value.writerIndex(valueOffset);
            buffer.readBytes(value, BLOCK_SIZE);
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j++) {
                assertEquals(expectedValue.getByte(j), value.getByte(j));
            }
            assertEquals(valueOffset, value.readerIndex());
            assertEquals(valueOffset + BLOCK_SIZE, value.writerIndex());
        }
    }

    @Test
    public void testSequentialByteBufferBackedHeapBufferTransfer1() {
        byte[] valueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer value = wrappedBuffer(ByteBuffer.allocate(BLOCK_SIZE * 2));
        value.writerIndex(0);
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(valueContent);
            value.setBytes(0, valueContent);
            assertEquals(0, buffer.readerIndex());
            assertEquals(i, buffer.writerIndex());
            buffer.writeBytes(value, random.nextInt(BLOCK_SIZE), BLOCK_SIZE);
            assertEquals(0, value.readerIndex());
            assertEquals(0, value.writerIndex());
        }

        random.setSeed(seed);
        byte[] expectedValueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer expectedValue = wrappedBuffer(expectedValueContent);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValueContent);
            int valueOffset = random.nextInt(BLOCK_SIZE);
            value.setBytes(0, valueContent);
            assertEquals(i, buffer.readerIndex());
            assertEquals(CAPACITY, buffer.writerIndex());
            buffer.readBytes(value, valueOffset, BLOCK_SIZE);
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j++) {
                assertEquals(expectedValue.getByte(j), value.getByte(j));
            }
            assertEquals(0, value.readerIndex());
            assertEquals(0, value.writerIndex());
        }
    }

    @Test
    public void testSequentialByteBufferBackedHeapBufferTransfer2() {
        byte[] valueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer value = wrappedBuffer(ByteBuffer.allocate(BLOCK_SIZE * 2));
        value.writerIndex(0);
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(valueContent);
            value.setBytes(0, valueContent);
            assertEquals(0, buffer.readerIndex());
            assertEquals(i, buffer.writerIndex());
            int readerIndex = random.nextInt(BLOCK_SIZE);
            value.readerIndex(0);
            value.writerIndex(readerIndex + BLOCK_SIZE);
            value.readerIndex(readerIndex);
            buffer.writeBytes(value);
            assertEquals(readerIndex + BLOCK_SIZE, value.writerIndex());
            assertEquals(value.writerIndex(), value.readerIndex());
        }

        random.setSeed(seed);
        byte[] expectedValueContent = new byte[BLOCK_SIZE * 2];
        ChannelBuffer expectedValue = wrappedBuffer(expectedValueContent);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValueContent);
            value.setBytes(0, valueContent);
            int valueOffset = random.nextInt(BLOCK_SIZE);
            assertEquals(i, buffer.readerIndex());
            assertEquals(CAPACITY, buffer.writerIndex());
            value.readerIndex(valueOffset);
            value.writerIndex(valueOffset);
            buffer.readBytes(value, BLOCK_SIZE);
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j++) {
                assertEquals(expectedValue.getByte(j), value.getByte(j));
            }
            assertEquals(valueOffset, value.readerIndex());
            assertEquals(valueOffset + BLOCK_SIZE, value.writerIndex());
        }
    }

    @Test
    public void testSequentialByteBufferTransfer() {
        buffer.writerIndex(0);
        ByteBuffer value = ByteBuffer.allocate(BLOCK_SIZE * 2);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(value.array());
            value.clear().position(random.nextInt(BLOCK_SIZE));
            value.limit(value.position() + BLOCK_SIZE);
            buffer.writeBytes(value);
        }

        random.setSeed(seed);
        ByteBuffer expectedValue = ByteBuffer.allocate(BLOCK_SIZE * 2);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValue.array());
            int valueOffset = random.nextInt(BLOCK_SIZE);
            value.clear().position(valueOffset).limit(valueOffset + BLOCK_SIZE);
            buffer.readBytes(value);
            assertEquals(valueOffset + BLOCK_SIZE, value.position());
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j++) {
                assertEquals(expectedValue.get(j), value.get(j));
            }
        }
    }

    @Test
    public void testSequentialCopiedBufferTransfer1() {
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            byte[] value = new byte[BLOCK_SIZE];
            random.nextBytes(value);
            assertEquals(0, buffer.readerIndex());
            assertEquals(i, buffer.writerIndex());
            buffer.writeBytes(value);
        }

        random.setSeed(seed);
        byte[] expectedValue = new byte[BLOCK_SIZE];
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValue);
            assertEquals(i, buffer.readerIndex());
            assertEquals(CAPACITY, buffer.writerIndex());
            ChannelBuffer actualValue = buffer.readBytes(BLOCK_SIZE);
            assertEquals(wrappedBuffer(expectedValue), actualValue);

            // Make sure if it is a copied buffer.
            actualValue.setByte(0, (byte) (actualValue.getByte(0) + 1));
            assertFalse(buffer.getByte(i) == actualValue.getByte(0));
        }
    }

    @Test
    public void testStreamTransfer1() throws Exception {
        byte[] expected = new byte[buffer.capacity()];
        random.nextBytes(expected);

        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            ByteArrayInputStream in = new ByteArrayInputStream(expected, i, BLOCK_SIZE);
            assertEquals(BLOCK_SIZE, buffer.setBytes(i, in, BLOCK_SIZE));
            assertEquals(-1, buffer.setBytes(i, in, 0));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            buffer.getBytes(i, out, BLOCK_SIZE);
        }

        assertTrue(Arrays.equals(expected, out.toByteArray()));
    }

    @Test
    public void testStreamTransfer2() throws Exception {
        byte[] expected = new byte[buffer.capacity()];
        random.nextBytes(expected);
        buffer.clear();

        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            ByteArrayInputStream in = new ByteArrayInputStream(expected, i, BLOCK_SIZE);
            assertEquals(i, buffer.writerIndex());
            buffer.writeBytes(in, BLOCK_SIZE);
            assertEquals(i + BLOCK_SIZE, buffer.writerIndex());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            assertEquals(i, buffer.readerIndex());
            buffer.readBytes(out, BLOCK_SIZE);
            assertEquals(i + BLOCK_SIZE, buffer.readerIndex());
        }

        assertTrue(Arrays.equals(expected, out.toByteArray()));
    }

    @Test
    public void testCopy() {
        for (int i = 0; i < buffer.capacity(); i++) {
            byte value = (byte) random.nextInt();
            buffer.setByte(i, value);
        }

        final int readerIndex = CAPACITY / 3;
        final int writerIndex = CAPACITY * 2 / 3;
        buffer.setIndex(readerIndex, writerIndex);

        // Make sure all properties are copied.
        ChannelBuffer copy = buffer.copy();
        assertEquals(0, copy.readerIndex());
        assertEquals(buffer.readableBytes(), copy.writerIndex());
        assertEquals(buffer.readableBytes(), copy.capacity());
        for (int i = 0; i < copy.capacity(); i++) {
            assertEquals(buffer.getByte(i + readerIndex), copy.getByte(i));
        }

        // Make sure the buffer content is independent from each other.
        buffer.setByte(readerIndex, (byte) (buffer.getByte(readerIndex) + 1));
        assertTrue(buffer.getByte(readerIndex) != copy.getByte(0));
        copy.setByte(1, (byte) (copy.getByte(1) + 1));
        assertTrue(buffer.getByte(readerIndex + 1) != copy.getByte(1));
    }

    @Test
    public void testToByteBuffer1() {
        byte[] value = new byte[buffer.capacity()];
        random.nextBytes(value);
        buffer.clear();
        buffer.writeBytes(value);

        assertEquals(ByteBuffer.wrap(value), buffer.toByteBuffer());
    }

    @Test
    public void testToByteBuffer2() {
        byte[] value = new byte[buffer.capacity()];
        random.nextBytes(value);
        buffer.clear();
        buffer.writeBytes(value);

        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            assertEquals(ByteBuffer.wrap(value, i, BLOCK_SIZE), buffer.toByteBuffer(i, BLOCK_SIZE));
        }
    }

    @Test
    public void testSkipBytes1() {
        buffer.setIndex(CAPACITY / 4, CAPACITY / 2);

        buffer.skipBytes(CAPACITY / 4);
        assertEquals(CAPACITY / 4 * 2, buffer.readerIndex());

        try {
            buffer.skipBytes(CAPACITY / 4 + 1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        // Should remain unchanged.
        assertEquals(CAPACITY / 4 * 2, buffer.readerIndex());
    }

}
