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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.StringUtils;

import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @since 3.3
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20) // After LoggingApplicationListener#DEFAULT_ORDER
public class DubboNetInterfaceConfigApplicationListener
        implements ApplicationListener<ApplicationContextInitializedEvent> {

    @Override
    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
        ConfigurableEnvironment environment = event.getApplicationContext().getEnvironment();
        String preferredNetworkInterface =
                System.getProperty(CommonConstants.DubboProperty.DUBBO_PREFERRED_NETWORK_INTERFACE);
        if (StringUtils.isBlank(preferredNetworkInterface)) {
            preferredNetworkInterface =
                    environment.getProperty(CommonConstants.DubboProperty.DUBBO_PREFERRED_NETWORK_INTERFACE);
            if (StringUtils.isNotBlank(preferredNetworkInterface)) {
                System.setProperty(
                        CommonConstants.DubboProperty.DUBBO_PREFERRED_NETWORK_INTERFACE, preferredNetworkInterface);
            }
        }
        String ignoredNetworkInterface =
                System.getProperty(CommonConstants.DubboProperty.DUBBO_NETWORK_IGNORED_INTERFACE);
        if (StringUtils.isBlank(ignoredNetworkInterface)) {
            ignoredNetworkInterface =
                    environment.getProperty(CommonConstants.DubboProperty.DUBBO_NETWORK_IGNORED_INTERFACE);
            if (StringUtils.isNotBlank(ignoredNetworkInterface)) {
                System.setProperty(
                        CommonConstants.DubboProperty.DUBBO_NETWORK_IGNORED_INTERFACE, ignoredNetworkInterface);
            }
        }
    }
}
