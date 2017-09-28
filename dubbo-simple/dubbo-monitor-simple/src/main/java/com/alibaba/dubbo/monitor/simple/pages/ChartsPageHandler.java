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
package com.alibaba.dubbo.monitor.simple.pages;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.monitor.MonitorService;
import com.alibaba.dubbo.monitor.simple.SimpleMonitorService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ChartsPageHandler
 *
 * @author william.liangf
 */
public class ChartsPageHandler implements PageHandler {

    public Page handle(URL url) {
        String service = url.getParameter("service");
        if (service == null || service.length() == 0) {
            throw new IllegalArgumentException("Please input service parameter.");
        }
        String date = url.getParameter("date");
        if (date == null || date.length() == 0) {
            date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        }
        List<List<String>> rows = new ArrayList<List<String>>();
        String directory = SimpleMonitorService.getInstance().getChartsDirectory();
        File chartsDir = new File(directory);
        String filename = directory + "/" + date + "/" + service;
        File serviceDir = new File(filename);
        if (serviceDir.exists()) {
            File[] methodDirs = serviceDir.listFiles();
            for (File methodDir : methodDirs) {
                String methodUri = chartsDir.getName() + "/" + date + "/" + service + "/" + methodDir.getName() + "/";
                rows.add(toRow(methodDir, methodUri));
            }
        }
        StringBuilder nav = new StringBuilder();
        nav.append("<a href=\"services.html\">Services</a> &gt; ");
        nav.append(service);
        nav.append(" &gt; <a href=\"providers.html?service=");
        nav.append(service);
        nav.append("\">Providers</a> | <a href=\"consumers.html?service=");
        nav.append(service);
        nav.append("\">Consumers</a> | <a href=\"statistics.html?service=");
        nav.append(service);
        nav.append("&date=");
        nav.append(date);
        nav.append("\">Statistics</a> | Charts &gt; <input type=\"text\" style=\"width: 65px;\" name=\"date\" value=\"");
        nav.append(date);
        nav.append("\" onkeyup=\"if (event.keyCode == 10 || event.keyCode == 13) {window.location.href='charts.html?service=");
        nav.append(service);
        nav.append("&date=' + this.value;}\" />");
        return new Page(nav.toString(), "Charts (" + rows.size() + ")",
                new String[]{"Method", "Requests per second (QPS)", "Average response time (ms)"}, rows);
    }

    private List<String> toRow(File dir, String uri) {
        List<String> row = new ArrayList<String>();
        row.add(dir.getName());
        if (new File(dir, MonitorService.SUCCESS + ".png").exists()) {
            String url = uri + MonitorService.SUCCESS + ".png";
            row.add("<a href=\"" + url + "\" target=\"_blank\"><img src=\"" + url + "\" style=\"width: 100%;\" border=\"0\" /></a>");
        } else {
            row.add("");
        }
        if (new File(dir, MonitorService.ELAPSED + ".png").exists()) {
            String url = uri + MonitorService.ELAPSED + ".png";
            row.add("<a href=\"" + url + "\" target=\"_blank\"><img src=\"" + url + "\" style=\"width: 100%;\" border=\"0\" /></a>");
        } else {
            row.add("");
        }
        return row;
    }

}