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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

public class DefaultAnonymousAccessPermissionChecker implements PermissionChecker {
    public static final DefaultAnonymousAccessPermissionChecker INSTANCE = new DefaultAnonymousAccessPermissionChecker();

    @Override
    public boolean access(CommandContext commandContext, PermissionLevel defaultCmdRequiredPermissionLevel) {
        final InetAddress inetAddress = Optional.ofNullable(commandContext.getRemote())
            .map(Channel::remoteAddress)
            .map(InetSocketAddress.class::cast)
            .map(InetSocketAddress::getAddress)
            .orElse(null);

        QosConfiguration qosConfiguration = commandContext.getQosConfiguration();
        PermissionLevel currentLevel = qosConfiguration.getAnonymousAccessPermissionLevel();

        // Local has private permission
        if (inetAddress != null && inetAddress.isLoopbackAddress()) {
            currentLevel = PermissionLevel.PRIVATE;
        } else if (inetAddress != null &&
            qosConfiguration.getAcceptForeignIpWhitelistPredicate()
                .test(inetAddress.getHostAddress())) {
            currentLevel = PermissionLevel.PROTECTED;
        }

        return currentLevel.getLevel() >= defaultCmdRequiredPermissionLevel.getLevel();
    }
}
