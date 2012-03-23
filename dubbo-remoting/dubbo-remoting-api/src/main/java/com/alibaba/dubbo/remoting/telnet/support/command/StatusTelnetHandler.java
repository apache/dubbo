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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.status.Status;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.common.status.support.StatusUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;
import com.alibaba.dubbo.remoting.telnet.support.Help;
import com.alibaba.dubbo.remoting.telnet.support.TelnetUtils;

/**
 * StatusTelnetHandler
 * 
 * @author william.liangf
 */
@Activate
@Help(parameter = "[-l]", summary = "Show status.", detail = "Show status.")
public class StatusTelnetHandler implements TelnetHandler {

    private final ExtensionLoader<StatusChecker> extensionLoader = ExtensionLoader.getExtensionLoader(StatusChecker.class);

    public String telnet(Channel channel, String message) {
        if (message.equals("-l")) {
            List<StatusChecker> checkers = extensionLoader.getActivateExtension(channel.getUrl(), "status");
            String[] header = new String[] {"resource", "status", "message"};
            List<List<String>> table = new ArrayList<List<String>>();
            Map<String, Status> statuses = new HashMap<String, Status>();
            if (checkers != null && checkers.size() > 0) {
                for (StatusChecker checker : checkers) {
                    String name = extensionLoader.getExtensionName(checker);
                    Status stat;
                    try {
                        stat = checker.check();
                    } catch (Throwable t) {
                        stat = new Status(Status.Level.ERROR, t.getMessage());
                    }
                    statuses.put(name, stat);
                    if (stat.getLevel() != null && stat.getLevel() != Status.Level.UNKNOWN) {
                        List<String> row = new ArrayList<String>();
                        row.add(name);
                        row.add(String.valueOf(stat.getLevel()));
                        row.add(stat.getMessage() == null ? "" : stat.getMessage());
                        table.add(row);
                    }
                }
            }
            Status stat= StatusUtils.getSummaryStatus(statuses);
            List<String> row = new ArrayList<String>();
            row.add("summary");
            row.add(String.valueOf(stat.getLevel()));
            row.add(stat.getMessage());
            table.add(row);
            return TelnetUtils.toTable(header, table);
        } else if (message.length() > 0) {
            return "Unsupported parameter " + message + " for status.";
        }
        String status = channel.getUrl().getParameter("status");
        Map<String, Status> statuses = new HashMap<String, Status>();
        if (status != null && status.length() > 0) {
            String[] ss = Constants.COMMA_SPLIT_PATTERN.split(status);
            for (String s : ss) {
                StatusChecker handler = extensionLoader.getExtension(s);
                Status stat;
                try {
                    stat = handler.check();
                } catch (Throwable t) {
                    stat = new Status(Status.Level.ERROR, t.getMessage());
                }
                statuses.put(s, stat);
            }
        }
        Status stat= StatusUtils.getSummaryStatus(statuses);
        return String.valueOf(stat.getLevel());
    }

}