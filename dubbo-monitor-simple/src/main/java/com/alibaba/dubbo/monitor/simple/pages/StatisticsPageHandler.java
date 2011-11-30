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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.monitor.MonitorService;
import com.alibaba.dubbo.monitor.simple.SimpleMonitorService;

/**
 * StatisticsPageHandler
 * 
 * @author william.liangf
 */
@Extension("statistics")
public class StatisticsPageHandler implements PageHandler {

    private static final Logger logger = LoggerFactory.getLogger(SimpleMonitorService.class);

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
                        rows.add(toRow(" &nbsp;&nbsp;&nbsp;&nbsp; |-- " + entry.getKey(), entry.getValue()));
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
        nav.append("\">Consumers</a> | Statistics &gt; <input type=\"text\" style=\"width: 65px;\" name=\"date\" value=\"");
        nav.append(date);
        nav.append("\" onkeyup=\"if (event.keyCode == 10 || event.keyCode == 13) {window.location.href='statistics.html?service=");
        nav.append(service);
        if (expand != null && expand.length() > 0) {
            nav.append("&expand=");
            nav.append(expand);
        }
        nav.append("&date=' + this.value;}\" /> &gt; ");
        if (! MonitorService.PROVIDER.equals(expand) && ! MonitorService.CONSUMER.equals(expand)) {
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
                new String[] { "Method:", "Success", "Failure", "Avg Elapsed (ms)",
                        "Max Elapsed (ms)", "Avg Concurrent", "Max Concurrent" }, rows);
    }
    
    private long[] newStatistics() {
        return new long[12];
    }
    
    private void appendStatistics(File providerDir, long[] statistics) {
        statistics[0] += countFile(new File(providerDir, MonitorService.CONSUMER + "." + MonitorService.SUCCESS), SUM);
        statistics[1] += countFile(new File(providerDir, MonitorService.PROVIDER + "." + MonitorService.SUCCESS), SUM);
        statistics[2] += countFile(new File(providerDir, MonitorService.CONSUMER + "." + MonitorService.FAILURE), SUM);
        statistics[3] += countFile(new File(providerDir, MonitorService.PROVIDER + "." + MonitorService.FAILURE), SUM);
        statistics[4] += countFile(new File(providerDir, MonitorService.CONSUMER + "." + MonitorService.ELAPSED), SUM);
        statistics[5] += countFile(new File(providerDir, MonitorService.PROVIDER + "." + MonitorService.ELAPSED), SUM);
        statistics[6] = Math.max(statistics[6], countFile(new File(providerDir, MonitorService.CONSUMER + "." + MonitorService.MAX_ELAPSED), MAX));
        statistics[7] = Math.max(statistics[7], countFile(new File(providerDir, MonitorService.PROVIDER + "." + MonitorService.MAX_ELAPSED), MAX));
        statistics[8] += countFile(new File(providerDir, MonitorService.CONSUMER + "." + MonitorService.CONCURRENT), AVG);
        statistics[9] += countFile(new File(providerDir, MonitorService.PROVIDER + "." + MonitorService.CONCURRENT), AVG);
        statistics[10] = Math.max(statistics[10], countFile(new File(providerDir, MonitorService.CONSUMER + "." + MonitorService.MAX_CONCURRENT), MAX));
        statistics[11] = Math.max(statistics[11], countFile(new File(providerDir, MonitorService.PROVIDER + "." + MonitorService.MAX_CONCURRENT), MAX));
    }
    
    private List<String> toRow(String name, long[] statistics) {
        List<String> row = new ArrayList<String>();
        row.add(name);
        row.add(String.valueOf(statistics[0]) + " &gt;&gt; " + String.valueOf(statistics[1]));
        row.add(String.valueOf(statistics[2]) + " &gt;&gt; " + String.valueOf(statistics[3]));
        row.add(String.valueOf(statistics[0] == 0 ? 0 : statistics[4] / statistics[0]) 
                + " &gt;&gt; " + String.valueOf(statistics[1] == 0 ? 0 : statistics[5] / statistics[1]));
        row.add(String.valueOf(statistics[6]) + " &gt;&gt; " + String.valueOf(statistics[7]));
        row.add(String.valueOf(statistics[7]) + " &gt;&gt; " + String.valueOf(statistics[9]));
        row.add(String.valueOf(statistics[10]) + " &gt;&gt; " + String.valueOf(statistics[11]));
        return row;
    }

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    
    private static final int SUM = 0;
    
    private static final int MAX = 1;
    
    private static final int AVG = 2;
    
    private long countFile(File file, int op) {
        if (file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                try {
                    int times = 0;
                    int count = 0;
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int i = line.indexOf(" ");
                        if (i > 0) {
                            line = line.substring(i + 1).trim();
                            if (NUMBER_PATTERN.matcher(line).matches()) {
                                int value = Integer.parseInt(line);
                                times ++;
                                if (op == MAX) {
                                    count = Math.max(count, value);
                                } else {
                                    count += value;
                                }
                            }
                        }
                    }
                    if (op == AVG) {
                        return count / times;
                    }
                    return count;
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            }
        }
        return 0;
    }
    
}
