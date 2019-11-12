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
package org.apache.dubbo.common.config.configcenter.file;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.DynamicConfigurationFactory;
import org.apache.dubbo.common.config.configcenter.nop.NopDynamicConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link FileSystemDynamicConfigurationFactory} Test
 *
 * @since 2.7.4
 */
public class FileSystemDynamicConfigurationFactoryTest {

    @Test
    public void testGetFactory() {
        DynamicConfigurationFactory factory = DynamicConfigurationFactory.getDynamicConfigurationFactory("not-exists");
        assertEquals(factory, DynamicConfigurationFactory.getDynamicConfigurationFactory("nop"));
        assertEquals(factory.getDynamicConfiguration(URL.valueOf("dummy")), factory.getDynamicConfiguration(URL.valueOf("dummy")));
        assertEquals(NopDynamicConfiguration.class, factory.getDynamicConfiguration(URL.valueOf("dummy")).getClass());
    }
}
