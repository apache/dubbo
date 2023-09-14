/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.qos.command;

import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.api.PermissionLevel;
import org.apache.dubbo.qos.api.QosConfiguration;
import org.apache.dubbo.qos.command.exception.NoSuchCommandException;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class DefaultCommandExecutorTest {
    @Test
    void testExecute1() {
        Assertions.assertThrows(NoSuchCommandException.class, () -> {
            DefaultCommandExecutor executor = new DefaultCommandExecutor(FrameworkModel.defaultModel());
            executor.execute(CommandContextFactory.newInstance("not-exit"));
        });
    }

    @Test
    void testExecute2() throws Exception {
        DefaultCommandExecutor executor = new DefaultCommandExecutor(FrameworkModel.defaultModel());
        final CommandContext commandContext = CommandContextFactory.newInstance("greeting", new String[]{"dubbo"}, false);
        commandContext.setQosConfiguration(QosConfiguration.builder()
            .anonymousAccessPermissionLevel(PermissionLevel.PROTECTED.name())
            .build());
        String result = executor.execute(commandContext);
        assertThat(result, equalTo("greeting dubbo"));
    }

    @Test
    void shouldNotThrowPermissionDenyException_GivenPermissionConfigAndMatchDefaultPUBLICCmdPermissionLevel() {
        DefaultCommandExecutor executor = new DefaultCommandExecutor(FrameworkModel.defaultModel());
        final CommandContext commandContext = CommandContextFactory.newInstance("live", new String[]{"dubbo"}, false);
        commandContext.setQosConfiguration(QosConfiguration.builder().build());
        Assertions.assertDoesNotThrow(() -> executor.execute(commandContext));
    }

    @Test
    void shouldNotThrowPermissionDenyException_GivenPermissionConfigAndNotMatchCmdPermissionLevel() {
        DefaultCommandExecutor executor = new DefaultCommandExecutor(FrameworkModel.defaultModel());
        final CommandContext commandContext = CommandContextFactory.newInstance("live", new String[]{"dubbo"}, false);
        // 1 PROTECTED
        commandContext.setQosConfiguration(QosConfiguration.builder().anonymousAccessPermissionLevel("1").build());
        Assertions.assertDoesNotThrow(() -> executor.execute(commandContext));
    }
}
