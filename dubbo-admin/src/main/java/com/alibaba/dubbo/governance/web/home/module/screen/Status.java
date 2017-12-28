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
package com.alibaba.dubbo.governance.web.home.module.screen;

import com.alibaba.dubbo.common.status.Status.Level;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.registry.common.StatusManager;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

/**
 */
public class Status {
    private static final Pattern OK_PATTERN = Pattern.compile("o(k)", Pattern.CASE_INSENSITIVE);
    @Autowired
    private HttpServletResponse response;

    public static String filterOK(String message) {
        if (message == null)
            return "";
        // Avoid the ok keyword, use the number 0 instead of the letter o
        return OK_PATTERN.matcher(message).replaceAll("0$1");
    }

    public void execute(Map<String, Object> context) throws Exception {
        //FIXME cache monitoring has bad performance, should be removed from summary page.
        Map<String, com.alibaba.dubbo.common.status.Status> statuses = StatusManager.getInstance().getStatusList(new String[]{"cache"});
        com.alibaba.dubbo.common.status.Status status = StatusManager.getInstance().getStatusSummary(statuses);
        Level level = status.getLevel();
        if (!com.alibaba.dubbo.common.status.Status.Level.OK.equals(level)) {
            context.put("message", level
                    + new SimpleDateFormat(" [yyyy-MM-dd HH:mm:ss] ").format(new Date())
                    + filterOK(status.getMessage()));
        } else {
            context.put("message", level.toString());
        }
        PrintWriter writer = response.getWriter();
        writer.print(context.get("message").toString());
        writer.flush();
    }

    public void setStatusHandlers(Collection<StatusChecker> statusHandlers) {
        StatusManager.getInstance().addStatusHandlers(statusHandlers);
    }

}
