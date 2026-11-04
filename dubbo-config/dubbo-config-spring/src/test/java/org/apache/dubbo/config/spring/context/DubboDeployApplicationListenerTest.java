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

import org.apache.dubbo.common.deploy.ModuleDeployer;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.SysProps;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DubboDeployApplicationListenerTest {

    private static final String RESOURCE = "org/apache/dubbo/config/spring/demo-provider.xml";

    @AfterEach
    void tearDown() {
        DubboBootstrap.getInstance().stop();
        SysProps.clear();
        System.clearProperty("dubbo.protocol.name");
        System.clearProperty("dubbo.registry.address");
    }

    @Test
    void testIntegrationLifecycle() {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(RESOURCE)) {
            context.start();

            DubboDeployApplicationListener listener = context.getBean(DubboDeployApplicationListener.class);
            Assertions.assertTrue(listener.isRunning(), "Dubbo Lifecycle should be running after context start");

            // Verify shutdown phase is configured low (to stop last)
            Assertions.assertTrue(listener.getPhase() < 0, "Dubbo should use a low phase to stop last");

            context.close();
            Assertions.assertFalse(listener.isRunning(), "Dubbo Lifecycle should stop after context close");
        }
    }

    @Test
    void testStartMethodInterrupted() throws ExecutionException, InterruptedException {
        // Expectations: Interrupt status is preserved
        DubboDeployApplicationListener listener = new DubboDeployApplicationListener();
        ModuleModel mockModuleModel = mock(ModuleModel.class);
        ModuleDeployer mockDeployer = mock(ModuleDeployer.class);
        Future mockFuture = mock(Future.class);

        when(mockModuleModel.getDeployer()).thenReturn(mockDeployer);
        when(mockDeployer.start()).thenReturn(mockFuture);
        when(mockDeployer.isBackground()).thenReturn(false);
        when(mockFuture.get()).thenThrow(new InterruptedException("Simulated Interrupt"));

        ReflectionTestUtils.setField(listener, "moduleModel", mockModuleModel);
        GenericApplicationContext context = new GenericApplicationContext();
        context.refresh();

        ReflectionTestUtils.setField(listener, "applicationContext", context);
        ReflectionTestUtils.setField(listener, "running", new AtomicBoolean(false));

        listener.start();

        Assertions.assertFalse(listener.isRunning(), "Running state should reset on interrupt");
        Assertions.assertTrue(Thread.currentThread().isInterrupted(), "Interrupt status should be preserved");
        Thread.interrupted();
    }

    @Test
    void testStartMethodException() throws ExecutionException, InterruptedException {
        // Expectations: Exception does not escape start()
        DubboDeployApplicationListener listener = new DubboDeployApplicationListener();
        ModuleModel mockModuleModel = mock(ModuleModel.class);
        ModuleDeployer mockDeployer = mock(ModuleDeployer.class);
        Future mockFuture = mock(Future.class);

        when(mockModuleModel.getDeployer()).thenReturn(mockDeployer);
        when(mockDeployer.start()).thenReturn(mockFuture);
        when(mockDeployer.isBackground()).thenReturn(false);
        when(mockFuture.get()).thenThrow(new RuntimeException("Simulated Failure"));

        ReflectionTestUtils.setField(listener, "moduleModel", mockModuleModel);
        GenericApplicationContext context = new GenericApplicationContext();
        context.refresh();

        ReflectionTestUtils.setField(listener, "applicationContext", context);
        ReflectionTestUtils.setField(listener, "running", new AtomicBoolean(false));

        listener.start();

        Assertions.assertFalse(listener.isRunning(), "Running state should reset on exception");
        verify(mockDeployer, times(1)).start();
    }

    @BeforeEach
    void setupDubboEnv() {
        System.setProperty("dubbo.protocol.name", "dubbo");
        System.setProperty("dubbo.registry.address", "N/A");
    }
}
