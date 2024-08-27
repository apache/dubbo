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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.QOS_UNEXPECTED_EXCEPTION;

public class ActuatorCommandExecutor implements ActuatorExecutor {
    private static final Logger log = LoggerFactory.getLogger(ActuatorCommandExecutor.class);
    private final ApplicationModel applicationModel;

    public ActuatorCommandExecutor(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    @Override
    public String execute(String commandName, String[] parameters) {
        CommandContext commandContext;

        if (parameters == null || parameters.length == 0) {
            commandContext = CommandContextFactory.newInstance(commandName);
            commandContext.setHttp(true);
        } else {
            commandContext = CommandContextFactory.newInstance(commandName, parameters, true);
        }

        BaseCommand command;
        try {
            command = applicationModel
                    .getExtensionLoader(BaseCommand.class)
                    .getExtension(commandContext.getCommandName());
            return command.execute(commandContext, commandContext.getArgs());
        } catch (Exception qosEx) {
            log.error(
                    QOS_UNEXPECTED_EXCEPTION,
                    "",
                    "",
                    "execute commandContext: " + commandContext + " got exception",
                    qosEx);
            return qosEx.getMessage();
        }
    }
}
