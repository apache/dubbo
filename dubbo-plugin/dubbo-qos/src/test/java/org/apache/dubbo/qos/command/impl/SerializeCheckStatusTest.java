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

import org.apache.dubbo.common.utils.SerializeSecurityManager;
import org.apache.dubbo.common.utils.SerializeCheckStatus;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SerializeCheckStatusTest {

    private SerializeSecurityManager ssm;

    @BeforeEach
    void setUp() {
        // Initialize SerializeSecurityManager
        ssm = new SerializeSecurityManager();
    }

    @Test
    void testNotify() {
        // Mocking CommandContext
        CommandContext commandContext1 = Mockito.mock(CommandContext.class);
        Mockito.when(commandContext1.isHttp()).thenReturn(false);
        CommandContext commandContext2 = Mockito.mock(CommandContext.class);
        Mockito.when(commandContext2.isHttp()).thenReturn(true);

        // Testing Allowed Class
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("Test1234"));
        ssm.addToAllowed("Test1234");
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("Test1234"));

        // Testing Disallowed Class
        Assertions.assertFalse(ssm.getDisAllowedPrefix().contains("Test4321"));
        ssm.addToDisAllowed("Test4321");
        Assertions.assertTrue(ssm.getDisAllowedPrefix().contains("Test4321"));

        // Testing CheckSerializable
        Assertions.assertTrue(ssm.isCheckSerializable());
        ssm.setCheckSerializable(false);
        Assertions.assertFalse(ssm.isCheckSerializable());

        // Testing CheckStatus
        Assertions.assertNotEquals(SerializeCheckStatus.DISABLE, ssm.getCheckStatus());
        ssm.setCheckStatus(SerializeCheckStatus.DISABLE);
        Assertions.assertEquals(SerializeCheckStatus.DISABLE, ssm.getCheckStatus());
    }
}
