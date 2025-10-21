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

import org.apache.dubbo.remoting.http12.CompositeInputStream;
import org.apache.dubbo.remoting.http12.exception.DecodeException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamLengthFieldDecoder
        extends AbstractLengthFieldStreamingDecoder<InputStream, CompositeInputStream> {

    public InputStreamLengthFieldDecoder() {
        this(0, 4);
    }

    public InputStreamLengthFieldDecoder(int lengthFieldOffset, int lengthFieldLength) {
        super(new CompositeInputStream(), lengthFieldOffset, lengthFieldLength);
    }

    @Override
    protected void addInputToAccumulate(InputStream input) {
        accumulate.addInputStream(input);
    }

    @Override
    protected void releaseAccumulate() {
        try {
            accumulate.close();
        } catch (IOException e) {
            throw new DecodeException(e);
        }
    }

    @Override
    protected boolean hasEnoughBytes() {
        return requiredLength <= accumulate.available();
    }

    @Override
    protected void processHeader() throws IOException {
        byte[] offsetData = new byte[lengthFieldOffset];
        accumulate.read(offsetData);
        requiredLength = readLengthField(accumulate, lengthFieldLength);
        state = DecodeState.PAYLOAD;
    }

    @Override
    protected void processBody() throws IOException {
        byte[] rawMessage = new byte[requiredLength];
        accumulate.read(rawMessage);
        invokeListener(new ByteArrayInputStream(rawMessage));
        state = DecodeState.HEADER;
        requiredLength = fixedHeaderLength;
    }

    @Override
    public void invokeListener(InputStream rawMessage) {
        listener.onFragmentMessage(rawMessage);
    }

    protected int readLengthField(InputStream input, int length) throws IOException {
        int result = 0;
        for (int i = 0; i < length; i++) {
            result = (result << 8) | (input.read() & 0xFF);
        }
        return result;
    }
}
