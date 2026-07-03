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
import org.apache.dubbo.common.utils.SerializeCheckStatus;
import org.apache.dubbo.common.utils.SerializeSecurityManager;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.example.test.TestPojo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FastJson2SerializationTest {

    public static class RefOuter implements Serializable {
        private List<RefInner> items;

        public List<RefInner> getItems() {
            return items;
        }

        public void setItems(List<RefInner> items) {
            this.items = items;
        }
    }

    public static class RefInner implements Serializable {
        private String name;
        private List<Long> ids;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }
    }

    @Test
    void testReadString() throws IOException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        // write string, read string
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject("hello");
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertEquals("hello", objectInput.readUTF());
        }

        // write string, read string
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(null);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertNull(objectInput.readUTF());
        }

        // write date, read failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(new HashMap<>());
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, objectInput::readUTF);
        }

        // write pojo, read failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(new TrustedPojo(ThreadLocalRandom.current().nextDouble()));
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertInstanceOf(String.class, objectInput.readUTF());
        }

        // write map, read failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(new HashMap<>());
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, objectInput::readUTF);
        }

        // write list, read failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(new LinkedList<>());
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertInstanceOf(String.class, objectInput.readUTF());
        }

        frameworkModel.destroy();
    }

    @Test
    void testReadEvent() throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        // write string, read event
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject("hello");
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertEquals("hello", objectInput.readEvent());
        }

        // write date, read failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(new HashMap<>());
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, objectInput::readEvent);
        }

        // write pojo, read failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(new TrustedPojo(ThreadLocalRandom.current().nextDouble()));
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertInstanceOf(String.class, objectInput.readEvent());
        }

        // write map, read failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(new HashMap<>());
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, objectInput::readEvent);
        }

        // write list, read failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(new LinkedList<>());
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertInstanceOf(String.class, objectInput.readEvent());
        }

        frameworkModel.destroy();
    }

    @Test
    void testReadByte() throws IOException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        // write byte, read byte
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject((byte) 11);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertEquals((byte) 11, objectInput.readByte());
        }

        // write date, read failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            // fastjson2 could not read byte from the serialization of LocalDate by now. 2025/04/02
            objectOutput.writeObject(LocalDate.now());
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, objectInput::readByte);
        }

        // write pojo, read failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(new TrustedPojo(ThreadLocalRandom.current().nextDouble()));
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, objectInput::readByte);
        }

        // write map, read failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(new HashMap<>());
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, objectInput::readByte);
        }

        // write list, read failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(new LinkedList<>());
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, objectInput::readByte);
        }

        frameworkModel.destroy();
    }

    @Test
    void testReadObject() throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        // write pojo, read pojo
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo =
                    new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertEquals(trustedPojo, objectInput.readObject());
        }

        // write list, read list
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo =
                    new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            LinkedList<TrustedPojo> pojos = new LinkedList<>();
            pojos.add(trustedPojo);

            objectOutput.writeObject(pojos);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertEquals(pojos, objectInput.readObject());
        }

        // write pojo, read pojo
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo =
                    new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertEquals(trustedPojo, objectInput.readObject(TrustedPojo.class));
        }

        // write list, read list
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo =
                    new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            LinkedList<TrustedPojo> pojos = new LinkedList<>();
            pojos.add(trustedPojo);

            objectOutput.writeObject(pojos);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertEquals(pojos, objectInput.readObject(List.class));
        }

        // write list, read list
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo =
                    new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            LinkedList<TrustedPojo> pojos = new LinkedList<>();
            pojos.add(trustedPojo);

            objectOutput.writeObject(pojos);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertEquals(pojos, objectInput.readObject(LinkedList.class));
        }

        frameworkModel.destroy();
    }

    @Test
    void testConcurrentReadObjectWithReferences() throws Exception {
        FrameworkModel frameworkModel = new FrameworkModel();
        try {
            Serialization serialization =
                    frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
            URL url = URL.valueOf("").setScopeModel(frameworkModel);
            byte[] bytes = serializeRefOuter(serialization, url);

            Assertions.assertEquals(0, countNullIds(serialization, url, bytes));
            Assertions.assertEquals(0, countConcurrentNullIds(serialization, url, bytes));
        } finally {
            frameworkModel.destroy();
        }
    }

    @Test
    void testReadObjectNotMatched() throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
        frameworkModel
                .getBeanFactory()
                .getBean(SerializeSecurityManager.class)
                .setCheckStatus(SerializeCheckStatus.STRICT);
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        // write pojo, read list failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo =
                    new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, () -> objectInput.readObject(List.class));
        }

        // write pojo, read list failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo =
                    new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, () -> objectInput.readObject(LinkedList.class));
        }

        // write pojo, read string failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo =
                    new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertInstanceOf(String.class, objectInput.readObject(String.class));
        }

        // write pojo, read other failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo =
                    new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, () -> objectInput.readObject(TrustedNotSerializable.class));
        }

        // write pojo, read same field failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo =
                    new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, () -> objectInput.readObject(TrustedPojo2.class));
        }

        // write pojo, read map failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo =
                    new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, () -> objectInput.readObject(Map.class));
        }

        // write list, read pojo failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo =
                    new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            LinkedList<TrustedPojo> pojos = new LinkedList<>();
            pojos.add(trustedPojo);

            objectOutput.writeObject(pojos);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, () -> objectInput.readObject(TrustedPojo.class));
        }

        // write list, read map failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo =
                    new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            LinkedList<TrustedPojo> pojos = new LinkedList<>();
            pojos.add(trustedPojo);

            objectOutput.writeObject(pojos);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, () -> objectInput.readObject(Map.class));
        }

        frameworkModel.destroy();
    }

    @Test
    void testLimit1() throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        // write trusted, read trusted
        TrustedPojo trustedPojo = new TrustedPojo(ThreadLocalRandom.current().nextDouble());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization.serialize(url, outputStream);
        objectOutput.writeObject(trustedPojo);
        objectOutput.flushBuffer();

        byte[] bytes = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInput objectInput = serialization.deserialize(url, inputStream);
        Assertions.assertEquals(trustedPojo, objectInput.readObject());

        frameworkModel.destroy();
    }

    @Test
    void testLimit4() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // write force untrusted, read failed

        {
            FrameworkModel frameworkModel = new FrameworkModel();
            Serialization serialization =
                    frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
            URL url = URL.valueOf("").setScopeModel(frameworkModel);

            TestPojo trustedPojo = new TestPojo("12345");

            frameworkModel
                    .getBeanFactory()
                    .getBean(SerializeSecurityManager.class)
                    .addToAllowed(trustedPojo.getClass().getName());
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            frameworkModel.destroy();
        }

        {
            FrameworkModel frameworkModel = new FrameworkModel();
            Serialization serialization =
                    frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
            URL url = URL.valueOf("").setScopeModel(frameworkModel);

            byte[] bytes = outputStream.toByteArray();
            frameworkModel
                    .getBeanFactory()
                    .getBean(SerializeSecurityManager.class)
                    .setCheckStatus(SerializeCheckStatus.STRICT);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, objectInput::readObject);
            frameworkModel.destroy();
        }
    }

    @Test
    void testLimit5() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // write force un-serializable, read failed

        {
            FrameworkModel frameworkModel = new FrameworkModel();
            Serialization serialization =
                    frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
            URL url = URL.valueOf("").setScopeModel(frameworkModel);

            TrustedNotSerializable trustedPojo =
                    new TrustedNotSerializable(ThreadLocalRandom.current().nextDouble());

            frameworkModel
                    .getBeanFactory()
                    .getBean(SerializeSecurityManager.class)
                    .setCheckSerializable(false);
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            frameworkModel.destroy();
        }

        {
            FrameworkModel frameworkModel = new FrameworkModel();
            Serialization serialization =
                    frameworkModel.getExtensionLoader(Serialization.class).getExtension("fastjson2");
            URL url = URL.valueOf("").setScopeModel(frameworkModel);

            byte[] bytes = outputStream.toByteArray();
            frameworkModel
                    .getBeanFactory()
                    .getBean(SerializeSecurityManager.class)
                    .setCheckStatus(SerializeCheckStatus.STRICT);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(IOException.class, objectInput::readObject);
            frameworkModel.destroy();
        }
    }

    private byte[] serializeRefOuter(Serialization serialization, URL url) throws IOException {
        RefOuter outer = new RefOuter();
        List<RefInner> items = new ArrayList<>();
        List<Long> sharedIds = new ArrayList<>();
        sharedIds.add(1L);
        sharedIds.add(2L);
        for (int i = 0; i < 20; i++) {
            RefInner inner = new RefInner();
            inner.setName("item-" + i);
            inner.setIds(sharedIds);
            items.add(inner);
        }
        outer.setItems(items);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization.serialize(url, outputStream);
        objectOutput.writeObject(outer);
        objectOutput.flushBuffer();
        return outputStream.toByteArray();
    }

    private int countConcurrentNullIds(Serialization serialization, URL url, byte[] bytes) throws Exception {
        int rounds = 10;
        int threadCount = 200;
        AtomicInteger totalNullTasks = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        for (int round = 0; round < rounds; round++) {
            clearObjectReaderCache();
            CyclicBarrier barrier = new CyclicBarrier(threadCount);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger roundNullTasks = new AtomicInteger();
            for (int i = 0; i < threadCount; i++) {
                Thread thread = new Thread(() -> {
                    try {
                        barrier.await();
                        if (countNullIds(serialization, url, bytes) > 0) {
                            roundNullTasks.incrementAndGet();
                        }
                    } catch (Throwable throwable) {
                        failure.compareAndSet(null, throwable);
                    } finally {
                        endLatch.countDown();
                    }
                });
                thread.start();
            }
            endLatch.await();
            if (failure.get() != null) {
                throw new AssertionError("Concurrent deserialization failed", failure.get());
            }
            totalNullTasks.addAndGet(roundNullTasks.get());
        }

        return totalNullTasks.get();
    }

    private int countNullIds(Serialization serialization, URL url, byte[] bytes) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInput objectInput = serialization.deserialize(url, inputStream);
        RefOuter outer = objectInput.readObject(RefOuter.class);
        if (outer == null || outer.getItems() == null) {
            return 1;
        }

        int nullCount = 0;
        for (RefInner inner : outer.getItems()) {
            if (inner == null || inner.getIds() == null) {
                nullCount++;
            }
        }
        return nullCount;
    }

    @SuppressWarnings("unchecked")
    private void clearObjectReaderCache() throws Exception {
        ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
        for (String fieldName : new String[] {"cache", "cacheFieldBased"}) {
            Field field = ObjectReaderProvider.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object cache = field.get(provider);
            if (cache instanceof Map) {
                ((Map<?, ?>) cache).clear();
            }
        }

        Field readerCacheField = ObjectReaderProvider.class.getDeclaredField("readerCache");
        readerCacheField.setAccessible(true);
        readerCacheField.set(null, null);
    }
}
