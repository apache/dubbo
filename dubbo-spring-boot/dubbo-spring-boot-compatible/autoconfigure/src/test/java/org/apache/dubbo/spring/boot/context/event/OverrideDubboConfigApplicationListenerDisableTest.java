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
package org.apache.dubbo.spring.boot.context.event;

import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Properties;

/**
 * {@link OverrideDubboConfigApplicationListener} Test
 *
 * @see OverrideDubboConfigApplicationListener
 * @since 2.7.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(
        properties = {
                "dubbo.config.override = false",
                "dubbo.application.name = dubbo-demo-application",
                "dubbo.module.name = dubbo-demo-module",
        }
)
@SpringBootTest(
        classes = {OverrideDubboConfigApplicationListener.class}
)
public class OverrideDubboConfigApplicationListenerDisableTest {

    @Before
    public void init() {
        ConfigUtils.getProperties().clear();
        ApplicationModel.reset();
    }

    @After
    public void destroy() {
        ApplicationModel.reset();
    }

    @Test
    public void testOnApplicationEvent() {

        Properties properties = ConfigUtils.getProperties();

        Assert.assertTrue(properties.isEmpty());
        Assert.assertNull(properties.get("dubbo.application.name"));
        Assert.assertNull(properties.get("dubbo.module.name"));

    }

}
