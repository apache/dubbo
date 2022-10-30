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
package org.apache.dubbo.spring.boot.context.event;

import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.config.AbstractConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.SortedMap;

import static org.apache.dubbo.spring.boot.util.DubboUtils.DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE;
import static org.apache.dubbo.spring.boot.util.DubboUtils.OVERRIDE_CONFIG_FULL_PROPERTY_NAME;
import static org.apache.dubbo.spring.boot.util.DubboUtils.filterDubboProperties;

/**
 * {@link ApplicationListener} to override the dubbo properties from {@link Environment}into
 * {@link ConfigUtils#getProperties() Dubbo Config}.
 * {@link AbstractConfig Dubbo Config} on {@link ApplicationEnvironmentPreparedEvent}.
 * <p>
 *
 * @see ConfigUtils
 * @since 2.7.0
 */
@Order // LOWEST_PRECEDENCE Make sure last execution
public class OverrideDubboConfigApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {

        /**
         * Gets Logger After LoggingSystem configuration ready
         * @see LoggingApplicationListener
         */
        final Logger logger = LoggerFactory.getLogger(getClass());

        ConfigurableEnvironment environment = event.getEnvironment();

        boolean override = environment.getProperty(OVERRIDE_CONFIG_FULL_PROPERTY_NAME, boolean.class,
                DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE);

        if (override) {

            SortedMap<String, Object> dubboProperties = filterDubboProperties(environment);

            ConfigUtils.getProperties().putAll(dubboProperties);

            if (logger.isInfoEnabled()) {
                logger.info("Dubbo Config was overridden by externalized configuration {}", dubboProperties);
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Disable override Dubbo Config caused by property {} = {}", OVERRIDE_CONFIG_FULL_PROPERTY_NAME, override);
            }
        }

    }

}
