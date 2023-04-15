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
package org.apache.dubbo.common.serialize.hessian2;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.DataInput;
import org.apache.dubbo.common.serialize.DataOutput;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ArgumentsSources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class TypeMatchTest {
    static class DataProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
            List<Object> datas = new LinkedList<>();
            List<Method> readMethods = new LinkedList<>();
            List<Method> writeMethods = new LinkedList<>();

            datas.add(true);
            datas.add(false);
            datas.add((byte) 123);
            datas.add((byte) 234);
            datas.add((short) 12345);
            datas.add((short) 23456);
            datas.add(123456);
            datas.add(234567);
            datas.add(1234567L);
            datas.add(2345678L);
            datas.add(0.123F);
            datas.add(1.234F);
            datas.add(0.1234D);
            datas.add(1.2345D);
            datas.add("hello");
            datas.add("world");
            datas.add("hello".getBytes());
            datas.add("world".getBytes());

            for (Method method : ObjectInput.class.getMethods()) {
                if (method.getName().startsWith("read") && method.getParameterTypes().length == 0 && !method.getReturnType().equals(Object.class)) {
                    readMethods.add(method);
                }
            }
            for (Method method : DataInput.class.getMethods()) {
                if (method.getName().startsWith("read") && method.getParameterTypes().length == 0 && !method.getReturnType().equals(Object.class)) {
                    readMethods.add(method);
                }
            }

            for (Method method : ObjectOutput.class.getMethods()) {
                if (method.getName().startsWith("write") && method.getParameterTypes().length == 1 && !method.getParameterTypes()[0].equals(Object.class)) {
                    writeMethods.add(method);
                }
            }

            for (Method method : DataOutput.class.getMethods()) {
                if (method.getName().startsWith("write") && method.getParameterTypes().length == 1 && !method.getParameterTypes()[0].equals(Object.class)) {
                    writeMethods.add(method);
                }
            }

            Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new HashMap<>(16);
            primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
            primitiveWrapperTypeMap.put(Byte.class, byte.class);
            primitiveWrapperTypeMap.put(Character.class, char.class);
            primitiveWrapperTypeMap.put(Double.class, double.class);
            primitiveWrapperTypeMap.put(Float.class, float.class);
            primitiveWrapperTypeMap.put(Integer.class, int.class);
            primitiveWrapperTypeMap.put(Long.class, long.class);
            primitiveWrapperTypeMap.put(Short.class, short.class);
            primitiveWrapperTypeMap.put(Void.class, void.class);

            List<Arguments> argumentsList = new LinkedList<>();
            for (Object data : datas) {
                for (Method input : readMethods) {
                    for (Method output : writeMethods) {
                        if (output.getParameterTypes()[0].isAssignableFrom(data.getClass())) {
                            argumentsList.add(Arguments.arguments(data, input, output));
                        }
                        if (primitiveWrapperTypeMap.containsKey(data.getClass()) &&
                            output.getParameterTypes()[0].isAssignableFrom(primitiveWrapperTypeMap.get(data.getClass()))) {
                            argumentsList.add(Arguments.arguments(data, input, output));
                        }
                    }
                }
            }

            return argumentsList.stream();
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DataProvider.class)
    void test(Object data, Method input, Method output) throws Exception {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization = frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization.serialize(url, outputStream);
        output.invoke(objectOutput, data);
        objectOutput.flushBuffer();

        byte[] bytes = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInput objectInput = serialization.deserialize(url, inputStream);
        if (output.getParameterTypes()[0].equals(input.getReturnType())) {
            Object result = input.invoke(objectInput);
            if (data.getClass().isArray()) {
                Assertions.assertArrayEquals((byte[]) data, (byte[]) result);
            } else {
                Assertions.assertEquals(data, result);
            }
        } else {
            try {
                Object result = input.invoke(objectInput);
                if (data.getClass().isArray()) {
                    Assertions.assertNotEquals(data.getClass(), result.getClass());
                } else {
                    Assertions.assertNotEquals(data, result);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        frameworkModel.destroy();
    }
}
