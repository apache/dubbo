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
    
    private static final String[] keys = {SUCCESS, FAILURE, ELAPSED, INPUT, OUTPUT, CONCURRENT, MAX_ELAPSED, MAX_INPUT, MAX_OUTPUT, MAX_CONCURRENT};
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleMonitorService.class);

    private File directory;

    public void setDirectory(String directory) {
        this.directory = new File(directory);
        if (! this.directory.exists()) {
            this.directory.mkdirs();
        }
    }

    public void count(URL statistics) {
        try {
            Date now = new Date();
            File day = new File(directory, new SimpleDateFormat("yyyyMMdd").format(now) 
                    + "/" + statistics.getServiceName() 
                    + "/" + statistics.getParameter(METHOD));
            if (! day.exists()) {
                day.mkdirs();
            }
            SimpleDateFormat format = new SimpleDateFormat("HHmm");
            for (String key : keys) {
                try {
                    String filename;
                    if (statistics.hasParameter(SERVER)) {
                        filename = statistics.getHost() + "-" + statistics.getParameter(SERVER) + "." + key + ".log";
                    } else {
                        filename = statistics.getParameter(CLIENT) + "+" + statistics.getHost() + "." + key + ".log";
                    }
                    File file = new File(day, filename);
                    FileWriter writer = new FileWriter(file, true);
                    try {
                        writer.write(format.format(now) + " " + statistics.getParameter(key, 0));
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