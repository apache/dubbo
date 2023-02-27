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

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ChannelBufferStreamTest {
    
    @Test
    void testChannelBufferOutputStreamWithNull() {
        assertThrows(NullPointerException.class, () -> new ChannelBufferOutputStream(null));
    }
    
    @Test
    void testChannelBufferInputStreamWithNull() {
        assertThrows(NullPointerException.class, () -> new ChannelBufferInputStream(null));
    }
    
    @Test
    void testChannelBufferInputStreamWithNullAndLength() {
        assertThrows(NullPointerException.class, () -> new ChannelBufferInputStream(null, 0));
    }
    
    @Test
    void testChannelBufferInputStreamWithBadLength() {
        assertThrows(IllegalArgumentException.class, () -> new ChannelBufferInputStream(mock(ChannelBuffer.class), -1));
    }
    
    @Test
    void testChannelBufferInputStreamWithOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            ChannelBuffer buf = mock(ChannelBuffer.class);
            new ChannelBufferInputStream(buf, buf.capacity() + 1);
        });
    }
    
    @Test
    void testChannelBufferWriteOutAndReadIn() {
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        testChannelBufferOutputStream(buf);
        testChannelBufferInputStream(buf);
    }
    
    public void testChannelBufferOutputStream(final ChannelBuffer buf) {
        try (ChannelBufferOutputStream out = new ChannelBufferOutputStream(buf)) {
            assertSame(buf, out.buffer());
            write(out);
        } catch (IOException ioe) {
            // ignored
        }
    }
    
    private void write(final ChannelBufferOutputStream out) throws IOException {
        out.write(new byte[0]);
        out.write(new byte[]{1, 2, 3, 4});
        out.write(new byte[]{1, 3, 3, 4}, 0, 0);
    }
    
    public void testChannelBufferInputStream(final ChannelBuffer buf) {
        try (ChannelBufferInputStream in = new ChannelBufferInputStream(buf)) {
            assertTrue(in.markSupported());
            in.mark(Integer.MAX_VALUE);
            
            assertEquals(buf.writerIndex(), in.skip(Long.MAX_VALUE));
            assertFalse(buf.readable());
            
            in.reset();
            assertEquals(0, buf.readerIndex());
            assertEquals(4, in.skip(4));
            assertEquals(4, buf.readerIndex());
            in.reset();
            
            readBytes(in);
            
            assertEquals(buf.readerIndex(), in.readBytes());
        } catch (IOException ioe) {
            // ignored
        }
    }
    
    private void readBytes(ChannelBufferInputStream in) throws IOException {
        byte[] tmp = new byte[13];
        in.read(tmp);
        
        assertEquals(1, tmp[0]);
        assertEquals(2, tmp[1]);
        assertEquals(3, tmp[2]);
        assertEquals(4, tmp[3]);
        
        assertEquals(-1, in.read());
        assertEquals(-1, in.read(tmp));
    }
}
