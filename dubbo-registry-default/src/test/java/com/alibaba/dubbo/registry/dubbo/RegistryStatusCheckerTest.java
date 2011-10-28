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

import org.junit.Before;
import org.junit.Test;

import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.status.Status;
import com.alibaba.dubbo.registry.RegistryFactory;
import com.alibaba.dubbo.registry.support.AbstractRegistryFactory;
import com.alibaba.dubbo.registry.support.RegistryStatusChecker;
import com.alibaba.dubbo.registry.support.SimpleRegistryExporter;

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
        StringBuilder buf = new StringBuilder();
        buf.append(registryUrl.getAddress());
        buf.append("(connected)");
        buf.append(",");
        buf.append(registryUrl2.getAddress());
        buf.append("(connected)");
        assertEquals(Status.Level.OK, new RegistryStatusChecker().check().getLevel());
        assertEquals(buf.toString(), new RegistryStatusChecker().check().getMessage());
    }
}