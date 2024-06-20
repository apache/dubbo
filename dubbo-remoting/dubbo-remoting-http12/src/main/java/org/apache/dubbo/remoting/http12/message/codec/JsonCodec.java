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

import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;

public class JsonCodec implements HttpMessageCodec {

    public static final JsonCodec INSTANCE = new JsonCodec();

    public void encode(OutputStream os, Object data, Charset charset) throws EncodeException {
        try {
            os.write(JsonUtils.toJson(data).getBytes(charset));
        } catch (HttpStatusException e) {
            throw e;
        } catch (Throwable t) {
            throw new EncodeException("Error encoding json", t);
        }
    }

    public void encode(OutputStream os, Object[] data, Charset charset) throws EncodeException {
        try {
            os.write(JsonUtils.toJson(data).getBytes(charset));
        } catch (HttpStatusException e) {
            throw e;
        } catch (Throwable t) {
            throw new EncodeException("Error encoding json", t);
        }
    }

    @Override
    public Object decode(InputStream is, Class<?> targetType, Charset charset) throws DecodeException {
        try {
            return JsonUtils.toJavaObject(StreamUtils.toString(is, charset), targetType);
        } catch (HttpStatusException e) {
            throw e;
        } catch (Throwable t) {
            throw new DecodeException("Error decoding json", t);
        }
    }

    @Override
    public Object[] decode(InputStream is, Class<?>[] targetTypes, Charset charset) throws DecodeException {
        try {
            int len = targetTypes.length;
            if (len == 0) {
                return new Object[0];
            }
            Object obj = JsonUtils.toJavaObject(StreamUtils.toString(is, charset), Object.class);
            if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                if (list.size() == len) {
                    Object[] results = new Object[len];
                    for (int i = 0; i < len; i++) {
                        results[i] = JsonUtils.convertObject(list.get(i), targetTypes[i]);
                    }
                    return results;
                }
                throw new DecodeException(
                        "Json array size [" + list.size() + "] must equals arguments count [" + len + "]");
            }
            if (len == 1) {
                return new Object[] {JsonUtils.convertObject(obj, targetTypes[0])};
            }
            throw new DecodeException("Json must be array");
        } catch (HttpStatusException e) {
            throw e;
        } catch (Throwable t) {
            throw new DecodeException("Error decoding json", t);
        }
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_JSON;
    }
}
