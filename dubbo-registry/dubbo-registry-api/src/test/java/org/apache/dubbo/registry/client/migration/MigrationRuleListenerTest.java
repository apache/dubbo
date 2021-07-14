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
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MigrationRuleListenerTest {
    @Test
    public void test() throws InterruptedException {
        String rule = "key: demo-consumer\n" +
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

        DynamicConfiguration dynamicConfiguration = Mockito.mock(DynamicConfiguration.class);

        ApplicationModel.getEnvironment().setDynamicConfiguration(dynamicConfiguration);
        ApplicationModel.getEnvironment().setLocalMigrationRule(rule);
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("demo-consumer");
        ApplicationModel.getConfigManager().setApplication(applicationConfig);

        URL consumerURL = Mockito.mock(URL.class);
        Mockito.when(consumerURL.getServiceKey()).thenReturn("Test");
        Mockito.when(consumerURL.getParameter("timestamp")).thenReturn("1");

        System.setProperty("dubbo.application.migration.delay", "1000");
        MigrationRuleHandler<?> handler = Mockito.mock(MigrationRuleHandler.class, Mockito.withSettings().verboseLogging());

        MigrationRuleListener migrationRuleListener = new MigrationRuleListener();
        MigrationInvoker<?> migrationInvoker = Mockito.mock(MigrationInvoker.class);
        migrationRuleListener.getHandlers().put(migrationInvoker, handler);

        Thread.sleep(5000);
        Mockito.verify(handler, Mockito.timeout(5000)).doMigrate(Mockito.any());

        migrationRuleListener.onRefer(null, migrationInvoker, consumerURL, null);
        Mockito.verify(handler, Mockito.times(2)).doMigrate(Mockito.any());

        ApplicationModel.reset();
    }
}
