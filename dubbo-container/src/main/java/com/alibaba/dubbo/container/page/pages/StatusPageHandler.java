/**
 * Project: dubbo.core.service.server-1.0.6-SNAPSHOT
 * 
 * File Created at 2010-1-19
 * $Id: StatusInformationProvider.java 34686 2010-01-19 07:38:33Z william.liangf $
 * 
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.container.page.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.status.Status;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.common.status.support.StatusUtils;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;

/**
 * StatusPageHandler
 * 
 * @author william.liangf
 */
@Extension("status")
public class StatusPageHandler implements PageHandler {

    public Page handle(URL url) {
        List<List<String>> rows = new ArrayList<List<String>>();
        Set<String> names = ExtensionLoader.getExtensionLoader(StatusChecker.class).getSupportedExtensions();
        Map<String, Status> statuses = new HashMap<String, Status>();
        for (String name : names) {
            StatusChecker checker = ExtensionLoader.getExtensionLoader(StatusChecker.class).getExtension(name);
            List<String> row = new ArrayList<String>();
            row.add(name);
            Status status = checker.check();
            statuses.put(name, status);
            row.add(getLevelHtml(status.getLevel()));
            row.add(status.getMessage());
            rows.add(row);
        }
        Status status = StatusUtils.getSummaryStatus(statuses);
        List<String> row = new ArrayList<String>();
        row.add("summary");
        row.add(getLevelHtml(status.getLevel()));
        row.add("<a href=\"/status\" target=\"_blank\">summary</a>");
        rows.add(row);
        return new Page("<a href=\"/\">Home</a> &gt; Status (<a href=\"/status\" target=\"_blank\">All</a>)", "Status", new String[] {"Name", "Status", "Description"}, rows);
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
