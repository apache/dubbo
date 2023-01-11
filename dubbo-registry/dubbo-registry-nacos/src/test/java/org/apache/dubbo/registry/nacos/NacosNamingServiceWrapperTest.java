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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

class NacosNamingServiceWrapperTest {

    @Test
    void testSuccess() {
        NamingService namingService = new MockNamingService() {
            @Override
            public void registerInstance(String serviceName, String groupName, Instance instance) throws NacosException {

            }

            @Override
            public List<Instance> getAllInstances(String serviceName, String groupName) throws NacosException {
                return null;
            }
        };

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new NacosNamingServiceWrapper(namingService, 0, 0);
        try {
            nacosNamingServiceWrapper.registerInstance("Test", "Test", null);
        } catch (NacosException e) {
            Assertions.fail(e);
        }
        try {
            nacosNamingServiceWrapper.getAllInstances("Test", "Test");
        } catch (NacosException e) {
            Assertions.fail(e);
        }
    }

    @Test
    void testFailNoRetry() {
        NamingService namingService = new MockNamingService() {
            @Override
            public void registerInstance(String serviceName, String groupName, Instance instance) throws NacosException {
                throw new NacosException();
            }

            @Override
            public List<Instance> getAllInstances(String serviceName, String groupName) throws NacosException {
                throw new NacosException();
            }
        };

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new NacosNamingServiceWrapper(namingService, 0, 0);
        Assertions.assertThrows(NacosException.class, () -> nacosNamingServiceWrapper.registerInstance("Test", "Test", null));
        Assertions.assertThrows(NacosException.class, () -> nacosNamingServiceWrapper.getAllInstances("Test", "Test"));
    }


    @Test
    void testFailRetry() {
        NamingService namingService = new MockNamingService() {
            private final AtomicInteger count1 = new AtomicInteger(0);
            private final AtomicInteger count2 = new AtomicInteger(0);

            @Override
            public void registerInstance(String serviceName, String groupName, Instance instance) throws NacosException {
                if (count1.incrementAndGet() < 10) {
                    throw new NacosException();
                }
            }

            @Override
            public List<Instance> getAllInstances(String serviceName, String groupName) throws NacosException {
                if (count2.incrementAndGet() < 10) {
                    throw new NacosException();
                }
                return null;
            }
        };

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new NacosNamingServiceWrapper(namingService, 5, 10);
        Assertions.assertThrows(NacosException.class, () -> nacosNamingServiceWrapper.registerInstance("Test", "Test", null));
        try {
            nacosNamingServiceWrapper.registerInstance("Test", "Test", null);
        } catch (NacosException e) {
            Assertions.fail(e);
        }

        Assertions.assertThrows(NacosException.class, () -> nacosNamingServiceWrapper.getAllInstances("Test", "Test"));
        try {
            nacosNamingServiceWrapper.getAllInstances("Test", "Test");
        } catch (NacosException e) {
            Assertions.fail(e);
        }

    }
}
