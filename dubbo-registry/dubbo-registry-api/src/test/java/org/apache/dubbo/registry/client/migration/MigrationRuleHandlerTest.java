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
import org.apache.dubbo.registry.client.migration.model.MigrationRule;
import org.apache.dubbo.registry.client.migration.model.MigrationStep;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MigrationRuleHandlerTest {
    @Test
    void test() {
        MigrationClusterInvoker<?> invoker = Mockito.mock(MigrationClusterInvoker.class);
        URL url = Mockito.mock(URL.class);
        Mockito.when(url.getDisplayServiceKey()).thenReturn("test");
        Mockito.when(url.getParameter(Mockito.any(), (String) Mockito.any())).thenAnswer(i->i.getArgument(1));
        Mockito.when(url.getOrDefaultApplicationModel()).thenReturn(ApplicationModel.defaultModel());
        MigrationRuleHandler<?> handler = new MigrationRuleHandler<>(invoker, url);

        Mockito.when(invoker.migrateToForceApplicationInvoker(Mockito.any())).thenReturn(true);
        Mockito.when(invoker.migrateToForceInterfaceInvoker(Mockito.any())).thenReturn(true);

        MigrationRule initRule = MigrationRule.getInitRule();
        handler.doMigrate(initRule);
        Mockito.verify(invoker, Mockito.times(1)).migrateToApplicationFirstInvoker(initRule);

        MigrationRule rule = Mockito.mock(MigrationRule.class);
        Mockito.when(rule.getStep(url)).thenReturn(MigrationStep.FORCE_APPLICATION);
        handler.doMigrate(rule);
        Mockito.verify(invoker, Mockito.times(1)).migrateToForceApplicationInvoker(rule);

        Mockito.when(rule.getStep(url)).thenReturn(MigrationStep.APPLICATION_FIRST);
        handler.doMigrate(rule);
        Mockito.verify(invoker, Mockito.times(1)).migrateToApplicationFirstInvoker(rule);

        Mockito.when(rule.getStep(url)).thenReturn(MigrationStep.FORCE_INTERFACE);
        handler.doMigrate(rule);
        Mockito.verify(invoker, Mockito.times(1)).migrateToForceInterfaceInvoker(rule);

        // migration failed, current rule not changed
        testMigrationFailed(rule, url, handler, invoker);
        // rule not changed, check migration not actually executed
        testMigrationWithStepUnchanged(rule, url, handler, invoker);
    }

    private void testMigrationFailed(MigrationRule rule, URL url, MigrationRuleHandler<?> handler, MigrationClusterInvoker<?> invoker) {
        Assertions.assertEquals(MigrationStep.FORCE_INTERFACE, handler.getMigrationStep());

        Mockito.when(invoker.migrateToForceApplicationInvoker(Mockito.any())).thenReturn(false);

        Mockito.when(rule.getStep(url)).thenReturn(MigrationStep.FORCE_APPLICATION);
        handler.doMigrate(rule);
        Mockito.verify(invoker, Mockito.times(2)).migrateToForceApplicationInvoker(rule);
        Assertions.assertEquals(MigrationStep.FORCE_INTERFACE, handler.getMigrationStep());
    }

    private void testMigrationWithStepUnchanged(MigrationRule rule, URL url, MigrationRuleHandler<?> handler, MigrationClusterInvoker<?> invoker) {
        // set the same as
        Mockito.when(rule.getStep(url)).thenReturn(handler.getMigrationStep());
        handler.doMigrate(rule);
        // no interaction
        Mockito.verify(invoker, Mockito.times(1)).migrateToForceInterfaceInvoker(rule);
    }

}
