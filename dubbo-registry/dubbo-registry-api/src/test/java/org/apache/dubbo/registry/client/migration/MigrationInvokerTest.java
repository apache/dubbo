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
package org.apache.dubbo.registry.client.migration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.status.reporter.FrameworkStatusReportService;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.registry.client.migration.model.MigrationRule;
import org.apache.dubbo.registry.client.migration.model.MigrationStep;
import org.apache.dubbo.registry.integration.DemoService;
import org.apache.dubbo.registry.integration.DynamicDirectory;
import org.apache.dubbo.registry.integration.RegistryProtocol;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.LinkedList;
import java.util.List;

public class MigrationInvokerTest {
    @BeforeEach
    public void before() {
        FrameworkModel.destroyAll();
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("Test");
        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(applicationConfig);
        ApplicationModel.defaultModel().getBeanFactory().registerBean(FrameworkStatusReportService.class);
    }

    @AfterEach
    public void after() {
        FrameworkModel.destroyAll();
    }

    @Test
    public void test() {
        RegistryProtocol registryProtocol = Mockito.mock(RegistryProtocol.class);

        ClusterInvoker invoker = Mockito.mock(ClusterInvoker.class);
        ClusterInvoker serviceDiscoveryInvoker = Mockito.mock(ClusterInvoker.class);

        DynamicDirectory directory = Mockito.mock(DynamicDirectory.class);
        DynamicDirectory serviceDiscoveryDirectory = Mockito.mock(DynamicDirectory.class);

        Mockito.when(invoker.getDirectory()).thenReturn(directory);
        Mockito.when(serviceDiscoveryInvoker.getDirectory()).thenReturn(serviceDiscoveryDirectory);

        Mockito.when(invoker.hasProxyInvokers()).thenReturn(true);
        Mockito.when(serviceDiscoveryInvoker.hasProxyInvokers()).thenReturn(true);

        List<Invoker> invokers = new LinkedList<>();
        invokers.add(Mockito.mock(Invoker.class));
        invokers.add(Mockito.mock(Invoker.class));
        List<Invoker> serviceDiscoveryInvokers = new LinkedList<>();
        serviceDiscoveryInvokers.add(Mockito.mock(Invoker.class));
        serviceDiscoveryInvokers.add(Mockito.mock(Invoker.class));
        Mockito.when(directory.getAllInvokers()).thenReturn(invokers);
        Mockito.when(serviceDiscoveryDirectory.getAllInvokers()).thenReturn(serviceDiscoveryInvokers);

        Mockito.when(registryProtocol.getInvoker(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(invoker);
        Mockito.when(registryProtocol.getServiceDiscoveryInvoker(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(serviceDiscoveryInvoker);

        URL consumerURL = Mockito.mock(URL.class);
        Mockito.when(consumerURL.getServiceInterface()).thenReturn("Test");
        Mockito.when(consumerURL.getGroup()).thenReturn("Group");
        Mockito.when(consumerURL.getVersion()).thenReturn("0.0.0");
        Mockito.when(consumerURL.getServiceKey()).thenReturn("Group/Test:0.0.0");
        Mockito.when(consumerURL.getDisplayServiceKey()).thenReturn("Test:0.0.0");
        Mockito.when(consumerURL.getOrDefaultApplicationModel()).thenReturn(ApplicationModel.defaultModel());

        Mockito.when(invoker.getUrl()).thenReturn(consumerURL);
        Mockito.when(serviceDiscoveryInvoker.getUrl()).thenReturn(consumerURL);

        MigrationInvoker migrationInvoker = new MigrationInvoker(registryProtocol, null, null, DemoService.class, null, consumerURL);

        MigrationRule migrationRule = Mockito.mock(MigrationRule.class);
        Mockito.when(migrationRule.getForce(Mockito.any())).thenReturn(true);
        migrationInvoker.migrateToForceInterfaceInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.FORCE_INTERFACE);
        migrationInvoker.invoke(null);
        Mockito.verify(invoker, Mockito.times(1)).invoke(null);

        migrationInvoker.migrateToForceApplicationInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.FORCE_APPLICATION);
        migrationInvoker.invoke(null);
        Mockito.verify(serviceDiscoveryInvoker, Mockito.times(1)).invoke(null);

        Mockito.when(migrationRule.getThreshold(Mockito.any())).thenReturn(1.0f);
        migrationInvoker.migrateToApplicationFirstInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.APPLICATION_FIRST);
        migrationInvoker.invoke(null);
        Mockito.verify(serviceDiscoveryInvoker, Mockito.times(2)).invoke(null);

        Mockito.when(migrationRule.getThreshold(Mockito.any())).thenReturn(2.0f);
        migrationInvoker.migrateToApplicationFirstInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.APPLICATION_FIRST);
        migrationInvoker.invoke(null);
        Mockito.verify(invoker, Mockito.times(2)).invoke(null);

        Mockito.when(migrationRule.getForce(Mockito.any())).thenReturn(false);
        Mockito.when(migrationRule.getThreshold(Mockito.any())).thenReturn(1.0f);
        migrationInvoker.migrateToForceInterfaceInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.FORCE_INTERFACE);
        migrationInvoker.invoke(null);
        Mockito.verify(invoker, Mockito.times(3)).invoke(null);

        migrationInvoker.migrateToForceInterfaceInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.FORCE_INTERFACE);
        migrationInvoker.invoke(null);
        Mockito.verify(invoker, Mockito.times(4)).invoke(null);

        Mockito.when(migrationRule.getThreshold(Mockito.any())).thenReturn(2.0f);
        migrationInvoker.migrateToForceApplicationInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.FORCE_APPLICATION);
        migrationInvoker.invoke(null);
        Mockito.verify(invoker, Mockito.times(5)).invoke(null);

        Mockito.when(migrationRule.getThreshold(Mockito.any())).thenReturn(1.0f);
        migrationInvoker.migrateToForceApplicationInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.FORCE_APPLICATION);
        migrationInvoker.invoke(null);
        Mockito.verify(serviceDiscoveryInvoker, Mockito.times(3)).invoke(null);

        migrationInvoker.migrateToForceApplicationInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.FORCE_APPLICATION);
        migrationInvoker.invoke(null);
        Mockito.verify(serviceDiscoveryInvoker, Mockito.times(4)).invoke(null);

        Mockito.when(migrationRule.getThreshold(Mockito.any())).thenReturn(2.0f);
        migrationInvoker.migrateToForceInterfaceInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.FORCE_INTERFACE);
        migrationInvoker.invoke(null);
        Mockito.verify(serviceDiscoveryInvoker, Mockito.times(5)).invoke(null);

        Mockito.when(migrationRule.getThreshold(Mockito.any())).thenReturn(2.0f);
        Mockito.when(migrationRule.getForce(Mockito.any())).thenReturn(true);
        migrationInvoker.migrateToForceInterfaceInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.FORCE_INTERFACE);
        migrationInvoker.invoke(null);
        Mockito.verify(invoker, Mockito.times(6)).invoke(null);

        migrationInvoker.migrateToForceApplicationInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.FORCE_APPLICATION);
        migrationInvoker.invoke(null);
        Mockito.verify(serviceDiscoveryInvoker, Mockito.times(6)).invoke(null);

        Mockito.when(migrationRule.getForce(Mockito.any())).thenReturn(false);
        migrationInvoker.migrateToForceInterfaceInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.FORCE_INTERFACE);
        migrationInvoker.invoke(null);
        Mockito.verify(serviceDiscoveryInvoker, Mockito.times(7)).invoke(null);
        Assertions.assertNull(migrationInvoker.getInvoker());

        Mockito.when(migrationRule.getForce(Mockito.any())).thenReturn(true);
        migrationInvoker.migrateToForceInterfaceInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.FORCE_INTERFACE);

        Mockito.when(migrationRule.getForce(Mockito.any())).thenReturn(false);
        migrationInvoker.migrateToForceApplicationInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.FORCE_APPLICATION);
        migrationInvoker.invoke(null);
        Mockito.verify(invoker, Mockito.times(7)).invoke(null);
        Assertions.assertNull(migrationInvoker.getServiceDiscoveryInvoker());

        ArgumentCaptor<InvokersChangedListener> argument = ArgumentCaptor.forClass(InvokersChangedListener.class);
        Mockito.verify(serviceDiscoveryDirectory, Mockito.atLeastOnce()).setInvokersChangedListener(argument.capture());

        Mockito.when(migrationRule.getThreshold(Mockito.any())).thenReturn(1.0f);
        migrationInvoker.migrateToApplicationFirstInvoker(migrationRule);
        migrationInvoker.setMigrationStep(MigrationStep.APPLICATION_FIRST);
        for (int i = 0; i < 20; i++) {
            migrationInvoker.invoke(null);
        }
        Mockito.verify(serviceDiscoveryInvoker, Mockito.times(27)).invoke(null);

        serviceDiscoveryInvokers.remove(1);
        Mockito.when(serviceDiscoveryInvoker.hasProxyInvokers()).thenReturn(false);
        argument.getAllValues().get(argument.getAllValues().size() - 1).onChange();

        for (int i = 0; i < 20; i++) {
            migrationInvoker.invoke(null);
        }
        Mockito.verify(invoker, Mockito.times(27)).invoke(null);

        serviceDiscoveryInvokers.add(Mockito.mock(Invoker.class));
        Mockito.when(serviceDiscoveryInvoker.hasProxyInvokers()).thenReturn(true);
        argument.getAllValues().get(argument.getAllValues().size() - 1).onChange();

        Mockito.when(migrationRule.getProportion(Mockito.any())).thenReturn(50);
        migrationInvoker.setMigrationRule(migrationRule);
        for (int i = 0; i < 1000; i++) {
            migrationInvoker.invoke(null);
        }
        Mockito.verify(serviceDiscoveryInvoker, Mockito.atMost(1026)).invoke(null);
        Mockito.verify(invoker, Mockito.atLeast(28)).invoke(null);

        Mockito.when(migrationRule.getDelay(Mockito.any())).thenReturn(1);
        long currentTimeMillis = System.currentTimeMillis();
        migrationInvoker.migrateToForceApplicationInvoker(migrationRule);
        Assertions.assertTrue(System.currentTimeMillis() - currentTimeMillis >= 2000);
    }

    @Test
    public void testConcurrency() {
        // 独立线程

        // 独立线程invoker状态切换

    }
}
