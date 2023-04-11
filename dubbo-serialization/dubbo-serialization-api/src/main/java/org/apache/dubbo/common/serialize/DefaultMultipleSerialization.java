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

package org.apache.dubbo.common.serialize;

import org.apache.dubbo.common.URL;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DefaultMultipleSerialization implements MultipleSerialization {

    @Override
    public void serialize(URL url, String serializeType, Class<?> clz, Object obj, OutputStream os) throws IOException {
        serializeType = convertHessian(serializeType);
        final Serialization serialization = url.getOrDefaultFrameworkModel().getExtensionLoader(Serialization.class).getExtension(serializeType);
        final ObjectOutput serialize = serialization.serialize(null, os);
        serialize.writeObject(obj);
        serialize.flushBuffer();
    }

    @Override
    public Object deserialize(URL url, String serializeType, Class<?> clz, InputStream os) throws IOException, ClassNotFoundException {
        serializeType = convertHessian(serializeType);
        final Serialization serialization = url.getOrDefaultFrameworkModel().getExtensionLoader(Serialization.class).getExtension(serializeType);
        final ObjectInput in = serialization.deserialize(null, os);
        return in.readObject(clz);
    }

    private String convertHessian(String ser) {
        if (ser.equals("hessian4")) {
            return "hessian2";
        }
        return ser;
    }
}
