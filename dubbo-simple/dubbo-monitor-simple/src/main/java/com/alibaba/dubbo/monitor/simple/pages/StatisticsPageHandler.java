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
import com.alibaba.dubbo.monitor.simple.CountUtils;
import com.alibaba.dubbo.monitor.simple.SimpleMonitorService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StatisticsPageHandler
 *
 * @author william.liangf
 */
public class StatisticsPageHandler implements PageHandler {

    public Page handle(URL url) {
        String service = url.getParameter("service");
        if (service == null || service.length() == 0) {
            throw new IllegalArgumentException("Please input service parameter.");
        }
        String date = url.getParameter("date");
        if (date == null || date.length() == 0) {
            date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        }
        String expand = url.getParameter("expand");
        List<List<String>> rows = new ArrayList<List<String>>();
        String directory = SimpleMonitorService.getInstance().getStatisticsDirectory();
        String filename = directory + "/" + date + "/" + service;
        File serviceDir = new File(filename);
        if (serviceDir.exists()) {
            File[] methodDirs = serviceDir.listFiles();
            for (File methodDir : methodDirs) {
                long[] statistics = newStatistics();
                Map<String, long[]> expandMap = new HashMap<String, long[]>();
                File[] consumerDirs = methodDir.listFiles();
                for (File consumerDir : consumerDirs) {
                    long[] expandStatistics = null;
                    if (MonitorService.CONSUMER.equals(expand)) {
                        expandStatistics = newStatistics();
                        expandMap.put(consumerDir.getName(), expandStatistics);
                    }
                    File[] providerDirs = consumerDir.listFiles();
                    for (File providerDir : providerDirs) {
                        if (MonitorService.PROVIDER.equals(expand)) {
                            expandStatistics = newStatistics();
                            expandMap.put(providerDir.getName(), expandStatistics);
                        }
                        appendStatistics(providerDir, statistics);
                        if (expandStatistics != null) {
                            appendStatistics(providerDir, expandStatistics);
                        }
                    }
                }
                rows.add(toRow(methodDir.getName(), statistics));
                if (expandMap != null && expandMap.size() > 0) {
                    for (Map.Entry<String, long[]> entry : expandMap.entrySet()) {
                        String node = MonitorService.CONSUMER.equals(expand) ? "&lt;--" : "--&gt;";
                        rows.add(toRow(" &nbsp;&nbsp;&nbsp;&nbsp; |" + node + " " + entry.getKey(), entry.getValue()));
                    }
                }
            }
        }
        StringBuilder nav = new StringBuilder();
        nav.append("<a href=\"services.html\">Services</a> &gt; ");
        nav.append(service);
        nav.append(" &gt; <a href=\"providers.html?service=");
        nav.append(service);
        nav.append("\">Providers</a> | <a href=\"consumers.html?service=");
        nav.append(service);
        nav.append("\">Consumers</a> | Statistics | <a href=\"charts.html?service=");
        nav.append(service);
        nav.append("&date=");
        nav.append(date);
        nav.append("\">Charts</a> &gt; <input type=\"text\" style=\"width: 65px;\" name=\"date\" value=\"");
        nav.append(date);
        nav.append("\" onkeyup=\"if (event.keyCode == 10 || event.keyCode == 13) {window.location.href='statistics.html?service=");
        nav.append(service);
        if (expand != null && expand.length() > 0) {
            nav.append("&expand=");
            nav.append(expand);
        }
        nav.append("&date=' + this.value;}\" /> &gt; ");
        if (!MonitorService.PROVIDER.equals(expand) && !MonitorService.CONSUMER.equals(expand)) {
            nav.append("Summary");
        } else {
            nav.append("<a href=\"statistics.html?service=");
            nav.append(service);
            nav.append("&date=");
            nav.append(date);
            nav.append("\">Summary</a>");
        }
        if (MonitorService.PROVIDER.equals(expand)) {
            nav.append(" | +Provider");
        } else {
            nav.append(" | <a href=\"statistics.html?service=");
            nav.append(service);
            nav.append("&date=");
            nav.append(date);
            nav.append("&expand=provider\">+Provider</a>");
        }
        if (MonitorService.CONSUMER.equals(expand)) {
            nav.append(" | +Consumer");
        } else {
            nav.append(" | <a href=\"statistics.html?service=");
            nav.append(service);
            nav.append("&date=");
            nav.append(date);
            nav.append("&expand=consumer\">+Consumer</a>");
        }
        return new Page(nav.toString(), "Statistics (" + rows.size() + ")",
                new String[]{"Method:", "Success", "Failure", "Avg Elapsed (ms)",
                        "Max Elapsed (ms)", "Max Concurrent"}, rows);
    }

    private long[] newStatistics() {
        return new long[10];
    }

    private void appendStatistics(File providerDir, long[] statistics) {
        statistics[0] += CountUtils.sum(new File(providerDir, MonitorService.CONSUMER + "." + MonitorService.SUCCESS));
        statistics[1] += CountUtils.sum(new File(providerDir, MonitorService.PROVIDER + "." + MonitorService.SUCCESS));
        statistics[2] += CountUtils.sum(new File(providerDir, MonitorService.CONSUMER + "." + MonitorService.FAILURE));
        statistics[3] += CountUtils.sum(new File(providerDir, MonitorService.PROVIDER + "." + MonitorService.FAILURE));
        statistics[4] += CountUtils.sum(new File(providerDir, MonitorService.CONSUMER + "." + MonitorService.ELAPSED));
        statistics[5] += CountUtils.sum(new File(providerDir, MonitorService.PROVIDER + "." + MonitorService.ELAPSED));
        statistics[6] = Math.max(statistics[6], CountUtils.max(new File(providerDir, MonitorService.CONSUMER + "." + MonitorService.MAX_ELAPSED)));
        statistics[7] = Math.max(statistics[7], CountUtils.max(new File(providerDir, MonitorService.PROVIDER + "." + MonitorService.MAX_ELAPSED)));
        statistics[8] = Math.max(statistics[8], CountUtils.max(new File(providerDir, MonitorService.CONSUMER + "." + MonitorService.MAX_CONCURRENT)));
        statistics[9] = Math.max(statistics[9], CountUtils.max(new File(providerDir, MonitorService.PROVIDER + "." + MonitorService.MAX_CONCURRENT)));
    }

    private List<String> toRow(String name, long[] statistics) {
        List<String> row = new ArrayList<String>();
        row.add(name);
        row.add(String.valueOf(statistics[0]) + " --&gt; " + String.valueOf(statistics[1]));
        row.add(String.valueOf(statistics[2]) + " --&gt; " + String.valueOf(statistics[3]));
        row.add(String.valueOf(statistics[0] == 0 ? 0 : statistics[4] / statistics[0])
                + " --&gt; " + String.valueOf(statistics[1] == 0 ? 0 : statistics[5] / statistics[1]));
        row.add(String.valueOf(statistics[6]) + " --&gt; " + String.valueOf(statistics[7]));
        row.add(String.valueOf(statistics[8]) + " --&gt; " + String.valueOf(statistics[9]));
        return row;
    }

}