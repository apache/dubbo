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

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.remoting.http12.exception.DecodeException;

import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface HttpMessageDecoder extends CodecMediaType {

    Object decode(InputStream inputStream, Class<?> targetType, Charset charset) throws DecodeException;

    default Object decode(InputStream inputStream, Type targetType, Charset charset) throws DecodeException {
        if (targetType instanceof Class) {
            return decode(inputStream, (Class<?>) targetType, charset);
        }
        if (targetType instanceof ParameterizedType) {
            return decode(inputStream, (Class<?>) ((ParameterizedType) targetType).getRawType(), charset);
        }
        throw new DecodeException("targetType " + targetType + " is not a class");
    }

    default Object[] decode(InputStream inputStream, Class<?>[] targetTypes, Charset charset) throws DecodeException {
        return new Object[] {decode(inputStream, ArrayUtils.isEmpty(targetTypes) ? null : targetTypes[0], charset)};
    }

    default Object decode(InputStream inputStream, Class<?> targetType) throws DecodeException {
        return decode(inputStream, targetType, UTF_8);
    }

    default Object decode(InputStream inputStream, Type targetType) throws DecodeException {
        return decode(inputStream, targetType, UTF_8);
    }

    default Object[] decode(InputStream inputStream, Class<?>[] targetTypes) throws DecodeException {
        return decode(inputStream, targetTypes, UTF_8);
    }
}
