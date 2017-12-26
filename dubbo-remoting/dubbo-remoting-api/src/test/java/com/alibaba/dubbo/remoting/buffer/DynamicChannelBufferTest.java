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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class DynamicChannelBufferTest extends AbstractChannelBufferTest {

    private ChannelBuffer buffer;

    @Override
    protected ChannelBuffer newBuffer(int length) {
        buffer = ChannelBuffers.dynamicBuffer(length);

        assertEquals(0, buffer.readerIndex());
        assertEquals(0, buffer.writerIndex());
        assertEquals(length, buffer.capacity());

        return buffer;
    }

    @Override
    protected ChannelBuffer[] components() {
        return new ChannelBuffer[]{buffer};
    }

    @Test
    public void shouldNotFailOnInitialIndexUpdate() {
        new DynamicChannelBuffer(10).setIndex(0, 10);
    }

    @Test
    public void shouldNotFailOnInitialIndexUpdate2() {
        new DynamicChannelBuffer(10).writerIndex(10);
    }

    @Test
    public void shouldNotFailOnInitialIndexUpdate3() {
        ChannelBuffer buf = new DynamicChannelBuffer(10);
        buf.writerIndex(10);
        buf.readerIndex(10);
    }
}

