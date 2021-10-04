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
package org.apache.dubbo.common.serialize.hessian2.dubbo;

import com.alibaba.com.caucho.hessian.io.SerializerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractHessian2FactoryInitializer implements Hessian2FactoryInitializer {
    private static final Map<ClassLoader, SerializerFactory> CL_2_SERIALIZER_FACTORY = new ConcurrentHashMap<>();
    private static volatile SerializerFactory SYSTEM_SERIALIZER_FACTORY;

    @Override
    public SerializerFactory getSerializerFactory() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            // system classloader
            if (SYSTEM_SERIALIZER_FACTORY == null) {
                synchronized (AbstractHessian2FactoryInitializer.class) {
                    if (SYSTEM_SERIALIZER_FACTORY == null) {
                        SYSTEM_SERIALIZER_FACTORY = createSerializerFactory();
                    }
                }
            }
            return SYSTEM_SERIALIZER_FACTORY;
        }

        if (!CL_2_SERIALIZER_FACTORY.containsKey(classLoader)) {
            synchronized (AbstractHessian2FactoryInitializer.class) {
                if (!CL_2_SERIALIZER_FACTORY.containsKey(classLoader)) {
                    SerializerFactory serializerFactory = createSerializerFactory();
                    CL_2_SERIALIZER_FACTORY.put(classLoader, serializerFactory);
                    return serializerFactory;
                }
            }
        }
        return CL_2_SERIALIZER_FACTORY.get(classLoader);
    }

    protected abstract SerializerFactory createSerializerFactory();
}
