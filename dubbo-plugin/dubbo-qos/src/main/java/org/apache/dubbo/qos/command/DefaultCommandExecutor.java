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
package org.apache.dubbo.qos.command;

import io.netty.channel.Channel;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.api.PermissionLevel;
import org.apache.dubbo.qos.api.Cmd;
import org.apache.dubbo.qos.command.exception.NoSuchCommandException;
import org.apache.dubbo.qos.command.exception.PermissionDenyException;
import org.apache.dubbo.qos.common.QosConstants;
import org.apache.dubbo.qos.permission.DefaultAnonymousAccessPermissionChecker;
import org.apache.dubbo.qos.permission.PermissionChecker;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class DefaultCommandExecutor implements CommandExecutor {
    private final static Logger logger = LoggerFactory.getLogger(DefaultCommandExecutor.class);
    private final FrameworkModel frameworkModel;

    public DefaultCommandExecutor(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public String execute(CommandContext commandContext) throws NoSuchCommandException, PermissionDenyException {
        String remoteAddress = Optional.ofNullable(commandContext.getRemote())
            .map(Channel::remoteAddress).map(Objects::toString).orElse("unknown");

        logger.info("[Dubbo QoS] Command Process start. Command: " + commandContext.getCommandName() +
            ", Args: " + Arrays.toString(commandContext.getArgs()) + ", Remote Address: " + remoteAddress);

        BaseCommand command = null;
        try {
            command = frameworkModel.getExtensionLoader(BaseCommand.class).getExtension(commandContext.getCommandName());
        } catch (Throwable throwable) {
            //can't find command
        }
        if (command == null) {
            logger.info("[Dubbo QoS] Command Not found. Command: " + commandContext.getCommandName() +
                ", Remote Address: " + remoteAddress);
            throw new NoSuchCommandException(commandContext.getCommandName());
        }

        // check permission when configs allow anonymous access
        if (commandContext.isAllowAnonymousAccess()) {
            PermissionChecker permissionChecker = DefaultAnonymousAccessPermissionChecker.INSTANCE;
            try {
                permissionChecker = frameworkModel.getExtensionLoader(PermissionChecker.class).getExtension(QosConstants.QOS_PERMISSION_CHECKER);
            } catch (Throwable throwable) {
                //can't find valid custom permissionChecker
            }

            final Cmd cmd = command.getClass().getAnnotation(Cmd.class);
            final PermissionLevel cmdRequiredPermissionLevel = cmd.requiredPermissionLevel();

            if (!permissionChecker.access(commandContext, cmdRequiredPermissionLevel)) {
                logger.info("[Dubbo QoS] Command Deny to access. Command: " + commandContext.getCommandName() +
                    ", Args: " + Arrays.toString(commandContext.getArgs()) + ", Required Permission Level: " + cmdRequiredPermissionLevel +
                    ", Remote Address: " + remoteAddress);
                throw new PermissionDenyException(commandContext.getCommandName());
            }
        }

        try {
            String result = command.execute(commandContext, commandContext.getArgs());
            if (command.logResult()) {
                logger.info("[Dubbo QoS] Command Process success. Command: " + commandContext.getCommandName() +
                    ", Args: " + Arrays.toString(commandContext.getArgs()) + ", Result: " + result +
                    ", Remote Address: " + remoteAddress);
            }
            return result;
        } catch (Throwable t) {
            logger.info("[Dubbo QoS] Command Process Failed. Command: " + commandContext.getCommandName() +
                ", Args: " + Arrays.toString(commandContext.getArgs()) +
                ", Remote Address: " + remoteAddress, t);
            throw t;
        }
    }
}
