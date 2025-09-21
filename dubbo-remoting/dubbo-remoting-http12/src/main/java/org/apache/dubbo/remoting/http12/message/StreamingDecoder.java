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

public interface StreamingDecoder<T> {

    void request(int numMessages);

    void decode(T inputStream) throws DecodeException;

    void close();

    void onStreamClosed();

    void setFragmentListener(FragmentListener<T> listener);

    interface FragmentListener<T> {

        /**
         * @param rawMessage raw message
         */
        void onFragmentMessage(T rawMessage);

        default void onClose() {}
    }

    final class DefaultFragmentListener<T> implements FragmentListener<T> {

        private final ListeningDecoder<T> listeningDecoder;

        public DefaultFragmentListener(ListeningDecoder<T> listeningDecoder) {
            this.listeningDecoder = listeningDecoder;
        }

        @Override
        public void onFragmentMessage(T rawMessage) {
            listeningDecoder.decode(rawMessage);
        }

        @Override
        public void onClose() {
            listeningDecoder.close();
        }
    }

    @SuppressWarnings("unchecked")
    static <T> FragmentListener<T> noop() {
        return (FragmentListener<T>) NoopFragmentListener.NOOP;
    }

    final class NoopFragmentListener<T> implements FragmentListener<T> {

        private static final NoopFragmentListener<?> NOOP = new NoopFragmentListener<>();

        private NoopFragmentListener() {}

        @Override
        public void onFragmentMessage(T rawMessage) {
            // no-op
        }
    }
}
