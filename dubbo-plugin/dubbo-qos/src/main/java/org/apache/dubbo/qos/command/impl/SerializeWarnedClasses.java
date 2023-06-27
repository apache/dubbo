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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.api.Cmd;
import org.apache.dubbo.qos.command.util.SerializeCheckUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

@Cmd(name = "serializeWarnedClasses",summary = "get serialize warned classes")
public class SerializeWarnedClasses implements BaseCommand {

    private final SerializeCheckUtils serializeCheckUtils;

    public SerializeWarnedClasses(FrameworkModel frameworkModel) {
        serializeCheckUtils = frameworkModel.getBeanFactory().getBean(SerializeCheckUtils.class);
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        if (commandContext.isHttp()) {
            Map<String, Object> result = new HashMap<>();
            result.put("warnedClasses", serializeCheckUtils.getWarnedClasses());
            return JsonUtils.toJson(result);
        } else {
            return "WarnedClasses: \n" +
                serializeCheckUtils.getWarnedClasses().stream().sorted().collect(Collectors.joining("\n")) +
                "\n\n";
        }
    }
}
