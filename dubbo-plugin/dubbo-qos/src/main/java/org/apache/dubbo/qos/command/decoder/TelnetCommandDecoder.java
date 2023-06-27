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
package org.apache.dubbo.qos.command.decoder;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.command.CommandContextFactory;


public class TelnetCommandDecoder {
    public static final CommandContext decode(String str) {
        CommandContext commandContext = null;
        if (!StringUtils.isBlank(str)) {
            str = str.trim();
            String[] array = str.split("(?<![\\\\]) ");
            if (array.length > 0) {
                String[] targetArgs = new String[array.length - 1];
                System.arraycopy(array, 1, targetArgs, 0, array.length - 1);
                String name = array[0].trim();
                if (name.equals("invoke") && array.length > 2) {
                    targetArgs = reBuildInvokeCmdArgs(str);
                }
                commandContext = CommandContextFactory.newInstance( name, targetArgs,false);
                commandContext.setOriginRequest(str);
            }
        }

        return commandContext;
    }

    private static String[] reBuildInvokeCmdArgs(String cmd) {
        return new String[] {cmd.substring(cmd.indexOf(" ") + 1).trim()};
    }

}
