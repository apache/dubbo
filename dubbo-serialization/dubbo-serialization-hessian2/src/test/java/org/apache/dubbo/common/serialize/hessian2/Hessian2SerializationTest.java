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
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.utils.SerializeCheckStatus;
import org.apache.dubbo.common.utils.SerializeSecurityManager;
import org.apache.dubbo.rpc.model.FrameworkModel;

import com.example.test.TestPojo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

class Hessian2SerializationTest {
    @Test
    void testReadString() throws IOException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization = frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
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
            objectOutput.writeObject(new Date());
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
            Assertions.assertThrows(IOException.class, objectInput::readUTF);
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
            Assertions.assertThrows(IOException.class, objectInput::readUTF);
        }

        frameworkModel.destroy();
    }

    @Test
    void testReadEvent() throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization = frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
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
            objectOutput.writeObject(new Date());
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
            Assertions.assertThrows(IOException.class, objectInput::readEvent);
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
            Assertions.assertThrows(IOException.class, objectInput::readEvent);
        }

        frameworkModel.destroy();
    }

    @Test
    void testReadByte() throws IOException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization = frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
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
            objectOutput.writeObject(new Date());
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
        Serialization serialization = frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        // write pojo, read pojo
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo = new TrustedPojo(ThreadLocalRandom.current().nextDouble());
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
            TrustedPojo trustedPojo = new TrustedPojo(ThreadLocalRandom.current().nextDouble());
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
            TrustedPojo trustedPojo = new TrustedPojo(ThreadLocalRandom.current().nextDouble());
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
            TrustedPojo trustedPojo = new TrustedPojo(ThreadLocalRandom.current().nextDouble());
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
            TrustedPojo trustedPojo = new TrustedPojo(ThreadLocalRandom.current().nextDouble());
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
    void testReadObjectNotMatched() throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization = frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        // write pojo, read list failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo = new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(RuntimeException.class, () -> objectInput.readObject(List.class));
        }

        // write pojo, read list failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo = new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(RuntimeException.class, () -> objectInput.readObject(LinkedList.class));
        }

        // write pojo, read string failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo = new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(RuntimeException.class, () -> objectInput.readObject(String.class));
        }

        // write pojo, read other failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo = new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(RuntimeException.class, () -> objectInput.readObject(TrustedNotSerializable.class));
        }

        // write pojo, read same field failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo = new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertInstanceOf(TrustedPojo2.class, objectInput.readObject(TrustedPojo2.class));
        }

        // write pojo, read map failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo = new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertInstanceOf(Map.class, objectInput.readObject(Map.class));
        }

        // write list, read pojo failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo = new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            LinkedList<TrustedPojo> pojos = new LinkedList<>();
            pojos.add(trustedPojo);

            objectOutput.writeObject(pojos);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(RuntimeException.class, () -> objectInput.readObject(TrustedPojo.class));
        }

        // write list, read map failed
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            TrustedPojo trustedPojo = new TrustedPojo(ThreadLocalRandom.current().nextDouble());
            LinkedList<TrustedPojo> pojos = new LinkedList<>();
            pojos.add(trustedPojo);

            objectOutput.writeObject(pojos);
            objectOutput.flushBuffer();

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertThrows(RuntimeException.class, () -> objectInput.readObject(Map.class));
        }

        frameworkModel.destroy();
    }

    @Test
    void testLimit1() throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization = frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
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
    void testLimit2() throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization = frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
        frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class).setCheckStatus(SerializeCheckStatus.STRICT);
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        // write untrusted failed
        TestPojo trustedPojo = new TestPojo("12345");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization.serialize(url, outputStream);
        Assertions.assertThrows(IllegalArgumentException.class, () -> objectOutput.writeObject(trustedPojo));

        frameworkModel.destroy();
    }

    @Test
    void testLimit3() throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization = frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        // write un-serializable failed
        TrustedNotSerializable trustedPojo = new TrustedNotSerializable(ThreadLocalRandom.current().nextDouble());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization.serialize(url, outputStream);
        Assertions.assertThrows(IllegalArgumentException.class, () -> objectOutput.writeObject(trustedPojo));

        frameworkModel.destroy();
    }

    @Test
    void testLimit4() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // write force untrusted, read failed

        {
            FrameworkModel frameworkModel = new FrameworkModel();
            Serialization serialization = frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
            URL url = URL.valueOf("").setScopeModel(frameworkModel);

            TestPojo trustedPojo = new TestPojo("12345");

            frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class).addToAllowed(trustedPojo.getClass().getName());
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            frameworkModel.destroy();
        }

        {
            FrameworkModel frameworkModel = new FrameworkModel();
            Serialization serialization = frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
            URL url = URL.valueOf("").setScopeModel(frameworkModel);

            byte[] bytes = outputStream.toByteArray();
            frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class).setCheckStatus(SerializeCheckStatus.STRICT);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertInstanceOf(Map.class, objectInput.readObject());
            frameworkModel.destroy();
        }
    }

    @Test
    void testLimit5() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // write force un-serializable, read failed

        {
            System.setProperty("dubbo.hessian.allowNonSerializable", "true");
            FrameworkModel frameworkModel = new FrameworkModel();
            Serialization serialization = frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
            URL url = URL.valueOf("").setScopeModel(frameworkModel);

            TrustedNotSerializable trustedPojo = new TrustedNotSerializable(ThreadLocalRandom.current().nextDouble());

            frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class).setCheckSerializable(false);
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(trustedPojo);
            objectOutput.flushBuffer();

            frameworkModel.destroy();
            System.clearProperty("dubbo.hessian.allowNonSerializable");
        }

        {
            FrameworkModel frameworkModel = new FrameworkModel();
            Serialization serialization = frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
            URL url = URL.valueOf("").setScopeModel(frameworkModel);

            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectInput objectInput = serialization.deserialize(url, inputStream);
            Assertions.assertInstanceOf(Map.class, objectInput.readObject());
            frameworkModel.destroy();
        }
    }
}
