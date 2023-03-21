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
package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LiveTest {
    private FrameworkModel frameworkModel;

    @BeforeEach
    public void setUp() {
        frameworkModel = new FrameworkModel();
    }

    @AfterEach
    public void reset() {
        frameworkModel.destroy();
    }

    @Test
    void testExecute() {
        Live live = new Live(frameworkModel);
        CommandContext commandContext = new CommandContext("live");
        String result = live.execute(commandContext, new String[0]);
        Assertions.assertEquals(result, "false");
        Assertions.assertEquals(commandContext.getHttpCode(), 503);

        MockLivenessProbe.setCheckReturnValue(true);
        result = live.execute(commandContext, new String[0]);
        Assertions.assertEquals(result, "true");
        Assertions.assertEquals(commandContext.getHttpCode(), 200);
    }
}
