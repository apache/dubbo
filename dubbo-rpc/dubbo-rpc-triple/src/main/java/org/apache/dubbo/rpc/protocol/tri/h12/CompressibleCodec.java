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
package org.apache.dubbo.rpc.protocol.tri.h12;

import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;

import java.io.InputStream;
import java.io.OutputStream;

public class CompressibleCodec implements HttpMessageCodec {

    private final HttpMessageCodec delegate;

    private Compressor compressor = Compressor.NONE;

    public CompressibleCodec(HttpMessageCodec delegate) {
        this.delegate = delegate;
    }

    public void setCompressor(Compressor compressor) {
        this.compressor = compressor;
    }

    @Override
    public void encode(OutputStream outputStream, Object data) throws EncodeException {
        delegate.encode(compressor.decorate(outputStream), data);
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType) throws DecodeException {
        return delegate.decode(inputStream, targetType);
    }

    @Override
    public void encode(OutputStream outputStream, Object[] data) throws EncodeException {
        delegate.encode(outputStream, data);
    }

    @Override
    public Object[] decode(InputStream inputStream, Class<?>[] targetTypes) throws DecodeException {
        return delegate.decode(inputStream, targetTypes);
    }

    @Override
    public MediaType mediaType() {
        return delegate.mediaType();
    }
}
