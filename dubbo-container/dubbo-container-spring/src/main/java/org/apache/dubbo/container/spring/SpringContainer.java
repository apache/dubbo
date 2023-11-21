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
package org.apache.dubbo.container.spring;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.SystemPropertyConfigUtils;
import org.apache.dubbo.container.Container;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.apache.dubbo.common.constants.CommonConstants.DubboProperty.DUBBO_SPRING_CONFIG;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_STOP_DUBBO_ERROR;

/**
 * SpringContainer. (SPI, Singleton, ThreadSafe)
 * <p>
 * The container class implementation for Spring
 */
public class SpringContainer implements Container {

    public static final String DEFAULT_SPRING_CONFIG = "classpath*:META-INF/spring/*.xml";
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(SpringContainer.class);
    static ClassPathXmlApplicationContext context;

    public static ClassPathXmlApplicationContext getContext() {
        return context;
    }

    @Override
    public void start() {
        String configPath = SystemPropertyConfigUtils.getSystemProperty(DUBBO_SPRING_CONFIG);
        if (StringUtils.isEmpty(configPath)) {
            configPath = DEFAULT_SPRING_CONFIG;
        }
        context = new ClassPathXmlApplicationContext(configPath.split("[,\\s]+"), false);
        context.refresh();
        context.start();
    }

    @Override
    public void stop() {
        try {
            if (context != null) {
                context.stop();
                context.close();
                context = null;
            }
        } catch (Throwable e) {
            logger.error(CONFIG_STOP_DUBBO_ERROR, "", "", e.getMessage(), e);
        }
    }
}
