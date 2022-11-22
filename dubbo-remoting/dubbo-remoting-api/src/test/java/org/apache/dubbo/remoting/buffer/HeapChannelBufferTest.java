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

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

class HeapChannelBufferTest extends AbstractChannelBufferTest {

    private ChannelBuffer buffer;

    @Override
    protected ChannelBuffer newBuffer(int capacity) {
        buffer = ChannelBuffers.buffer(capacity);
        Assertions.assertEquals(0, buffer.writerIndex());
        return buffer;
    }

    @Override
    protected ChannelBuffer[] components() {
        return new ChannelBuffer[]{buffer};
    }

    @Test
    void testEqualsAndHashcode() {
        HeapChannelBuffer b1 = new HeapChannelBuffer("hello-world".getBytes());
        HeapChannelBuffer b2 = new HeapChannelBuffer("hello-world".getBytes());

        MatcherAssert.assertThat(b1.equals(b2), is(true));
        MatcherAssert.assertThat(b1.hashCode(), is(b2.hashCode()));

        b1 = new HeapChannelBuffer("hello-world".getBytes());
        b2 = new HeapChannelBuffer("hello-worldd".getBytes());

        MatcherAssert.assertThat(b1.equals(b2), is(false));
        MatcherAssert.assertThat(b1.hashCode(), not(b2.hashCode()));
    }
}
