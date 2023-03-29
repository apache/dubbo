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

import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.api.Cmd;

import java.util.Locale;

@Cmd(name = "switchLogLevel", summary = "Switch log level", example = {
    "switchLogLevel info"
})
public class SwitchLogLevel implements BaseCommand {
    @Override
    public String execute(CommandContext commandContext, String[] args) {
        if (args.length != 1) {
            return "Unexpected argument length.";
        }
        Level level;
        switch (args[0]) {
            case "0":
                level = Level.ALL;
                break;
            case "1":
                level = Level.TRACE;
                break;
            case "2":
                level = Level.DEBUG;
                break;
            case "3":
                level = Level.INFO;
                break;
            case "4":
                level = Level.WARN;
                break;
            case "5":
                level = Level.ERROR;
                break;
            case "6":
                level = Level.OFF;
                break;
            default:
                level = Level.valueOf(args[0].toUpperCase(Locale.ROOT));
                break;
        }
        LoggerFactory.setLevel(level);
        return "OK";
    }
}
