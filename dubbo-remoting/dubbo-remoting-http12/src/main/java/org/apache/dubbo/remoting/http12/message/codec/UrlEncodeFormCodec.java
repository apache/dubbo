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
package org.apache.dubbo.remoting.http12.message.codec;

import org.apache.dubbo.common.convert.ConverterUtil;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Activate
public class UrlEncodeFormCodec implements HttpMessageCodec {

    private final ConverterUtil converterUtil;

    public UrlEncodeFormCodec(ConverterUtil converterUtil) {
        this.converterUtil = converterUtil;
    }

    @Override
    public void encode(OutputStream outputStream, Object data) throws EncodeException {
        throw new EncodeException("UrlEncodeFormCodec does not support encode.");
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType) throws DecodeException {
        Object[] res = decode(inputStream, new Class[] {targetType});
        return res.length > 1 ? res : res[0];
    }

    @Override
    public Object[] decode(InputStream inputStream, Class<?>[] targetTypes) throws DecodeException {
        try {
            boolean toMap;
            // key=value&key2=value2 -> method(map<keys,values>)
            if (targetTypes.length == 1 && targetTypes[0].isAssignableFrom(HashMap.class)) {
                toMap = true;
            }
            // key=value&key2=value2 -> method(value,value2)
            else if (Arrays.stream(targetTypes)
                    .allMatch(clz -> String.class.isAssignableFrom(clz) || Number.class.isAssignableFrom(clz))) {
                toMap = false;
            } else {
                throw new DecodeException(
                        "For x-www-form-urlencoded MIME type, please use Map/String/base-types as method param.");
            }
            String decoded = URLDecoder.decode(toByteArrayStream(inputStream).toString(), StandardCharsets.UTF_8.name())
                    .trim();
            Map<String, Object> res = toMap(decoded, targetTypes, toMap);
            if (toMap) {
                return new Object[] {res};
            } else {
                return res.values().toArray();
            }
        } catch (Exception e) {
            throw new DecodeException(e);
        }
    }

    private Map<String, Object> toMap(String formString, Class<?>[] targetTypes, boolean toMap) {
        Map<String, Object> res = new HashMap<>(1);
        String[] parts = formString.split("[=&]");
        if (parts.length % 2 != 0 && parts.length / 2 == targetTypes.length) {
            throw new DecodeException("Broken request:" + formString);
        }
        for (int i = 0; i < parts.length - 1; i += 2) {
            res.put(
                    parts[i],
                    // If method param is Map or String...
                    toMap || targetTypes[i / 2].equals(String.class)
                            ? parts[i + 1]
                            : converterUtil.convertIfPossible(parts[i + 1], targetTypes[i / 2]));
        }
        return res;
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_X_WWW_FROM_URLENCODED;
    }
}
