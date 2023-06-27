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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.JsonUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PAYLOAD;
import static org.apache.dubbo.metadata.RevisionResolver.EMPTY_REVISION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Some construction and filter cases are covered in InMemoryMetadataServiceTest
 */
class MetadataInfoTest {
    private static URL url = URL.valueOf("dubbo://30.225.21.30:20880/org.apache.dubbo.registry.service.DemoService2?" +
        "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=5000&deprecated=false&dubbo=2.0.2" +
        "&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.registry.service.DemoService2" +
        "&metadata-type=remote&methods=sayHello&sayHello.timeout=7000&pid=36621&release=&revision=1.0.0&service-name-mapping=true" +
        "&side=provider&timeout=5000&timestamp=1629970068002&version=1.0.0&params-filter=customized,-excluded");

    private static URL url2 = URL.valueOf("dubbo://30.225.21.30:20880/org.apache.dubbo.registry.service.DemoService?" +
        "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=5000&deprecated=false&dubbo=2.0.2" +
        "&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.registry.service.DemoService" +
        "&metadata-type=remote&methods=sayHello&pid=36621&release=&revision=1.0.0&service-name-mapping=true" +
        "&side=provider&timeout=5000&timestamp=1629970068002&version=1.0.0&params-filter=customized,-excluded");

    private static URL url3 = URL.valueOf("dubbo://30.225.21.30:20880/org.apache.dubbo.registry.service.DemoService?" +
        "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=5000&deprecated=false&dubbo=2.0.2" +
        "&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.registry.service.DemoService" +
        "&metadata-type=remote&methods=sayHello&sayHello.timeout=7000&pid=36621&release=&revision=1.0.0&service-name-mapping=true" +
        "&side=provider&timeout=5000&timestamp=1629970068002&version=1.0.0&params-filter=-customized,excluded");

    private static URL url4 = URL.valueOf("dubbo://30.225.21.30:20880/org.apache.dubbo.registry.service.DemoService?" +
        "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=5000&deprecated=false&dubbo=2.0.2" +
        "&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.registry.service.DemoService" +
        "&metadata-type=remote&methods=sayHello&sayHello.timeout=7000&pid=36621&release=&revision=1.0.0&service-name-mapping=true" +
        "&side=provider&timeout=5000&timestamp=1629970068002&version=1.0.0&params-filter=-customized,excluded&payload=1024");

    @Test
    void testEmptyRevision() {
        MetadataInfo metadataInfo = new MetadataInfo("demo");
        metadataInfo.setApp("demo");

        Assertions.assertEquals(EMPTY_REVISION, metadataInfo.calAndGetRevision());
    }

    @Test
    void testParamsFilterIncluded() {
        MetadataInfo metadataInfo = new MetadataInfo("demo");

        // export normal url again
        metadataInfo.addService(url);
        MetadataInfo.ServiceInfo serviceInfo2 = metadataInfo.getServiceInfo(url.getProtocolServiceKey());
        assertNotNull(serviceInfo2);
        assertEquals(5, serviceInfo2.getParams().size());
        assertNull(serviceInfo2.getParams().get(INTERFACE_KEY));
        assertNull(serviceInfo2.getParams().get("delay"));
        assertNotNull(serviceInfo2.getParams().get(APPLICATION_KEY));
        assertNotNull(serviceInfo2.getParams().get(VERSION_KEY));
        assertNotNull(serviceInfo2.getParams().get(GROUP_KEY));
        assertNotNull(serviceInfo2.getParams().get(TIMEOUT_KEY));
        assertEquals("7000", serviceInfo2.getMethodParameter("sayHello", TIMEOUT_KEY, "1000"));
    }

    @Test
    void testParamsFilterExcluded() {
        MetadataInfo metadataInfo = new MetadataInfo("demo");

        // export normal url again
        metadataInfo.addService(url3);
        MetadataInfo.ServiceInfo serviceInfo3 = metadataInfo.getServiceInfo(url3.getProtocolServiceKey());
        assertNotNull(serviceInfo3);
        assertEquals(14, serviceInfo3.getParams().size());
        assertNotNull(serviceInfo3.getParams().get(INTERFACE_KEY));
        assertNotNull(serviceInfo3.getParams().get(APPLICATION_KEY));
        assertNotNull(serviceInfo3.getParams().get(VERSION_KEY));
        assertNull(serviceInfo3.getParams().get(GROUP_KEY));
        assertNull(serviceInfo3.getParams().get(TIMEOUT_KEY));
        assertNull(serviceInfo3.getParams().get("anyhost"));
        assertEquals("1000", serviceInfo3.getMethodParameter("sayHello", TIMEOUT_KEY, "1000"));
    }

    @Test
    void testEqualsAndRevision() {
        // same metadata
        MetadataInfo metadataInfo = new MetadataInfo("demo");
        metadataInfo.addService(url);
        MetadataInfo sameMetadataInfo = new MetadataInfo("demo");
        sameMetadataInfo.addService(url);
        assertEquals(metadataInfo, sameMetadataInfo);
        assertEquals(metadataInfo.calAndGetRevision(), sameMetadataInfo.calAndGetRevision());

        // url with different params that are not counted in ServiceInfo
        MetadataInfo metadataInfoWithDifferentParam1 = new MetadataInfo("demo");
        metadataInfoWithDifferentParam1.addService(url.addParameter("delay", 6000));
        assertEquals(metadataInfo, metadataInfoWithDifferentParam1);
        assertEquals(metadataInfo.calAndGetRevision(), metadataInfoWithDifferentParam1.calAndGetRevision());
        // url with different params that are counted in ServiceInfo
        MetadataInfo metadataInfoWithDifferentParam2 = new MetadataInfo("demo");
        metadataInfoWithDifferentParam2.addService(url.addParameter(TIMEOUT_KEY, 6000));
        assertNotEquals(metadataInfo, metadataInfoWithDifferentParam2);
        assertNotEquals(metadataInfo.calAndGetRevision(), metadataInfoWithDifferentParam2.calAndGetRevision());

        MetadataInfo metadataInfoWithDifferentGroup = new MetadataInfo("demo");
        metadataInfoWithDifferentGroup.addService(url.addParameter(GROUP_KEY, "newGroup"));
        assertNotEquals(metadataInfo, metadataInfoWithDifferentGroup);
        assertNotEquals(metadataInfo.calAndGetRevision(), metadataInfoWithDifferentGroup.calAndGetRevision());

        MetadataInfo metadataInfoWithDifferentServices = new MetadataInfo("demo");
        metadataInfoWithDifferentServices.addService(url);
        metadataInfoWithDifferentServices.addService(url2);
        assertNotEquals(metadataInfo, metadataInfoWithDifferentServices);
        assertNotEquals(metadataInfo.calAndGetRevision(), metadataInfoWithDifferentServices.calAndGetRevision());
    }

    @Test
    void testChanged() {
        MetadataInfo metadataInfo = new MetadataInfo("demo");
        metadataInfo.addService(url);
        metadataInfo.addService(url2);
        assertTrue(metadataInfo.updated);
        metadataInfo.calAndGetRevision();
        assertFalse(metadataInfo.updated);
        metadataInfo.removeService(url2);
        assertTrue(metadataInfo.updated);
    }

    @Test
    void testJsonFormat() {
        MetadataInfo metadataInfo = new MetadataInfo("demo");

        // export normal url again
        metadataInfo.addService(url);
        System.out.println(JsonUtils.toJson(metadataInfo));

        MetadataInfo metadataInfo2 = new MetadataInfo("demo");
        // export normal url again
        metadataInfo2.addService(url);
        metadataInfo2.addService(url2);
        System.out.println(JsonUtils.toJson(metadataInfo2));

    }

    @Test
    void testJdkSerialize() throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        MetadataInfo metadataInfo = new MetadataInfo("demo");
        metadataInfo.addService(url);
        objectOutputStream.writeObject(metadataInfo);
        objectOutputStream.close();
        byteArrayOutputStream.close();
        byte[] bytes = byteArrayOutputStream.toByteArray();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        MetadataInfo metadataInfo2 = (MetadataInfo) objectInputStream.readObject();
        objectInputStream.close();

        Assertions.assertEquals(metadataInfo, metadataInfo2);
        Field initiatedField = MetadataInfo.class.getDeclaredField("initiated");
        initiatedField.setAccessible(true);
        Assertions.assertInstanceOf(AtomicBoolean.class, initiatedField.get(metadataInfo2));
        Assertions.assertFalse(((AtomicBoolean)initiatedField.get(metadataInfo2)).get());
    }

    @Test
    void testCal() {
        MetadataInfo metadataInfo = new MetadataInfo("demo");

        // export normal url again
        metadataInfo.addService(url);

        metadataInfo.calAndGetRevision();

        metadataInfo.addService(url2);

        metadataInfo.calAndGetRevision();

        metadataInfo.addService(url3);

        metadataInfo.calAndGetRevision();

        Map<String, Object> ret  = JsonUtils.toJavaObject(metadataInfo.getContent(), Map.class);
        assertNull(ret.get("content"));
        assertNull(ret.get("rawMetadataInfo"));
    }

    @Test
    void testPayload() {
        MetadataInfo metadataInfo = new MetadataInfo("demo");

        metadataInfo.addService(url4);
        MetadataInfo.ServiceInfo serviceInfo4 = metadataInfo.getServiceInfo(url4.getProtocolServiceKey());
        assertNotNull(serviceInfo4);
        assertEquals("1024", serviceInfo4.getParameter(PAYLOAD));
    }
}
