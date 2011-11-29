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
import java.util.List;
import java.util.regex.Pattern;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.container.web.Page;
import com.alibaba.dubbo.container.web.PageHandler;
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
        String consumer = url.getParameter("consumer");
        String provider = url.getParameter("provider");
        String side = url.getParameter("side");
        if (side == null || side.length() == 0) {
            side = (provider != null && provider.length() > 0 ? "provider" : "consumer");
        }
        List<List<String>> rows = new ArrayList<List<String>>();
        String directory = SimpleMonitorService.getInstance().getDirectory();
        String filename = directory + "/" + service;
        File serviceDir = new File(filename);
        if (serviceDir.exists()) {
            File[] methodDirs = serviceDir.listFiles();
            for (File methodDir : methodDirs) {
                long success = 0;
                long failure = 0;
                long elapsed = 0;
                long maxElapsed = 0;
                long concurrent = 0;
                long maxConcurrent = 0;
                File[] consumerDirs = methodDir.listFiles();
                for (File consumerDir : consumerDirs) {
                    if (consumer == null || consumer.length() == 0
                            || consumerDir.getName().equals(consumer)) {
                        File[] providerDirs = consumerDir.listFiles();
                        for (File providerDir : providerDirs) {
                            if (provider == null || provider.length() == 0
                                    || providerDir.getName().equals(provider)) {
                                File dateDir = new File(providerDir, date);
                                success += countFile(new File(dateDir, side + "."
                                        + MonitorService.SUCCESS), SUM);
                                failure += countFile(new File(dateDir, side + "."
                                        + MonitorService.FAILURE), SUM);
                                elapsed += countFile(new File(dateDir, side + "."
                                        + MonitorService.ELAPSED), SUM);
                                maxElapsed = Math.max(
                                        maxElapsed,
                                        countFile(new File(dateDir, side + "."
                                                + MonitorService.MAX_ELAPSED), MAX));
                                concurrent += countFile(new File(dateDir, side + "."
                                        + MonitorService.CONCURRENT), AVG);
                                maxConcurrent = Math.max(
                                        maxConcurrent,
                                        countFile(new File(dateDir, side + "."
                                                + MonitorService.MAX_CONCURRENT), MAX));
                            }
                        }
                    }
                }
                List<String> row = new ArrayList<String>();
                row.add(methodDir.getName());
                row.add(String.valueOf(success));
                row.add(String.valueOf(failure));
                row.add(String.valueOf(success == 0 ? 0 : elapsed / success));
                row.add(String.valueOf(maxElapsed));
                row.add(String.valueOf(concurrent));
                row.add(String.valueOf(maxConcurrent));
                rows.add(row);
            }
        }
        StringBuilder nav = new StringBuilder();
        nav.append("<a href=\"services.html\">Services</a> &gt; ");
        nav.append(service);
        nav.append(" &gt; ");
        if (provider != null && provider.length() > 0) {
            nav.append("<a href=\"providers.html?service=");
            nav.append(service);
            nav.append("\">Providers</a> &gt; ");
            nav.append(provider);
            nav.append(" &gt; ");
        } else if (consumer != null && consumer.length() > 0) {
            nav.append("<a href=\"consumers.html?service=");
            nav.append(service);
            nav.append("\">Consumers</a> &gt; ");
            nav.append(consumer);
            nav.append(" &gt; ");
        } else {
            nav.append("<a href=\"providers.html?service=");
            nav.append(service);
            nav.append("\">Providers</a> | <a href=\"consumers.html?service=");
            nav.append(service);
            nav.append("\">Consumers</a> | ");
        }
        nav.append("Statistics: <input type=\"text\" name=\"date\" value=\"");
        nav.append(date);
        nav.append("\" onkeyup=\"if (event.keyCode == 10 || event.keyCode == 13) {window.location.href='statistics.html?service=");
        nav.append(service);
        if (provider != null && provider.length() > 0) {
            nav.append("&provider=");
            nav.append(provider);
        } else if (consumer != null && consumer.length() > 0) {
            nav.append("&consumer=");
            nav.append(consumer);
        }
        nav.append("&date=' + this.value;}\" />");
        return new Page(nav.toString(), "Statistics (" + rows.size() + ")",
                new String[] { "Method:", "Success", "Failure", "Avg Elapsed (ms)",
                        "Max Elapsed (ms)", "Avg Concurrent", "Max Concurrent" }, rows);
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
