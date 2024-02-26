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
package org.apache.dubbo.rpc.protocol.rest.message.codec;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodec;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@Activate(onClass = "javax.ws.rs.core.Response")
public class ResteasyResponseCodec implements HttpMessageCodec<byte[], OutputStream> {

    private Class<?> responseClass;

    public ResteasyResponseCodec() {
        try {
            responseClass = ClassUtils.forName("javax.ws.rs.core.Response");
            JsonCodec.addUnSupportClass(responseClass);
        } catch (Exception exception) {
            responseClass = null;
        }
    }

    @Override
    public boolean contentTypeSupport(MediaType mediaType, Class<?> targetType) {
        return isMatch(targetType);
    }

    @Override
    public boolean typeSupport(Class<?> targetType) {
        return isMatch(targetType);
    }

    @Override
    public MediaType contentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public Object decode(byte[] body, Class<?> targetType, Type type) throws Exception {
        if (null == body || body.length == 0) {
            return null;
        }

        Class<?> builtResponse = ClassUtils.forName("org.jboss.resteasy.specimpl.BuiltResponse");

        Object o = builtResponse.newInstance();

        Method method = builtResponse.getMethod("setEntity", Object.class);

        method.invoke(o, new String(body, StandardCharsets.UTF_8));

        return o;
    }

    @Override
    public void encode(OutputStream os, Object target, URL url) throws Exception {
        if (target != null) {
            Method method = target.getClass().getMethod("getEntity");
            method.setAccessible(true);
            Object result = method.invoke(target);
            os.write(JsonUtils.toJson(result).getBytes(StandardCharsets.UTF_8));
        }
    }

    private boolean isMatch(Class<?> targetType) {
        return responseClass != null && null != targetType && responseClass.isAssignableFrom(targetType);
    }
}
