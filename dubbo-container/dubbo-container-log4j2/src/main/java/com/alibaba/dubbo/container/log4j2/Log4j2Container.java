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
package com.alibaba.dubbo.container.log4j2;

import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.container.Container;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.properties.PropertiesConfiguration;
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;


import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * Log4j2Container. (SPI, Singleton, ThreadSafe)
 *
 * @author william.liangf
 */
public class Log4j2Container implements Container {

    public static final String LOG4J_FILE = "dubbo.log4j.file";

    public static final String LOG4J_LEVEL = "dubbo.log4j.level";

    public static final String LOG4J_IS_CONSOLE = "dubbo.log4j.console";

    public static final String DEFAULT_LOG4J_LEVEL = "ERROR";

    /**
     * The Unix separator character.
     */
    private static final char UNIX_SEPARATOR = '/';

    /**
     * The Windows separator character.
     */
    private static final char WINDOWS_SEPARATOR = '\\';

    @SuppressWarnings("unchecked")
    public void start() {
        String file = ConfigUtils.getProperty(LOG4J_FILE);
        if (file != null && file.length() > 0) {
            Level level = Level.toLevel(ConfigUtils.getProperty(LOG4J_LEVEL), Level.ERROR);

            final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

            try {
                String basePath;
                String filename;
                int index = -1;

                if (file.lastIndexOf(UNIX_SEPARATOR) > -1) {
                    index = file.lastIndexOf(UNIX_SEPARATOR);
                } else if (file.lastIndexOf(WINDOWS_SEPARATOR) > -1) {
                    index = file.lastIndexOf(WINDOWS_SEPARATOR);
                }

                if (index > -1) {
                    basePath = file.substring(0, index);
                    filename = file.substring(index + 1);
                } else {
                    basePath = "./";
                    filename = file;
                }

                if (filename.lastIndexOf(".") > -1) {
                    filename = filename.substring(0, filename.lastIndexOf("."));
                }

                Map<String, Appender> appenderMap = ctx.getConfiguration().getAppenders();

                System.setProperty("log4j2_file", file);
                System.setProperty("log4j2_basePath", basePath);
                System.setProperty("log4j2_filename", filename);
                URL url = getClass().getResource("/log4j2-default.properties");
                ConfigurationSource source = new ConfigurationSource(url.openStream(), url);

                PropertiesConfigurationFactory propertiesConfigurationFactory = new PropertiesConfigurationFactory();

                PropertiesConfiguration configuration = propertiesConfigurationFactory.getConfiguration(ctx, source);
                configuration.start();

                LoggerConfig loggerConfig = ctx.getConfiguration().getRootLogger();

                //清除默认的Appender
                for (String name : loggerConfig.getAppenders().keySet()) {
                    if (name != null && name.contains("DefaultConsole")) {
                        loggerConfig.removeAppender(name);
                    }
                }

                loggerConfig.addAppender(configuration.getAppender("DubboRollingFileName"), level, null);

                if (Boolean.valueOf(ConfigUtils.getProperty(LOG4J_IS_CONSOLE, "true"))) {
                    loggerConfig.addAppender(configuration.getAppender("DubboConsoleName"), level, null);
                }

                loggerConfig.setLevel(level);
                ctx.updateLoggers();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
    }

}