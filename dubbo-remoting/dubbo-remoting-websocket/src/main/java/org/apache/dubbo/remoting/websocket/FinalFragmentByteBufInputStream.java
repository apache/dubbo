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
package org.apache.dubbo.remoting.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

public class FinalFragmentByteBufInputStream extends ByteBufInputStream implements FinalFragment {

    private final boolean finalFragment;

    public FinalFragmentByteBufInputStream(ByteBuf buffer) {
        this(buffer, buffer.readableBytes());
    }

    public FinalFragmentByteBufInputStream(ByteBuf buffer, int length) {
        this(buffer, length, false);
    }

    public FinalFragmentByteBufInputStream(ByteBuf buffer, boolean releaseOnClose) {
        this(buffer, buffer.readableBytes(), releaseOnClose);
    }

    public FinalFragmentByteBufInputStream(ByteBuf buffer, boolean releaseOnClose, boolean finalFragment) {
        this(buffer, buffer.readableBytes(), releaseOnClose, finalFragment);
    }

    public FinalFragmentByteBufInputStream(ByteBuf buffer, int length, boolean releaseOnClose) {
        this(buffer, length, releaseOnClose, false);
    }

    public FinalFragmentByteBufInputStream(ByteBuf buffer, int length, boolean releaseOnClose, boolean finalFragment) {
        super(buffer, length, releaseOnClose);
        this.finalFragment = finalFragment;
    }

    @Override
    public boolean isFinalFragment() {
        return finalFragment;
    }
}
