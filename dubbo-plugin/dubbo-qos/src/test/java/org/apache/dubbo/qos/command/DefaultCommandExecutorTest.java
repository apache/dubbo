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

import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class DefaultCommandExecutorTest {
    @Test
    public void testExecute1() throws Exception {
        Assertions.assertThrows(NoSuchCommandException.class, () -> {
            DefaultCommandExecutor executor = new DefaultCommandExecutor(FrameworkModel.defaultModel());
            executor.execute(CommandContextFactory.newInstance("not-exit"));
        });
    }

    @Test
    public void testExecute2() throws Exception {
        DefaultCommandExecutor executor = new DefaultCommandExecutor(FrameworkModel.defaultModel());
        String result = executor.execute(CommandContextFactory.newInstance("greeting", new String[]{"dubbo"}, false));
        assertThat(result, equalTo("greeting dubbo"));
    }
}
