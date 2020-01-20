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
package org.apache.dubbo.container.log4j;

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.container.Container;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;

import java.util.Enumeration;
import java.util.Properties;

/**
 * Log4jContainer. (SPI, Singleton, ThreadSafe)
 *
 * The container class implementation for Log4j
 */
public class Log4jContainer implements Container {

    public static final String LOG4J_FILE = "dubbo.log4j.file";

    public static final String LOG4J_LEVEL = "dubbo.log4j.level";

    public static final String LOG4J_SUBDIRECTORY = "dubbo.log4j.subdirectory";

    public static final String DEFAULT_LOG4J_LEVEL = "ERROR";

    @Override
    @SuppressWarnings("unchecked")
    public void start() {
        String file = ConfigurationUtils.getProperty(LOG4J_FILE);
        if (file != null && file.length() > 0) {
            String level = ConfigurationUtils.getProperty(LOG4J_LEVEL);
            if (StringUtils.isEmpty(level)) {
                level = DEFAULT_LOG4J_LEVEL;
            }
            Properties properties = new Properties();
            properties.setProperty("log4j.rootLogger", level + ",application");
            properties.setProperty("log4j.appender.application", "org.apache.log4j.DailyRollingFileAppender");
            properties.setProperty("log4j.appender.application.File", file);
            properties.setProperty("log4j.appender.application.Append", "true");
            properties.setProperty("log4j.appender.application.DatePattern", "'.'yyyy-MM-dd");
            properties.setProperty("log4j.appender.application.layout", "org.apache.log4j.PatternLayout");
            properties.setProperty("log4j.appender.application.layout.ConversionPattern", "%d [%t] %-5p %C{6} (%F:%L) - %m%n");
            PropertyConfigurator.configure(properties);
        }
        String subdirectory = ConfigurationUtils.getProperty(LOG4J_SUBDIRECTORY);
        if (subdirectory != null && subdirectory.length() > 0) {
            Enumeration<org.apache.log4j.Logger> ls = LogManager.getCurrentLoggers();
            while (ls.hasMoreElements()) {
                org.apache.log4j.Logger l = ls.nextElement();
                if (l != null) {
                    Enumeration<Appender> as = l.getAllAppenders();
                    while (as.hasMoreElements()) {
                        Appender a = as.nextElement();
                        if (a instanceof FileAppender) {
                            FileAppender fa = (FileAppender) a;
                            String f = fa.getFile();
                            if (f != null && f.length() > 0) {
                                int i = f.replace('\\', '/').lastIndexOf('/');
                                String path;
                                if (i == -1) {
                                    path = subdirectory;
                                } else {
                                    path = f.substring(0, i);
                                    if (!path.endsWith(subdirectory)) {
                                        path = path + "/" + subdirectory;
                                    }
                                    f = f.substring(i + 1);
                                }
                                fa.setFile(path + "/" + f);
                                fa.activateOptions();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void stop() {
    }

}
