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
import org.apache.dubbo.common.profiler.ProfilerSwitch;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;

@Cmd(name = "enableDetailProfiler", summary = "Enable Dubbo Invocation Profiler.")
public class EnableDetailProfiler implements BaseCommand {
    private final static Logger logger = LoggerFactory.getLogger(EnableDetailProfiler.class);

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        ProfilerSwitch.enableDetailProfiler();
        logger.warn("Dubbo Invocation Profiler has been enabled.");
        return "OK. This will cause performance degradation, please be careful!";
    }
}
