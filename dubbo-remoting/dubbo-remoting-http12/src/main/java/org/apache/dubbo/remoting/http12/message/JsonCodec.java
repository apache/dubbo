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

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * body is json
 */
@Activate("json")
public class JsonCodec implements HttpMessageCodec {

    @Override
    public MediaType contentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public void encode(OutputStream body, Object unSerializedBody) throws IOException {
        try {
            String jsonString = JsonUtils.toJson(unSerializedBody);
            body.write(jsonString.getBytes(StandardCharsets.UTF_8));
        } finally {
            body.flush();
        }
    }

    @Override
    public Object decode(InputStream body, Class<?> targetType) throws IOException {
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
    }

}
