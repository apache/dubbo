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

import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotSwitcher;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;

@Cmd(name = "disableRouterSnapshot",
    summary = "Disable Dubbo Invocation Level Router Snapshot Print",
    example = "disableRouterSnapshot xx.xx.xxx.service")
public class DisableRouterSnapshot implements BaseCommand {
    private final RouterSnapshotSwitcher routerSnapshotSwitcher;
    private final FrameworkModel frameworkModel;

    public DisableRouterSnapshot(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        this.routerSnapshotSwitcher = frameworkModel.getBeanFactory().getBean(RouterSnapshotSwitcher.class);
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        if (args.length != 1) {
            return "args count should be 1. example disableRouterSnapshot xx.xx.xxx.service";
        }
        String servicePattern = args[0];
        int count = 0;
        for (ConsumerModel consumerModel : frameworkModel.getServiceRepository().allConsumerModels()) {
            try {
                ServiceMetadata metadata = consumerModel.getServiceMetadata();
                if (metadata.getServiceKey().matches(servicePattern) || metadata.getDisplayServiceKey().matches(servicePattern)) {
                    routerSnapshotSwitcher.removeEnabledService(metadata.getServiceKey());
                    count += 1;
                }
            } catch (Throwable ignore) {

            }
        }
        return "OK. Found service count: " + count;
    }
}
