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
package org.apache.dubbo.common.serialize.fastjson2;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FastJson2StripedLockTest {

    private Serialization createSerialization() {
        FrameworkModel frameworkModel = new FrameworkModel();
        return frameworkModel
                .getExtensionLoader(Serialization.class)
                .getExtension("fastjson2");
    }

    private URL createURL() {
        FrameworkModel frameworkModel = new FrameworkModel();
        return URL.valueOf("").setScopeModel(frameworkModel);
    }

    @Test
    void testReadObjectWithType() throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        TrustedPojo pojo = new TrustedPojo(42.0);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization.serialize(url, outputStream);
        objectOutput.writeObject(pojo);
        objectOutput.flushBuffer();

        byte[] bytes = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInput objectInput = serialization.deserialize(url, inputStream);

        TrustedPojo result = objectInput.readObject(TrustedPojo.class, (Type) TrustedPojo.class);
        Assertions.assertEquals(pojo, result);

        frameworkModel.destroy();
    }

    @Test
    void testReadObjectWithTypeList() throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        List<TrustedPojo> pojos = new ArrayList<>();
        pojos.add(new TrustedPojo(1.0));
        pojos.add(new TrustedPojo(2.0));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization.serialize(url, outputStream);
        objectOutput.writeObject(pojos);
        objectOutput.flushBuffer();

        byte[] bytes = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInput objectInput = serialization.deserialize(url, inputStream);

        List<?> result = objectInput.readObject(List.class, new java.lang.reflect.ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{TrustedPojo.class};
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        });
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());

        frameworkModel.destroy();
    }

    @Test
    void testReadObjectNullClass() throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        TrustedPojo pojo = new TrustedPojo(99.0);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization.serialize(url, outputStream);
        objectOutput.writeObject(pojo);
        objectOutput.flushBuffer();

        byte[] bytes = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInput objectInput = serialization.deserialize(url, inputStream);

        TrustedPojo result = objectInput.readObject(null);
        Assertions.assertNotNull(result);

        frameworkModel.destroy();
    }

    @Test
    void testConcurrentDeserializationSameType() throws Exception {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        TrustedPojo pojo = new TrustedPojo(123.0);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization.serialize(url, outputStream);
        objectOutput.writeObject(pojo);
        objectOutput.flushBuffer();
        byte[] bytes = outputStream.toByteArray();

        int threadCount = 8;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<TrustedPojo>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                ObjectInput objectInput = serialization.deserialize(url, inputStream);
                return objectInput.readObject(TrustedPojo.class);
            }));
        }

        for (Future<TrustedPojo> future : futures) {
            Assertions.assertEquals(pojo, future.get(10, TimeUnit.SECONDS));
        }

        executor.shutdown();
        frameworkModel.destroy();
    }

    @Test
    void testConcurrentDeserializationDifferentTypes() throws Exception {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        TrustedPojo pojo = new TrustedPojo(456.0);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization.serialize(url, outputStream);
        objectOutput.writeObject(pojo);
        objectOutput.flushBuffer();
        byte[] pojoBytes = outputStream.toByteArray();

        HashMap<String, String> map = new LinkedHashMap<>();
        map.put("key", "value");
        ByteArrayOutputStream mapStream = new ByteArrayOutputStream();
        ObjectOutput mapOutput = serialization.serialize(url, mapStream);
        mapOutput.writeObject(map);
        mapOutput.flushBuffer();
        byte[] mapBytes = mapStream.toByteArray();

        int threadCount = 4;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            final byte[] data = pojoBytes;
            futures.add(executor.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                ObjectInput objectInput = serialization.deserialize(url, inputStream);
                return objectInput.readObject(TrustedPojo.class);
            }));
        }

        for (int i = 0; i < 2; i++) {
            final byte[] data = mapBytes;
            futures.add(executor.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                ObjectInput objectInput = serialization.deserialize(url, inputStream);
                return objectInput.readObject(HashMap.class);
            }));
        }

        for (Future<?> future : futures) {
            Assertions.assertNotNull(future.get(10, TimeUnit.SECONDS));
        }

        executor.shutdown();
        frameworkModel.destroy();
    }

    @Test
    void testReadObjectWithTypeAndString() throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        String value = "test-value";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization.serialize(url, outputStream);
        objectOutput.writeObject(value);
        objectOutput.flushBuffer();

        byte[] bytes = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInput objectInput = serialization.deserialize(url, inputStream);

        String result = objectInput.readObject(String.class, (Type) String.class);
        Assertions.assertEquals(value, result);

        frameworkModel.destroy();
    }

    @Test
    void testReadObjectWithTypeMap() throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        Map<String, TrustedPojo> map = new HashMap<>();
        map.put("first", new TrustedPojo(1.0));
        map.put("second", new TrustedPojo(2.0));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization.serialize(url, outputStream);
        objectOutput.writeObject(map);
        objectOutput.flushBuffer();

        byte[] bytes = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInput objectInput = serialization.deserialize(url, inputStream);

        Map<?, ?> result = objectInput.readObject(Map.class, new java.lang.reflect.ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{String.class, TrustedPojo.class};
            }

            @Override
            public Type getRawType() {
                return Map.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        });
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());

        frameworkModel.destroy();
    }
}
