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
import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class UrlEncodeFormCodec implements HttpMessageCodec {

    private final ConverterUtil converterUtil;

    public UrlEncodeFormCodec(ConverterUtil converterUtil) {
        this.converterUtil = converterUtil;
    }

    @Override
    public void encode(OutputStream outputStream, Object data, Charset charset) throws EncodeException {
        try {
            if (data instanceof String) {
                outputStream.write(((String) data).getBytes());
            } else if (data instanceof Map) {
                StringBuilder toWrite = new StringBuilder();
                for (Map.Entry<?, ?> e : ((Map<?, ?>) data).entrySet()) {
                    String k = e.getKey().toString();
                    String v = e.getValue().toString();
                    toWrite.append(k)
                            .append("=")
                            .append(URLEncoder.encode(v, StandardCharsets.UTF_8.name()))
                            .append("&");
                }
                if (toWrite.length() > 1) {
                    outputStream.write(
                            toWrite.substring(0, toWrite.length() - 1).getBytes(charset));
                }
            } else {
                throw new EncodeException("UrlEncodeFrom media-type only supports String or Map as return type.");
            }
        } catch (HttpStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new EncodeException(e);
        }
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType, Charset charset) throws DecodeException {
        Object[] res = decode(inputStream, new Class[] {targetType}, charset);
        return res.length > 1 ? res : res[0];
    }

    @Override
    public Object[] decode(InputStream inputStream, Class<?>[] targetTypes, Charset charset) throws DecodeException {
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
            String decoded = URLDecoder.decode(
                            StreamUtils.toString(inputStream, charset), StandardCharsets.UTF_8.name())
                    .trim();
            Map<String, Object> res = toMap(decoded, targetTypes, toMap);
            if (toMap) {
                return new Object[] {res};
            } else {
                return res.values().toArray();
            }
        } catch (HttpStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new DecodeException(e);
        }
    }

    private Map<String, Object> toMap(String formString, Class<?>[] targetTypes, boolean toMap) {
        Map<String, Object> res = new HashMap<>(1);
        // key1=val1&key2=&key3=&key4=val4
        String[] parts = formString.split("&");
        for (int i = 0; i < parts.length; i++) {
            String pair = parts[i];
            int index = pair.indexOf("=");
            if (index < 1) {
                throw new DecodeException("Broken request:" + formString);
            }
            String key = pair.substring(0, index);
            String val = (index == pair.length() - 1) ? "" : pair.substring(index + 1);
            res.put(
                    key,
                    toMap || targetTypes[i].equals(String.class)
                            // method params are Map or String, use plain text as value
                            ? val
                            // try convert to target types
                            : converterUtil.convertIfPossible(val, targetTypes[i]));
        }
        return res;
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_FROM_URLENCODED;
    }
}
