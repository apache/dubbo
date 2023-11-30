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

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson2.JSONObject;

/**
 * body is json
 */
@Activate
public class JsonCodec implements HttpMessageCodec {

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public void encode(OutputStream outputStream, Object unSerializedBody) throws EncodeException {
        try {
            try {
                String jsonString = JsonUtils.toJson(unSerializedBody);
                outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
            } finally {
                outputStream.flush();
            }
        } catch (Throwable e) {
            throw new EncodeException(e);
        }
    }

    @Override
    public void encode(OutputStream outputStream, Object[] data) throws EncodeException {
        try {
            try {
                String jsonString = JsonUtils.toJson(data);
                outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
            } finally {
                outputStream.flush();
            }
        } catch (Throwable e) {
            throw new EncodeException(e);
        }
    }

    @Override
    public Object decode(InputStream body, Class<?> targetType) throws DecodeException {
        try {
            try {
                int len;
                byte[] data = new byte[4096];
                StringBuilder builder = new StringBuilder(4096);
                while ((len = body.read(data)) != -1) {
                    builder.append(new String(data, 0, len));
                }
                return JsonUtils.toJavaObject(builder.toString(), targetType);
            } finally {
                body.close();
            }
        } catch (Throwable e) {
            throw new DecodeException(e);
        }
    }

    @Override
    public Object[] decode(InputStream dataInputStream, Class<?>[] targetTypes) throws DecodeException {
        List<Object> result = new ArrayList<>();
        try {
            try {
                int len;
                byte[] data = new byte[4096];
                StringBuilder builder = new StringBuilder(4096);
                while ((len = dataInputStream.read(data)) != -1) {
                    builder.append(new String(data, 0, len));
                }
                String jsonString = builder.toString();
                List<Object> jsonObjects = JsonUtils.toJavaList(jsonString, Object.class);

                for (int i = 0; i < targetTypes.length; i++) {
                    Object jsonObject = jsonObjects.get(i);
                    Class<?> type = targetTypes[i];
                    if (jsonObject instanceof JSONObject) {
                        Object o = ((JSONObject) jsonObject).toJavaObject(type);
                        result.add(o);
                    } else {
                        result.add(jsonObject);
                    }
                }
                return result.toArray();
            } finally {
                dataInputStream.close();
            }
        } catch (Throwable e) {
            throw new DecodeException(e);
        }
    }
}
