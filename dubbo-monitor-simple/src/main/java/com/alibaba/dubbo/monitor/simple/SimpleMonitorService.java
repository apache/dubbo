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
package com.alibaba.dubbo.monitor.simple;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.monitor.MonitorService;

/**
 * SimpleMonitorService
 * 
 * @author william.liangf
 */
public class SimpleMonitorService implements MonitorService {
    
    private static final String[] types = {SUCCESS, FAILURE, ELAPSED, CONCURRENT, MAX_ELAPSED, MAX_CONCURRENT};
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleMonitorService.class);

    private String directory;
    
    private static SimpleMonitorService INSTANCE = null;

    public static String[] getTypes() {
        return types;
    }

    public SimpleMonitorService() {
        INSTANCE = this;
    }
    
    public static SimpleMonitorService getInstance() {
        return INSTANCE;
    }

    public String getDirectory() {
        return directory;
    }
    
    public void setDirectory(String directory) {
        this.directory = directory;
    }
    
    public void count(URL statistics) {
        try {
            Date now = new Date();
            String day = new SimpleDateFormat("yyyyMMdd").format(now);
            SimpleDateFormat format = new SimpleDateFormat("HHmm");
            for (String key : types) {
                try {
                    String type;
                    String consumer;
                    String provider;
                    if (statistics.hasParameter(PROVIDER)) {
                        type = PROVIDER;
                        consumer = statistics.getHost();
                        provider = statistics.getParameter(PROVIDER);
                        int i = provider.indexOf(':');
                        if (i > 0) {
                            provider = provider.substring(0, i);
                        }
                    } else {
                        type = CONSUMER;
                        consumer = statistics.getParameter(CONSUMER);
                        provider = statistics.getHost();
                    }
                    String filename = directory + "/" + statistics.getServiceName() 
                            + "/" + statistics.getParameter(METHOD) 
                            + "/" + consumer 
                            + "/" + provider 
                            + "/" + day 
                            + "/" + type + "." + key;
                    File file = new File(filename);
                    File dir = file.getParentFile();
                    if (dir != null && ! dir.exists()) {
                        dir.mkdirs();
                    }
                    FileWriter writer = new FileWriter(file, true);
                    try {
                        writer.write(format.format(now) + " " + statistics.getParameter(key, 0) + "\n");
                        writer.flush();
                    } finally {
                        writer.close();
                    }
                } catch (Throwable t) {
                    logger.error(t.getMessage(), t);
                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

}