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
package org.apache.dubbo.spring.boot.actuate.health;

import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 * {@link DubboHealthIndicator} Test
 *
 * @see DubboHealthIndicator
 * @since 2.7.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
        "dubbo.application.id = my-application-1",
        "dubbo.application.name = dubbo-demo-application-1",
        "dubbo.protocol.id = dubbo-protocol",
        "dubbo.protocol.name = dubbo",
        "dubbo.protocol.port = 12345",
        "dubbo.protocol.status = registry",
        "dubbo.provider.id = dubbo-provider",
        "dubbo.provider.status = server",
        "management.health.dubbo.status.defaults = memory",
        "management.health.dubbo.status.extras = load,threadpool"
})
@SpringBootTest(
        classes = {
                DubboHealthIndicator.class,
                DubboHealthIndicatorTest.class
        }
)
@EnableConfigurationProperties(DubboHealthIndicatorProperties.class)
@EnableDubboConfig
public class DubboHealthIndicatorTest {

    @Autowired
    private DubboHealthIndicator dubboHealthIndicator;

    @Test
    public void testResolveStatusCheckerNamesMap() {

        Map<String, String> statusCheckerNamesMap = dubboHealthIndicator.resolveStatusCheckerNamesMap();

        Assert.assertEquals(5, statusCheckerNamesMap.size());

        Assert.assertEquals("dubbo-protocol@ProtocolConfig.getStatus()", statusCheckerNamesMap.get("registry"));
        Assert.assertEquals("dubbo-provider@ProviderConfig.getStatus()", statusCheckerNamesMap.get("server"));
        Assert.assertEquals("management.health.dubbo.status.defaults", statusCheckerNamesMap.get("memory"));
        Assert.assertEquals("management.health.dubbo.status.extras", statusCheckerNamesMap.get("load"));
        Assert.assertEquals("management.health.dubbo.status.extras", statusCheckerNamesMap.get("threadpool"));

    }

    @Test
    public void testHealth() {

        Health health = dubboHealthIndicator.health();

        Assert.assertEquals(Status.UNKNOWN, health.getStatus());

    }
}
