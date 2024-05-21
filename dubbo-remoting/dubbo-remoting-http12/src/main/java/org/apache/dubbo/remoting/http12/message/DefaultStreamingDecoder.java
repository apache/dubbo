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

import java.io.IOException;
import java.io.InputStream;

public class DefaultStreamingDecoder implements StreamingDecoder {

    private boolean closed;

    protected final CompositeInputStream accumulate = new CompositeInputStream();

    protected FragmentListener listener;

    @Override
    public void request(int numMessages) {
        // do nothing
    }

    @Override
    public void decode(InputStream inputStream) throws DecodeException {
        if (closed) {
            // ignored
            return;
        }
        accumulate.addInputStream(inputStream);
    }

    @Override
    public void close() {
        try {
            if (!closed) {
                closed = true;
                listener.onFragmentMessage(accumulate);
                accumulate.close();
                listener.onClose();
            }
        } catch (IOException e) {
            throw new DecodeException(e);
        }
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
}
