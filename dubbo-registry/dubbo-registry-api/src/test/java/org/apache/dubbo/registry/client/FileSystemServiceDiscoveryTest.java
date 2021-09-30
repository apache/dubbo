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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URLBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.registry.client.DefaultServiceInstanceTest.createInstance;

/**
 * {@link FileSystemServiceDiscovery} Test
 *
 * @since 2.7.5
 */
@Disabled("FileSystemServiceDiscovery implementation is not stable enough at present")
public class FileSystemServiceDiscoveryTest {

    private FileSystemServiceDiscovery serviceDiscovery;

    private ServiceInstance serviceInstance;

    @BeforeEach
    public void init() throws Exception {
        serviceDiscovery = new FileSystemServiceDiscovery();
        serviceDiscovery.initialize(new URLBuilder().build());
        serviceInstance = createInstance();
    }

    @AfterEach
    public void destroy() throws Exception {
        serviceDiscovery.destroy();
        serviceInstance = null;
    }

    @Test
    public void testRegisterAndUnregister() {

        serviceDiscovery.register(serviceInstance);

        serviceDiscovery.unregister(serviceInstance);

        serviceDiscovery.register(serviceInstance);
    }
}
