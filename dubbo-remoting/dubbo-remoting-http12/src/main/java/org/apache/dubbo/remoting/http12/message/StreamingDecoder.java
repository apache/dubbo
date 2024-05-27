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
         * @param rawMessage raw message
         */
        void onFragmentMessage(InputStream rawMessage);

        /**
         * @param rawMessage raw message
         */
        default void onFragmentMessage(InputStream dataHeader, InputStream rawMessage) {
            onFragmentMessage(rawMessage);
        }

        default void onClose() {}
    }

    class DefaultFragmentListener implements FragmentListener {

        private final ListeningDecoder listeningDecoder;

        public DefaultFragmentListener(ListeningDecoder listeningDecoder) {
            this.listeningDecoder = listeningDecoder;
        }

        @Override
        public void onFragmentMessage(InputStream rawMessage) {
            listeningDecoder.decode(rawMessage);
        }

        @Override
        public void onClose() {
            this.listeningDecoder.close();
        }
    }
}
