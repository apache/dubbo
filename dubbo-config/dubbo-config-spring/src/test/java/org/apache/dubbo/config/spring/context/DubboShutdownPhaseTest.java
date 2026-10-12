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
package org.apache.dubbo.config.spring.context;

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.SysProps;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

class DubboShutdownPhaseTest {

    private static final String PHASE_KEY = "dubbo.spring.shutdown.phase";
    private static final int DEFAULT_PHASE = Integer.MIN_VALUE + 2000;

    @AfterEach
    void tearDown() {
        DubboBootstrap.getInstance().stop();
        SysProps.clear();
        System.clearProperty(PHASE_KEY);
        System.clearProperty("dubbo.protocol.name");
        System.clearProperty("dubbo.registry.address");
    }

    @Test
    void testDefaultPhase() {
        try (ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("org/apache/dubbo/config/spring/demo-provider.xml")) {
            context.start();
            DubboDeployApplicationListener listener = context.getBean(DubboDeployApplicationListener.class);
            Assertions.assertEquals(DEFAULT_PHASE, listener.getPhase());
        }
    }

    @Test
    void testValidConfiguredPhase() {
        System.setProperty(PHASE_KEY, "100");

        try (ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("org/apache/dubbo/config/spring/demo-provider.xml")) {
            context.start();
            DubboDeployApplicationListener listener = context.getBean(DubboDeployApplicationListener.class);
            Assertions.assertEquals(100, listener.getPhase());
        }
    }

    @Test
    void testInvalidConfiguredPhase() {
        System.setProperty(PHASE_KEY, "invalid-text");

        try (ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("org/apache/dubbo/config/spring/demo-provider.xml")) {
            context.start();
            DubboDeployApplicationListener listener = context.getBean(DubboDeployApplicationListener.class);
            Assertions.assertEquals(
                    DEFAULT_PHASE,
                    listener.getPhase(),
                    "Default shutdown phase should be used when no configuration is provided");
        }
    }

    @Test
    void testBoundaryValuePhase() {
        System.setProperty(PHASE_KEY, String.valueOf(Integer.MIN_VALUE));

        try (ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("org/apache/dubbo/config/spring/demo-provider.xml")) {
            context.start();
            DubboDeployApplicationListener listener = context.getBean(DubboDeployApplicationListener.class);
            Assertions.assertEquals(Integer.MIN_VALUE + 1, listener.getPhase());
        }
    }

    @BeforeEach
    void setupDubboEnv() {
        System.setProperty("dubbo.protocol.name", "dubbo");
        System.setProperty("dubbo.registry.address", "N/A");
    }
}
