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

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * for http body codec
 */
@SPI(scope = ExtensionScope.FRAMEWORK)
public interface HttpMessageCodec {

    void encode(OutputStream outputStream, Object data) throws EncodeException;

    default void encode(OutputStream outputStream, Object[] data) throws EncodeException {
        // default encode first data
        this.encode(outputStream, data == null || data.length == 0 ? null : data[0]);
    }

    Object decode(InputStream inputStream, Class<?> targetType) throws DecodeException;

    default Object[] decode(InputStream inputStream, Class<?>[] targetTypes) throws DecodeException {
        // default decode first target type
        return new Object[] {
            this.decode(inputStream, targetTypes == null || targetTypes.length == 0 ? null : targetTypes[0])
        };
    }

    MediaType mediaType();
}
