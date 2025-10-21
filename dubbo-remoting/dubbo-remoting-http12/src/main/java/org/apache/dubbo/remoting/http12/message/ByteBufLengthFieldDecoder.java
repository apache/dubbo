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
package org.apache.dubbo.remoting.http12.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

public class ByteBufLengthFieldDecoder extends AbstractLengthFieldStreamingDecoder<ByteBuf, CompositeByteBuf> {

    public ByteBufLengthFieldDecoder() {
        this(0, 4);
    }

    public ByteBufLengthFieldDecoder(int lengthFieldOffset, int lengthFieldLength) {
        super(Unpooled.compositeBuffer(), lengthFieldOffset, lengthFieldLength);
    }

    @Override
    protected void addInputToAccumulate(ByteBuf input) {
        accumulate.addComponent(true, input);
    }

    @Override
    protected void releaseAccumulate() {
        accumulate.clear();
        accumulate.release();
    }

    @Override
    protected boolean hasEnoughBytes() {
        return requiredLength <= accumulate.readableBytes();
    }

    @Override
    protected void processHeader() {
        accumulate.skipBytes(lengthFieldOffset);
        requiredLength = readLengthField(accumulate, lengthFieldLength);
        state = DecodeState.PAYLOAD;
    }

    @Override
    protected void processBody() {
        ByteBuf rawMessage = accumulate.readSlice(requiredLength);
        invokeListener(rawMessage);
        state = DecodeState.HEADER;
        requiredLength = fixedHeaderLength;
        accumulate.discardReadComponents();
    }

    @Override
    public void invokeListener(ByteBuf rawMessage) {
        listener.onFragmentMessage(rawMessage);
    }

    protected int readLengthField(ByteBuf buffer, int length) {
        int result = 0;
        for (int i = 0; i < length; i++) {
            result = (result << 8) | (buffer.readByte() & 0xFF);
        }
        return result;
    }
}
