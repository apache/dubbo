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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.qos.probe.ReadinessProbe;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.List;

@Cmd(name = "ready", summary = "Judge if service is ready to work? ")
public class Ready implements BaseCommand {

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        URL url = URL.valueOf("application://")
                .addParameter(CommonConstants.QOS_READY_PROBE_EXTENSION, ApplicationModel.defaultModel().getCurrentConfig().getReadinessProbe());
        List<ReadinessProbe> readinessProbes = ExtensionLoader.getExtensionLoader(ReadinessProbe.class)
                .getActivateExtension(url, CommonConstants.QOS_READY_PROBE_EXTENSION);
        if (!readinessProbes.isEmpty()) {
            for (ReadinessProbe readinessProbe : readinessProbes) {
                if (!readinessProbe.check()) {
                    // 503 Service Unavailable
                    commandContext.setHttpCode(503);
                    return "false";
                }
            }
        }
        // 200 OK
        commandContext.setHttpCode(200);
        return "true";
    }

}
