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

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
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
public class MetadataInfoTest {
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

    @Test
    public void testEmptyRevision() {
        MetadataInfo metadataInfo = new MetadataInfo("demo");
        metadataInfo.setApp("demo");

        Assertions.assertEquals(EMPTY_REVISION, metadataInfo.calAndGetRevision());
    }

    @Test
    public void testParamsFiltered() {
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
    public void testEqualsAndRevision() {
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
    public void testChanged() {
        MetadataInfo metadataInfo = new MetadataInfo("demo");
        metadataInfo.addService(url);
        metadataInfo.addService(url2);
        assertTrue(metadataInfo.updated.get());
        metadataInfo.calAndGetRevision();
        assertFalse(metadataInfo.updated.get());
        metadataInfo.removeService(url2);
        assertTrue(metadataInfo.updated.get());
    }

    @Test
    public void testJsonFormat() {
        MetadataInfo metadataInfo = new MetadataInfo("demo");

        // export normal url again
        metadataInfo.addService(url);
        Gson gson = new Gson();
       System.out.println(gson.toJson(metadataInfo));

        MetadataInfo metadataInfo2 = new MetadataInfo("demo");
        // export normal url again
        metadataInfo2.addService(url);
        metadataInfo2.addService(url2);
        System.out.println(gson.toJson(metadataInfo2));

    }
}
