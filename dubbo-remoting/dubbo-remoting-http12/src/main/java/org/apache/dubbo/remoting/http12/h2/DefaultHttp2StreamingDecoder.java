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
package org.apache.dubbo.remoting.http12.h2;

import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.message.DefaultListeningDecoder;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;

import java.io.InputStream;

public class DefaultHttp2StreamingDecoder implements StreamingDecoder {

    private final DefaultListeningDecoder delegate;

    public DefaultHttp2StreamingDecoder(HttpMessageCodec httpMessageCodec, Class<?>[] targetTypes) {
        this.delegate = new DefaultListeningDecoder(httpMessageCodec, targetTypes);
    }

    @Override
    public void decode(InputStream inputStream) throws DecodeException {
        delegate.decode(inputStream);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void setListener(Listener listener) {
        delegate.setListener(listener);
    }

    @Override
    public void request(int numMessages) {
        //no op
    }
}
