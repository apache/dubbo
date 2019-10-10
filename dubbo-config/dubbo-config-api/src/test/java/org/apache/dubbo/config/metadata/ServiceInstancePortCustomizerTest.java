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
package org.apache.dubbo.config.metadata;

import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.registry.client.DefaultServiceInstance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ServiceInstancePortCustomizer} Test
 *
 * @since 2.7.4
 */
public class ServiceInstancePortCustomizerTest {

    private ServiceInstancePortCustomizer customizer;

    private DefaultServiceInstance serviceInstance;

    @BeforeEach

    public void init() {
        customizer = new ServiceInstancePortCustomizer();
        serviceInstance = new DefaultServiceInstance();
        ConfigManager.getInstance()
                .addProtocol(new ProtocolConfig("rest", 9090));
    }

    @Test
    public void testCustomizeWithoutSet() {
        serviceInstance.setPort(8080);
        customizer.customize(serviceInstance);
        assertEquals(8080, serviceInstance.getPort());
    }

    @Test
    public void testCustomize() {
        customizer.customize(serviceInstance);
        assertEquals(9090, serviceInstance.getPort());
    }
}
