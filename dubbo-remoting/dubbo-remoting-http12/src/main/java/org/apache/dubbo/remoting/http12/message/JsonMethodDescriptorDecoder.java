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

import com.alibaba.fastjson2.JSONObject;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.rpc.model.MethodDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class JsonMethodDescriptorDecoder implements MethodDescriptorDecoder {

    @Override
    public Object[] decode(InputStream dataInputStream, MethodDescriptor md) throws IOException {
        List<Object> result = new ArrayList<>();
        try {
            int len;
            byte[] data = new byte[4096];
            StringBuilder builder = new StringBuilder(4096);
            while ((len = dataInputStream.read(data)) != -1) {
                builder.append(new String(data, 0, len));
            }
            String jsonString = builder.toString();
            List<Object> jsonObjects = JsonUtils.toJavaList(jsonString, Object.class);
            Class<?>[] parameterClasses = md.getParameterClasses();

            for (int i = 0; i < parameterClasses.length; i++) {
                Object jsonObject = jsonObjects.get(i);
                Class<?> type = parameterClasses[i];
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
    }
}
