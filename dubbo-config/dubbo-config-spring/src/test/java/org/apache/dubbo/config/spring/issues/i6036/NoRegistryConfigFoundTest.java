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
package org.apache.dubbo.config.spring.issues.i6036;

import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

/**
 * https://github.com/apache/dubbo/issues/6036
 *
 * @since 2.7.7
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = {
                NoRegistryConfigFoundTest.class
        })
@PropertySource("classpath:/org/apache/dubbo/config/spring/issues/i6036/application.properties")
@ImportResource(locations = "classpath:/org/apache/dubbo/config/spring/issues/i6036/biz-consumer.xml")
@EnableDubboConfig
public class NoRegistryConfigFoundTest {

    @Autowired
    @Qualifier("registry2")
    private RegistryConfig registryConfig;

    @Test
    public void testCase1() {
        assertEquals("zookeeper://localhost:2181", registryConfig.getAddress());
    }
}
