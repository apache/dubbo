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

import org.apache.dubbo.remoting.http12.CompositeInputStream;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FinalFragmentStreamingDecoder implements StreamingDecoder {

    private boolean inDelivery;

    private boolean pendingDelivery;

    private boolean closed;

    private boolean closing;

    protected final CompositeInputStream accumulate = new CompositeInputStream();

    protected FragmentListener listener;

    @Override
    public void request(int numMessages) {}

    @Override
    public void decode(InputStream inputStream) throws DecodeException {
        if (closing || closed) {
            // ignored
            return;
        }
        accumulate.addInputStream(inputStream);
        if (inputStream instanceof FinalFragment && ((FinalFragment) inputStream).isFinalFragment()) {
            pendingDelivery = true;
            deliver();
        }
    }

    @Override
    public void close() {
        closing = true;
        deliver();
    }

    @Override
    public void onStreamClosed() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            accumulate.close();
        } catch (IOException e) {
            throw new DecodeException(e);
        }
    }

    @Override
    public void setFragmentListener(FragmentListener listener) {
        this.listener = listener;
    }

    private void deliver() {
        if (inDelivery) {
            return;
        }
        if (closed) {
            return;
        }
        inDelivery = true;
        try {
            if (pendingDelivery) {
                processBody();
                pendingDelivery = false;
            }
            if (closing) {
                if (!closed) {
                    closed = true;
                    accumulate.close();
                    listener.onClose();
                }
            }
        } catch (IOException e) {
            throw new DecodeException(e);
        } finally {
            inDelivery = false;
        }
    }

    private void processBody() throws IOException {
        byte[] rawMessage = readRawMessage(accumulate, accumulate.available());
        InputStream inputStream = new ByteArrayInputStream(rawMessage);
        invokeListener(inputStream);
    }

    protected void invokeListener(InputStream inputStream) {
        this.listener.onFragmentMessage(inputStream);
    }

    protected byte[] readRawMessage(InputStream inputStream, int length) throws IOException {
        byte[] data = new byte[length];
        inputStream.read(data, 0, length);
        return data;
    }
}
