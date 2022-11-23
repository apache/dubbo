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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.profiler.ProfilerSwitch;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.QOS_PROFILER_DISABLED;

@Cmd(name = "disableDetailProfiler", summary = "Disable Dubbo Invocation Profiler.")
public class DisableDetailProfiler implements BaseCommand {
    private final static ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DisableDetailProfiler.class);

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        ProfilerSwitch.disableDetailProfiler();
        logger.warn(QOS_PROFILER_DISABLED, "", "", "Dubbo Invocation Profiler has been disabled.");
        return "OK";
    }
}
