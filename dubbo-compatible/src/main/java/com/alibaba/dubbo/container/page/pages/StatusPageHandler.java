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
package com.alibaba.dubbo.container.page.pages;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.status.Status;
import org.apache.dubbo.common.status.support.StatusUtils;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.container.page.Menu;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * StatusPageHandler
 */
@Menu(name = "Status", desc = "Show system status.", order = Integer.MAX_VALUE - 12000)
public class StatusPageHandler implements PageHandler {

    @Override
    public Page handle(URL url) {
        List<List<String>> rows = new ArrayList<List<String>>();
        Set<String> names = ExtensionLoader.getExtensionLoader(StatusChecker.class).getSupportedExtensions();
        Map<String, Status> statuses = new HashMap<String, Status>();
        for (String name : names) {
            StatusChecker checker = ExtensionLoader.getExtensionLoader(StatusChecker.class).getExtension(name);
            List<String> row = new ArrayList<String>();
            row.add(name);
            Status status = checker.check();
            if (status != null && !Status.Level.UNKNOWN.equals(status.getLevel())) {
                statuses.put(name, status);
                row.add(getLevelHtml(status.getLevel()));
                row.add(status.getMessage());
                rows.add(row);
            }
        }
        Status status = StatusUtils.getSummaryStatus(statuses);
        if ("status".equals(url.getPath())) {
            return new Page("", "", "", status.getLevel().toString());
        } else {
            List<String> row = new ArrayList<String>();
            row.add("summary");
            row.add(getLevelHtml(status.getLevel()));
            row.add("<a href=\"/status\" target=\"_blank\">summary</a>");
            rows.add(row);
            return new Page("Status (<a href=\"/status\" target=\"_blank\">summary</a>)", "Status", new String[]{"Name", "Status", "Description"}, rows);
        }
    }

    private String getLevelHtml(Status.Level level) {
        return "<font color=\"" + getLevelColor(level) + "\">" + level.name() + "</font>";
    }

    private String getLevelColor(Status.Level level) {
        if (level == Status.Level.OK) {
            return "green";
        } else if (level == Status.Level.ERROR) {
            return "red";
        } else if (level == Status.Level.WARN) {
            return "yellow";
        }
        return "gray";
    }

}
