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

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Cmd(name = "publishMetadata", summary = "update service metadata and service instance", example = {
        "publishMetadata",
        "publishMetadata 5"
})
public class PublishMetadata implements BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(PublishMetadata.class);
    private final ExecutorRepository executorRepository = ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();
    private ScheduledFuture future;

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        logger.info("received publishMetadata command.");

        if (ArrayUtils.isEmpty(args)) {
            ServiceInstanceMetadataUtils.refreshMetadataAndInstance();
            return "publish metadata succeeded.";
        }

        try {
            int delay = Integer.parseInt(args[0]);
            if (future == null || future.isDone() || future.isCancelled()) {
                future = executorRepository.nextScheduledExecutor()
                        .scheduleWithFixedDelay(ServiceInstanceMetadataUtils::refreshMetadataAndInstance, 0, delay, TimeUnit.MILLISECONDS);
            }
        } catch (NumberFormatException e) {
            logger.error("Wrong delay param", e);
            return "publishMetadata failed! Wrong delay param!";
        }
        return "publish task submitted, will publish in " + args[0] + " seconds.";
    }

}
