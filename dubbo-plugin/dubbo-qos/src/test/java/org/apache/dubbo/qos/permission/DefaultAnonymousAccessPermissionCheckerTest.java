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
package org.apache.dubbo.qos.permission;

import io.netty.channel.Channel;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.api.PermissionLevel;
import org.apache.dubbo.qos.api.QosConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

class DefaultAnonymousAccessPermissionCheckerTest {
    @Test
    void testPermission() throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName("127.0.0.1");

        InetSocketAddress socketAddress = Mockito.mock(InetSocketAddress.class);
        Mockito.when(socketAddress.getAddress()).thenReturn(inetAddress);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(channel.remoteAddress()).thenReturn(socketAddress);
        CommandContext commandContext = Mockito.mock(CommandContext.class);
        Mockito.when(commandContext.getRemote()).thenReturn(channel);

        QosConfiguration qosConfiguration = Mockito.mock(QosConfiguration.class);
        Mockito.when(qosConfiguration.getAnonymousAccessPermissionLevel()).thenReturn(PermissionLevel.PUBLIC);
        Mockito.when(qosConfiguration.getAcceptForeignIpWhitelistPredicate()).thenReturn(ip -> false);

        Mockito.when(commandContext.getQosConfiguration()).thenReturn(qosConfiguration);

        DefaultAnonymousAccessPermissionChecker checker = new DefaultAnonymousAccessPermissionChecker();
        Assertions.assertTrue(checker.access(commandContext, PermissionLevel.NONE));
        Assertions.assertTrue(checker.access(commandContext, PermissionLevel.PUBLIC));
        Assertions.assertTrue(checker.access(commandContext, PermissionLevel.PROTECTED));
        Assertions.assertTrue(checker.access(commandContext, PermissionLevel.PRIVATE));

        inetAddress = InetAddress.getByName("1.1.1.1");
        Mockito.when(socketAddress.getAddress()).thenReturn(inetAddress);

        Assertions.assertTrue(checker.access(commandContext, PermissionLevel.NONE));
        Assertions.assertTrue(checker.access(commandContext, PermissionLevel.PUBLIC));
        Assertions.assertFalse(checker.access(commandContext, PermissionLevel.PROTECTED));
        Assertions.assertFalse(checker.access(commandContext, PermissionLevel.PRIVATE));

        Mockito.when(qosConfiguration.getAnonymousAccessPermissionLevel()).thenReturn(PermissionLevel.PROTECTED);

        Assertions.assertTrue(checker.access(commandContext, PermissionLevel.NONE));
        Assertions.assertTrue(checker.access(commandContext, PermissionLevel.PUBLIC));
        Assertions.assertTrue(checker.access(commandContext, PermissionLevel.PROTECTED));
        Assertions.assertFalse(checker.access(commandContext, PermissionLevel.PRIVATE));

        Mockito.when(qosConfiguration.getAnonymousAccessPermissionLevel()).thenReturn(PermissionLevel.NONE);

        Assertions.assertTrue(checker.access(commandContext, PermissionLevel.NONE));
        Assertions.assertFalse(checker.access(commandContext, PermissionLevel.PUBLIC));
        Assertions.assertFalse(checker.access(commandContext, PermissionLevel.PROTECTED));
        Assertions.assertFalse(checker.access(commandContext, PermissionLevel.PRIVATE));

        Mockito.when(qosConfiguration.getAcceptForeignIpWhitelistPredicate()).thenReturn(ip -> true);
        Assertions.assertTrue(checker.access(commandContext, PermissionLevel.NONE));
        Assertions.assertTrue(checker.access(commandContext, PermissionLevel.PUBLIC));
        Assertions.assertTrue(checker.access(commandContext, PermissionLevel.PROTECTED));
        Assertions.assertFalse(checker.access(commandContext, PermissionLevel.PRIVATE));
    }
}
