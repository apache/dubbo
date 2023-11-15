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

import java.io.InputStream;

public class DefaultListeningDecoder implements ListeningDecoder {

    private final HttpMessageCodec httpMessageCodec;

    private final Class<?>[] targetTypes;

    private Listener listener;

    public DefaultListeningDecoder(HttpMessageCodec httpMessageCodec, Class<?>[] targetTypes) {
        this.httpMessageCodec = httpMessageCodec;
        this.targetTypes = targetTypes;
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void decode(InputStream inputStream) {
        Object[] decode = this.httpMessageCodec.decode(inputStream, targetTypes);
        this.listener.onMessage(decode);
    }

    @Override
    public void close() {
        this.listener.onClose();
    }
}
