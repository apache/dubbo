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

import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.qos.common.QosConstants;
import org.apache.dubbo.qos.permission.PermissionLevel;
import org.apache.dubbo.qos.command.exception.NoSuchCommandException;
import org.apache.dubbo.qos.command.exception.PermissionDenyException;
import org.apache.dubbo.qos.permission.DefaultAnonymousAccessPermissionChecker;
import org.apache.dubbo.qos.permission.PermissionChecker;
import org.apache.dubbo.rpc.model.FrameworkModel;

public class DefaultCommandExecutor implements CommandExecutor {
    private final FrameworkModel frameworkModel;

    public DefaultCommandExecutor(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public String execute(CommandContext commandContext) throws NoSuchCommandException, PermissionDenyException {
        BaseCommand command = null;
        try {
            command = frameworkModel.getExtensionLoader(BaseCommand.class).getExtension(commandContext.getCommandName());
        } catch (Throwable throwable) {
                //can't find command
        }
        if (command == null) {
            throw new NoSuchCommandException(commandContext.getCommandName());
        }

        // check permission when configs allow anonymous access
        if (commandContext.isAllowAnonymousAccess()) {
            PermissionChecker permissionChecker = DefaultAnonymousAccessPermissionChecker.INSTANCE;
            try {
                permissionChecker = frameworkModel.getExtensionLoader(PermissionChecker.class).getExtension(QosConstants.QOS_PERMISSION_CHECKER);
            } catch (Throwable throwable) {
                //can't find custom permissionChecker
            }

            final Cmd cmd = command.getClass().getAnnotation(Cmd.class);
            final PermissionLevel cmdRequiredPermissionLevel = cmd.requiredPermissionLevel();

            if (!permissionChecker.access(commandContext, cmdRequiredPermissionLevel)) {
                throw new PermissionDenyException(commandContext.getCommandName());
            }
        }

        return command.execute(commandContext, commandContext.getArgs());
    }
}
