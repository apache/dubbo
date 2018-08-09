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
package org.apache.dubbo.common.serialize.kryo;

import com.esotericsoftware.kryo.Serializer;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.kryo.serializer.CommonJavaSerializer;
import org.apache.dubbo.common.serialize.kryo.utils.ReflectionUtils;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

public class ExtensionKryo extends CompatibleKryo {

    private static ReflectionFactory reflectionFactory = ReflectionFactory
            .getReflectionFactory();

    private static final Logger logger = LoggerFactory.getLogger(ExtensionKryo.class);

    private static ConcurrentHashMap<Class<?>, Constructor<?>> constructorCache = new ConcurrentHashMap<>();

    @Override
    public Serializer getDefaultSerializer(Class type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null.");
        }

        // 对于这些类型的  ：array 、enum、没有默认构造器的类
        if (!type.isArray() && !type.isEnum() && !ReflectionUtils.checkZeroArgConstructor(type)) {
            return new CommonJavaSerializer();
        }
        return super.getDefaultSerializer(type);
    }

    @Override
    public <T> T newInstance(Class<T> type) {
        try {
            return super.newInstance(type);
        } catch (Exception e) {
            return newInstanceByReflection(type);
        }
    }

    @SuppressWarnings("all")
    private <T> T newInstanceByReflection(Class<T> type) {
        Object instance = null;
        try {
            Constructor<?> constructor = constructorCache.get(type);
            if (constructor == null) {
                constructor = reflectionFactory.newConstructorForSerialization(type,
                        Object.class.getDeclaredConstructor());
                constructorCache.putIfAbsent(type, constructor);
                // in order to improve reflection performance
                constructor.setAccessible(true);
            }
            instance = constructor.newInstance();
            return (T) instance;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            logger.error(type.getName() + "unable to be serialized", e);
            e.printStackTrace();
        }
        return (T) instance;
    }
}
