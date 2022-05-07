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

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

@Cmd(name = "OnlineMetaData", summary = "online service metadata and service instance", example = {
    "online-metadata",
    "online-metadata 5"
})
public class OnlineMetadata implements BaseCommand {

    private static final Logger logger = LoggerFactory.getLogger(OnlineMetadata.class);
    private final FrameworkModel frameworkModel;

    public OnlineMetadata(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        logger.info("received onlineMetaData command.");

        StringBuilder stringBuilder = new StringBuilder();
        List<ApplicationModel> applicationModels = frameworkModel.getApplicationModels();

        for (ApplicationModel applicationModel : applicationModels) {
            if (ArrayUtils.isEmpty(args)) {
                ServiceInstanceMetadataUtils.registerMetadataAndInstance(applicationModel);
                stringBuilder.append("online metadata succeeded. App:").append(applicationModel.getApplicationName())
                    .append("\n");
            } else {
                try {
                    int delay = Integer.parseInt(args[0]);
                    FrameworkExecutorRepository frameworkExecutorRepository = applicationModel.getFrameworkModel()
                        .getBeanFactory().getBean(FrameworkExecutorRepository.class);
                    frameworkExecutorRepository.nextScheduledExecutor()
                        .schedule(() -> ServiceInstanceMetadataUtils.registerMetadataAndInstance(applicationModel),
                            delay, TimeUnit.SECONDS);
                } catch (NumberFormatException e) {
                    logger.error("Wrong delay param", e);
                    return "onlineMetadata failed! Wrong delay param!";
                }
                stringBuilder.append("online task submitted, will publish in ").append(args[0])
                    .append(" seconds. App:").append(applicationModel.getApplicationName()).append("\n");
            }
        }
        return stringBuilder.toString();
    }

}
