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

import org.apache.dubbo.remoting.http12.exception.DecodeException;

import java.io.InputStream;

public interface StreamingDecoder {

    void request(int numMessages);

    void decode(InputStream inputStream) throws DecodeException;

    void close();

    void onStreamClosed();

    void setFragmentListener(FragmentListener listener);

    interface FragmentListener {

        /**
         * Called when the given number of bytes has been read from the input source of the deframer.
         * This is typically used to indicate to the underlying transport that more data can be
         * accepted.
         */
        void bytesRead(int numBytes);

        /**
         * Called when a complete message fragment is received.
         *
         * @param rawMessage raw message as InputStream
         * @param messageLength the length of the message payload in bytes
         */
        void onFragmentMessage(InputStream rawMessage, int messageLength);

        default void onClose() {}
    }

    final class NoopFragmentListener implements FragmentListener {

        static final FragmentListener NOOP = new NoopFragmentListener();

        private NoopFragmentListener() {}

        @Override
        public void bytesRead(int numBytes) {}

        @Override
        public void onFragmentMessage(InputStream rawMessage, int messageLength) {}
    }
}
