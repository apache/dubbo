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
package org.apache.dubbo.configcenter.mock;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.configcenter.DynamicConfiguration;
import org.apache.dubbo.configcenter.DynamicConfigurationFactory;
import org.apache.dubbo.configcenter.support.nop.NopDynamicConfigurationFactory;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class AbstractDynamicConfigurationTest {
    public DynamicConfigurationFactory configurationFactory = ExtensionLoader.getExtensionLoader(DynamicConfigurationFactory.class).getExtension("mock");
    public URL url = URL.valueOf("nop://127.0.0.1:10880/DynamicConfiguration");

    @Test
    public void testInit() {
        DynamicConfiguration configuration1 = configurationFactory.getDynamicConfiguration(url);
        DynamicConfiguration configuration2 = configurationFactory.getDynamicConfiguration(url);
        Assert.assertEquals(configuration1, configuration2);
    }

    @Test
    public void testDefaultExtension() {
        DynamicConfigurationFactory factory = ExtensionLoader.getExtensionLoader(DynamicConfigurationFactory.class).getDefaultExtension();
        Assert.assertTrue(factory instanceof NopDynamicConfigurationFactory);
    }
}
