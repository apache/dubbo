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
package org.apache.dubbo.registry.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.status.Status;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.status.RegistryStatusChecker;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * StatusTest
 *
 */
public class RegistryStatusCheckerTest {

    static {
        SimpleRegistryExporter.exportIfAbsent(9090);
        SimpleRegistryExporter.exportIfAbsent(9091);
    }

    URL registryUrl = URL.valueOf("dubbo://cat:cat@127.0.0.1:9090/");
    URL registryUrl2 = URL.valueOf("dubbo://cat:cat@127.0.0.1:9091");

    @BeforeEach
    public void setUp() {
        AbstractRegistryFactory.destroyAll();
    }

    @Test
    public void testCheckUnknown() {
        assertEquals(Status.Level.UNKNOWN, new RegistryStatusChecker().check().getLevel());
    }

    @Test
    public void testCheckOK() {
        ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension().getRegistry(registryUrl);
        ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension().getRegistry(registryUrl2);
        assertEquals(Status.Level.OK, new RegistryStatusChecker().check().getLevel());
        String message = new RegistryStatusChecker().check().getMessage();
        Assertions.assertTrue(message.contains(registryUrl.getAddress() + "(connected)"));
        Assertions.assertTrue(message.contains(registryUrl2.getAddress() + "(connected)"));
    }
}