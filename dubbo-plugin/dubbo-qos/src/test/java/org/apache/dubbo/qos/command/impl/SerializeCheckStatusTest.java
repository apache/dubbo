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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link SerializeSecurityManager}.
 */
class SerializeCheckStatusTest {

    private SerializeSecurityManager serializeSecurityManager;

    @BeforeEach
    void setUp() {
        // Initialize SerializeSecurityManager before each test
        serializeSecurityManager = new SerializeSecurityManager();
    }

    @Test
    void testSerializationSecurityManagement() {
        // Mock CommandContext instances
        CommandContext commandContext1 = Mockito.mock(CommandContext.class);
        Mockito.when(commandContext1.isHttp()).thenReturn(false);

        CommandContext commandContext2 = Mockito.mock(CommandContext.class);
        Mockito.when(commandContext2.isHttp()).thenReturn(true);

        // Test: Allowed Class
        Assertions.assertFalse(serializeSecurityManager.getAllowedPrefix().contains("Test1234"),
                "Class 'Test1234' should not be in the allowed list initially.");
        
        serializeSecurityManager.addToAllowed("Test1234");
        
        Assertions.assertTrue(serializeSecurityManager.getAllowedPrefix().contains("Test1234"),
                "Class 'Test1234' should be added to the allowed list.");

        // Test: Disallowed Class
        Assertions.assertFalse(serializeSecurityManager.getDisAllowedPrefix().contains("Test4321"),
                "Class 'Test4321' should not be in the disallowed list initially.");
        
        serializeSecurityManager.addToDisAllowed("Test4321");
        
        Assertions.assertTrue(serializeSecurityManager.getDisAllowedPrefix().contains("Test4321"),
                "Class 'Test4321' should be added to the disallowed list.");

        // Test: CheckSerializable Default Behavior
        Assertions.assertTrue(serializeSecurityManager.isCheckSerializable(),
                "CheckSerializable should be enabled by default.");
        
        serializeSecurityManager.setCheckSerializable(false);
        
        Assertions.assertFalse(serializeSecurityManager.isCheckSerializable(),
                "CheckSerializable should be disabled after calling setCheckSerializable(false).");

        // Test: CheckStatus Default & Update
        Assertions.assertNotEquals(SerializeCheckStatus.DISABLE, serializeSecurityManager.getCheckStatus(),
                "Default check status should not be DISABLE.");
        
        serializeSecurityManager.setCheckStatus(SerializeCheckStatus.DISABLE);
        
        Assertions.assertEquals(SerializeCheckStatus.DISABLE, serializeSecurityManager.getCheckStatus(),
                "CheckStatus should be updated to DISABLE.");
    }
}
