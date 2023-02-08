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
package org.apache.dubbo.registry.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for NacosRegistryFactory
 */
class NacosRegistryFactoryTest {

    private NacosRegistryFactory nacosRegistryFactory;

    @BeforeEach
    public void setup() {
        nacosRegistryFactory = new NacosRegistryFactory();
    }

    @AfterEach
    public void teardown() {
    }

    @Test
    void testCreateRegistryCacheKey() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostAddress() + ":8080?nacos.check=false");
        String registryCacheKey1 = nacosRegistryFactory.createRegistryCacheKey(url);
        String registryCacheKey2 = nacosRegistryFactory.createRegistryCacheKey(url);
        Assertions.assertEquals(registryCacheKey1, registryCacheKey2);
    }

    @Test
    void testCreateRegistryCacheKeyWithNamespace() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostAddress() + ":8080?namespace=test&nacos.check=false");
        String registryCacheKey1 = nacosRegistryFactory.createRegistryCacheKey(url);
        String registryCacheKey2 = nacosRegistryFactory.createRegistryCacheKey(url);
        Assertions.assertEquals(registryCacheKey1, registryCacheKey2);
    }

}
