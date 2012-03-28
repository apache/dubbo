/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.registry.dubbo;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.status.Status;
import com.alibaba.dubbo.registry.RegistryFactory;
import com.alibaba.dubbo.registry.status.RegistryStatusChecker;
import com.alibaba.dubbo.registry.support.AbstractRegistryFactory;

/**
 * StatusTest
 * 
 * @author tony.chenl
 */
public class RegistryStatusCheckerTest {

    static {
        SimpleRegistryExporter.exportIfAbsent(9090);
        SimpleRegistryExporter.exportIfAbsent(9091);
    }
    URL registryUrl = URL.valueOf("dubbo://cat:cat@127.0.0.1:9090/");
    URL registryUrl2 = URL.valueOf("dubbo://cat:cat@127.0.0.1:9091");

    @Before
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
        Assert.assertTrue(message.contains(registryUrl.getAddress() + "(connected)"));
        Assert.assertTrue(message.contains(registryUrl2.getAddress() + "(connected)"));
    }
}