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

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.api.Cmd;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.ArrayList;
import java.util.List;

@Cmd(name = "shutdown", summary = "Shutdown Dubbo Application.", example = {
    "shutdown -t <milliseconds>"
})
public class ShutdownTelnet implements BaseCommand {

    private final FrameworkModel frameworkModel;

    public ShutdownTelnet(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {

        int sleepMilliseconds = 0;
        if (args != null && args.length > 0) {
            if (args.length == 2 && "-t".equals(args[0]) && StringUtils.isNumber(args[1])) {
                sleepMilliseconds = Integer.parseInt(args[1]);
            } else {
                return "Invalid parameter,please input like shutdown -t 10000";
            }
        }
        long start = System.currentTimeMillis();
        if (sleepMilliseconds > 0) {
            try {
                Thread.sleep(sleepMilliseconds);
            } catch (InterruptedException e) {
                return "Failed to invoke shutdown command, cause: " + e.getMessage();
            }
        }
        StringBuilder buf = new StringBuilder();
        List<ApplicationModel> applicationModels = frameworkModel.getApplicationModels();
        for (ApplicationModel applicationModel : new ArrayList<>(applicationModels)) {
            applicationModel.destroy();
        }
        // TODO change to ApplicationDeployer.destroy() or ApplicationModel.destroy()
//        DubboShutdownHook.getDubboShutdownHook().unregister();
//        DubboShutdownHook.getDubboShutdownHook().doDestroy();
        long end = System.currentTimeMillis();
        buf.append("Application has shutdown successfully");
        buf.append("\r\nelapsed: ");
        buf.append(end - start);
        buf.append(" ms.");
        return buf.toString();
    }
}
