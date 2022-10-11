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

import io.netty.buffer.ByteBufAllocator;

/**
 * The default allocator for {@link ChannelWritableBuffer}s used by the Netty transport. We set a
 * minimum bound to avoid unnecessary re-allocation for small follow-on writes and to facilitate
 * Netty's caching of buffer objects for small writes. We set an upper-bound to avoid allocations
 * outside of the arena-pool which are orders of magnitude slower. The Netty transport can receive
 * buffers of arbitrary size and will chunk them based on flow-control so there is no transport
 * requirement for an upper bound.
 *
 * <p>Note: It is assumed that most applications will be using Netty's direct buffer pools for
 * maximum performance.
 */
public class ChannelWritableBufferAllocator implements WritableBufferAllocator {

    // Use 4k as our minimum buffer size.
    private static final int MIN_BUFFER = 4 * 1024;

    // Set the maximum buffer size to 1MB.
    private static final int MAX_BUFFER = 1024 * 1024;

    private final ByteBufAllocator allocator;

    public ChannelWritableBufferAllocator(ByteBufAllocator allocator) {
        this.allocator = allocator;
    }

    @Override
    public WritableBuffer allocate(int capacityHint) {
        capacityHint = Math.min(MAX_BUFFER, Math.max(MIN_BUFFER, capacityHint));
        return new ChannelWritableBuffer(allocator.buffer(capacityHint, capacityHint));
    }
}
