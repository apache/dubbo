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
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.registry.client.migration.model.MigrationRule;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;

import static org.awaitility.Awaitility.await;

class MigrationRuleListenerTest {

    private String localRule = "key: demo-consumer\n" +
        "step: APPLICATION_FIRST\n" +
        "threshold: 1.0\n" +
        "proportion: 60\n" +
        "delay: 60\n" +
        "force: false\n" +
        "interfaces:\n" +
        "  - serviceKey: DemoService:1.0.0\n" +
        "    threshold: 0.5\n" +
        "    proportion: 30\n" +
        "    delay: 30\n" +
        "    force: true\n" +
        "    step: APPLICATION_FIRST\n" +
        "  - serviceKey: GreetingService:1.0.0\n" +
        "    step: FORCE_APPLICATION";

    private String remoteRule = "key: demo-consumer\n" +
        "step: FORCE_APPLICATION\n" +
        "threshold: 1.0\n" +
        "proportion: 60\n" +
        "delay: 60\n" +
        "force: false\n" +
        "interfaces:\n" +
        "  - serviceKey: DemoService:1.0.0\n" +
        "    threshold: 0.5\n" +
        "    proportion: 30\n" +
        "    delay: 30\n" +
        "    force: true\n" +
        "    step: FORCE_APPLICATION\n" +
        "  - serviceKey: GreetingService:1.0.0\n" +
        "    step: FORCE_INTERFACE";

    private String dynamicRemoteRule = "key: demo-consumer\n" +
        "step: APPLICATION_FIRST\n" +
        "threshold: 1.0\n" +
        "proportion: 60\n" +
        "delay: 60\n" +
        "force: false\n" +
        "interfaces:\n";

    @AfterEach
    public void tearDown() {
        ApplicationModel.reset();
        System.clearProperty("dubbo.application.migration.delay");
    }

    /**
     * Listener started with config center and local rule, no initial remote rule.
     * Check local rule take effect
     */
    @Test
    void test() {
        DynamicConfiguration dynamicConfiguration = Mockito.mock(DynamicConfiguration.class);

        ApplicationModel.reset();
        ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().setDynamicConfiguration(dynamicConfiguration);
        ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().setLocalMigrationRule(localRule);
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("demo-consumer");
        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(applicationConfig);

        URL consumerURL = Mockito.mock(URL.class);
        Mockito.when(consumerURL.getServiceKey()).thenReturn("Test");
        Mockito.when(consumerURL.getParameter("timestamp")).thenReturn("1");

        System.setProperty("dubbo.application.migration.delay", "1");
        MigrationRuleHandler<?> handler = Mockito.mock(MigrationRuleHandler.class, Mockito.withSettings().verboseLogging());

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MigrationRuleListener migrationRuleListener = new MigrationRuleListener(ApplicationModel.defaultModel().getDefaultModule()) {
            @Override
            public synchronized void process(ConfigChangedEvent event) {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                super.process(event);
            }
        };

        MigrationInvoker<?> migrationInvoker = Mockito.mock(MigrationInvoker.class);
        migrationRuleListener.getHandlers().put(migrationInvoker, handler);

        countDownLatch.countDown();
        await().untilAsserted(() -> {
            Mockito.verify(handler).doMigrate(Mockito.any());
        });
//        Mockito.verify(handler, Mockito.timeout(5000)).doMigrate(Mockito.any());

        migrationRuleListener.onRefer(null, migrationInvoker, consumerURL, null);
        Mockito.verify(handler, Mockito.times(2)).doMigrate(Mockito.any());
    }

    /**
     * Test listener started without local rule and config center, INIT should be used and no scheduled task should be started.
     */
    @Test
    void testWithInitAndNoLocalRule() {
        ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().setDynamicConfiguration(null);
        ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().setLocalMigrationRule("");
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("demo-consumer");
        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(applicationConfig);

        URL consumerURL = Mockito.mock(URL.class);
        Mockito.when(consumerURL.getServiceKey()).thenReturn("Test");
        Mockito.when(consumerURL.getParameter("timestamp")).thenReturn("1");

        System.setProperty("dubbo.application.migration.delay", "1000");
        MigrationRuleHandler<?> handler = Mockito.mock(MigrationRuleHandler.class, Mockito.withSettings().verboseLogging());

        MigrationRuleListener migrationRuleListener = new MigrationRuleListener(ApplicationModel.defaultModel().getDefaultModule());
        MigrationInvoker<?> migrationInvoker = Mockito.mock(MigrationInvoker.class);
        migrationRuleListener.getHandlers().put(migrationInvoker, handler);
        migrationRuleListener.onRefer(null, migrationInvoker, consumerURL, null);
        // check migration happened after invoker referred
        Mockito.verify(handler, Mockito.times(1)).doMigrate(MigrationRule.getInitRule());

        // check no delay tasks created for there's no local rule and no config center
        Assertions.assertNull(migrationRuleListener.localRuleMigrationFuture);
        Assertions.assertNull(migrationRuleListener.ruleMigrationFuture);
        Assertions.assertEquals(0, migrationRuleListener.ruleQueue.size());
    }

    /**
     * Listener with config center， initial remote rule and local rule, check
     * 1. initial remote rule other than local rule take effect
     * 2. remote rule change and all invokers gets notified
     */
    @Test
    void testWithConfigurationListenerAndLocalRule() {
        DynamicConfiguration dynamicConfiguration = Mockito.mock(DynamicConfiguration.class);
        Mockito.doReturn(remoteRule).when(dynamicConfiguration).getConfig(Mockito.anyString(), Mockito.anyString());

        ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().setDynamicConfiguration(dynamicConfiguration);
        ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().setLocalMigrationRule(localRule);
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("demo-consumer");
        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(applicationConfig);

        URL consumerURL = Mockito.mock(URL.class);
        Mockito.when(consumerURL.getServiceKey()).thenReturn("Test");
        Mockito.when(consumerURL.getParameter("timestamp")).thenReturn("1");

        URL consumerURL2 = Mockito.mock(URL.class);
        Mockito.when(consumerURL2.getServiceKey()).thenReturn("Test2");
        Mockito.when(consumerURL2.getParameter("timestamp")).thenReturn("2");

        System.setProperty("dubbo.application.migration.delay", "10");
        MigrationRuleHandler<?> handler = Mockito.mock(MigrationRuleHandler.class, Mockito.withSettings().verboseLogging());
        MigrationRuleHandler<?> handler2 = Mockito.mock(MigrationRuleHandler.class, Mockito.withSettings().verboseLogging());

        // Both local rule and remote rule are here
        // Local rule with one delayed task started to apply
        MigrationRuleListener migrationRuleListener = new MigrationRuleListener(ApplicationModel.defaultModel().getDefaultModule());
        Assertions.assertNotNull(migrationRuleListener.localRuleMigrationFuture);
        Assertions.assertNull(migrationRuleListener.ruleMigrationFuture);
        MigrationInvoker<?> migrationInvoker = Mockito.mock(MigrationInvoker.class);
        MigrationInvoker<?> migrationInvoker2 = Mockito.mock(MigrationInvoker.class);

        // Remote rule will be applied when onRefer gets executed
        migrationRuleListener.getHandlers().put(migrationInvoker, handler);
        migrationRuleListener.onRefer(null, migrationInvoker, consumerURL, null);

        MigrationRule tmpRemoteRule = migrationRuleListener.getRule();
        ArgumentCaptor<MigrationRule> captor = ArgumentCaptor.forClass(MigrationRule.class);
        Mockito.verify(handler, Mockito.times(1)).doMigrate(captor.capture());
        Assertions.assertEquals(tmpRemoteRule, captor.getValue());

        await().until(() -> migrationRuleListener.localRuleMigrationFuture.isDone());
        Assertions.assertNull(migrationRuleListener.ruleMigrationFuture);
        Assertions.assertEquals(tmpRemoteRule, migrationRuleListener.getRule());
        Mockito.verify(handler, Mockito.times(1)).doMigrate(Mockito.any());

        ArgumentCaptor<MigrationRule> captor2 = ArgumentCaptor.forClass(MigrationRule.class);
        migrationRuleListener.getHandlers().put(migrationInvoker2, handler2);
        migrationRuleListener.onRefer(null, migrationInvoker2, consumerURL2, null);
        Mockito.verify(handler2, Mockito.times(1)).doMigrate(captor2.capture());
        Assertions.assertEquals(tmpRemoteRule, captor2.getValue());


        migrationRuleListener.process(new ConfigChangedEvent("key", "group", dynamicRemoteRule));

        await().until(migrationRuleListener.ruleQueue::isEmpty);
        await().untilAsserted(() -> {
            Mockito.verify(handler, Mockito.times(2)).doMigrate(Mockito.any());
            Mockito.verify(handler2, Mockito.times(2)).doMigrate(Mockito.any());
        });

        Assertions.assertNotNull(migrationRuleListener.ruleMigrationFuture);
        ArgumentCaptor<MigrationRule> captor_event = ArgumentCaptor.forClass(MigrationRule.class);
        Mockito.verify(handler, Mockito.times(2)).doMigrate(captor_event.capture());
        Assertions.assertEquals("APPLICATION_FIRST", captor_event.getValue().getStep().toString());
        Mockito.verify(handler2, Mockito.times(2)).doMigrate(captor_event.capture());
        Assertions.assertEquals("APPLICATION_FIRST", captor_event.getValue().getStep().toString());
    }
}
