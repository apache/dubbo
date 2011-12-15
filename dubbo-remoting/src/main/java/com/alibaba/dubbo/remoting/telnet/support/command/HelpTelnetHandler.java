/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.telnet.support.command;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;
import com.alibaba.dubbo.remoting.telnet.support.Help;
import com.alibaba.dubbo.remoting.telnet.support.TelnetUtils;

/**
 * HelpTelnetHandler
 * 
 * @author william.liangf
 */
@Help(parameter = "[command]", summary = "Show help.", detail = "Show help.")
@Extension("help")
public class HelpTelnetHandler implements TelnetHandler {

    public String telnet(Channel channel, String message) {
        if (message.length() > 0) {
            if (! ExtensionLoader.getExtensionLoader(TelnetHandler.class).hasExtension(message)) {
                return "No such command " + message;
            }
            TelnetHandler handler = ExtensionLoader.getExtensionLoader(TelnetHandler.class).getExtension(message);
            Help help = handler.getClass().getAnnotation(Help.class);
            StringBuilder buf = new StringBuilder();
            buf.append("Command:\r\n    ");
            buf.append(message + " " + help.parameter().replace("\r\n", " ").replace("\n", " "));
            buf.append("\r\nSummary:\r\n    ");
            buf.append(help.summary().replace("\r\n", " ").replace("\n", " "));
            buf.append("\r\nDetail:\r\n    ");
            buf.append(help.detail().replace("\r\n", "    \r\n").replace("\n", "    \n"));
            return buf.toString();
        } else {
            List<List<String>> table = new ArrayList<List<String>>();
            String telnet = channel.getUrl().getParameter("telnet");
            List<String> cmds = ConfigUtils.mergeValues(TelnetHandler.class, telnet, Constants.DEFAULT_TELNET_COMMANDS);
            for (String cmd : cmds) {
                TelnetHandler handler = ExtensionLoader.getExtensionLoader(TelnetHandler.class).getExtension(cmd);
                Help help = handler.getClass().getAnnotation(Help.class);
                List<String> row = new ArrayList<String>();
                String parameter = " " + cmd + " " + (help != null ? help.parameter().replace("\r\n", " ").replace("\n", " ") : "");
                row.add(parameter.length() > 50 ? parameter.substring(0, 50) + "..." : parameter);
                String summary = help != null ? help.summary().replace("\r\n", " ").replace("\n", " ") : "";
                row.add(summary.length() > 50 ? summary.substring(0, 50) + "..." : summary);
                table.add(row);
            }
            return "Please input \"help [command]\" show detail.\r\n" + TelnetUtils.toList(table);
        }
    }

}