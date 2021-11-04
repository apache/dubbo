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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Cmd(name = "publishMetadata", summary = "update service metadata and service instance", example = {
        "publishMetadata",
        "publishMetadata 5"
})
public class  PublishMetadata implements BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(PublishMetadata.class);
    private final FrameworkModel frameworkModel;

    public PublishMetadata(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        logger.info("received publishMetadata command.");

        StringBuilder stringBuilder = new StringBuilder();
        List<ApplicationModel> applicationModels = frameworkModel.getApplicationModels();

        for (ApplicationModel applicationModel : applicationModels) {
            if (ArrayUtils.isEmpty(args)) {
                ServiceInstanceMetadataUtils.refreshMetadataAndInstance(applicationModel);
                stringBuilder.append("publish metadata succeeded. App:").append(applicationModel.getApplicationName()).append("\n");
            } else {
                try {
                    int delay = Integer.parseInt(args[0]);
                    ExecutorRepository executorRepository = applicationModel.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();
                    executorRepository.nextScheduledExecutor()
                        .schedule(() -> ServiceInstanceMetadataUtils.refreshMetadataAndInstance(applicationModel), delay, TimeUnit.SECONDS);
                } catch (NumberFormatException e) {
                    logger.error("Wrong delay param", e);
                    return "publishMetadata failed! Wrong delay param!";
                }
                stringBuilder.append("publish task submitted, will publish in ").append(args[0]).append(" seconds. App:").append(applicationModel.getApplicationName()).append("\n");
            }
        }
        return stringBuilder.toString();
    }

}
