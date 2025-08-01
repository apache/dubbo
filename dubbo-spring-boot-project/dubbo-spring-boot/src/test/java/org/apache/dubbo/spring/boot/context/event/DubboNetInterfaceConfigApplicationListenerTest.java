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

import org.apache.dubbo.common.utils.SystemPropertyConfigUtils;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import static org.apache.dubbo.common.constants.CommonConstants.DubboProperty.DUBBO_NETWORK_IGNORED_INTERFACE;
import static org.apache.dubbo.common.constants.CommonConstants.DubboProperty.DUBBO_PREFERRED_NETWORK_INTERFACE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @since 3.3
 */
public class DubboNetInterfaceConfigApplicationListenerTest {

    private static final String USE_NETWORK_INTERFACE_NAME = "eth0";

    private static final String IGNORED_NETWORK_INTERFACE_NAME = "eth1";

    @Test
    public void testOnApplicationEvent() {

        SpringApplicationBuilder builder =
                new SpringApplicationBuilder(DubboNetInterfaceConfigApplicationListenerTest.class);
        builder.listeners(new NetworkInterfaceApplicationListener());
        builder.web(WebApplicationType.NONE);
        SpringApplication application = builder.build();
        application.run();

        String preferredNetworkInterface =
                SystemPropertyConfigUtils.getSystemProperty(DUBBO_PREFERRED_NETWORK_INTERFACE);
        String ignoredNetworkInterface = SystemPropertyConfigUtils.getSystemProperty(DUBBO_NETWORK_IGNORED_INTERFACE);
        assertEquals(USE_NETWORK_INTERFACE_NAME, preferredNetworkInterface);
        assertEquals(IGNORED_NETWORK_INTERFACE_NAME, ignoredNetworkInterface);
    }

    static class NetworkInterfaceApplicationListener
            implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

        @Override
        public void onApplicationEvent(ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent) {
            ConfigurableEnvironment environment = applicationEnvironmentPreparedEvent.getEnvironment();
            MutablePropertySources propertySources = environment.getPropertySources();

            Map<String, Object> map = new HashMap<>();
            map.put(DUBBO_PREFERRED_NETWORK_INTERFACE, USE_NETWORK_INTERFACE_NAME);
            map.put(DUBBO_NETWORK_IGNORED_INTERFACE, IGNORED_NETWORK_INTERFACE_NAME);
            propertySources.addLast(new MapPropertySource("networkInterfaceConfig", map));
        }
    }
}
