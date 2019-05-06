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
package org.apache.dubbo.serialize.hessian;

import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.Serializer;
import com.caucho.hessian.io.SerializerFactory;

public class Hessian2SerializerFactory extends SerializerFactory {
    public static final SerializerFactory INSTANCE = new Hessian2SerializerFactory();

    private Hessian2SerializerFactory() {
        super();
    }

    @Override
    protected Serializer loadSerializer(Class<?> cl) throws HessianProtocolException {
        Serializer serializer = Java8SerializerFactory.INSTANCE.getSerializer(cl);
        return serializer != null ? serializer : super.loadSerializer(cl);
    }

    @Override
    protected Deserializer loadDeserializer(Class cl) throws HessianProtocolException {
        Deserializer deserializer = Java8SerializerFactory.INSTANCE.getDeserializer(cl);
        return deserializer != null ? deserializer : super.loadDeserializer(cl);
    }
}
