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

import org.apache.dubbo.common.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_GITHUB_URL;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_MAILING_LIST;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_SPRING_BOOT_GITHUB_URL;
import static org.apache.dubbo.spring.boot.util.DubboUtils.LINE_SEPARATOR;

/**
 * Dubbo Welcome Logo {@link ApplicationListener}
 *
 * @see ApplicationListener
 * @since 2.7.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20 + 1)  // After LoggingApplicationListener#DEFAULT_ORDER
public class WelcomeLogoApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static AtomicBoolean processed = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {

        // Skip if processed before, prevent duplicated execution in Hierarchical ApplicationContext
        if (processed.get()) {
            return;
        }

        /**
         * Gets Logger After LoggingSystem configuration ready
         * @see LoggingApplicationListener
         */
        final Logger logger = LoggerFactory.getLogger(getClass());

        String bannerText = buildBannerText();

        if (logger.isInfoEnabled()) {
            logger.info(bannerText);
        } else {
            System.out.print(bannerText);
        }

        // mark processed to be true
        processed.compareAndSet(false, true);
    }

    String buildBannerText() {

        StringBuilder bannerTextBuilder = new StringBuilder();

        bannerTextBuilder
                .append(LINE_SEPARATOR)
                .append(LINE_SEPARATOR)
                .append(" :: Dubbo Spring Boot (v").append(Version.getVersion(getClass(), Version.getVersion())).append(") : ")
                .append(DUBBO_SPRING_BOOT_GITHUB_URL)
                .append(LINE_SEPARATOR)
                .append(" :: Dubbo (v").append(Version.getVersion()).append(") : ")
                .append(DUBBO_GITHUB_URL)
                .append(LINE_SEPARATOR)
                .append(" :: Discuss group : ")
                .append(DUBBO_MAILING_LIST)
                .append(LINE_SEPARATOR)
        ;

        return bannerTextBuilder.toString();

    }

}
