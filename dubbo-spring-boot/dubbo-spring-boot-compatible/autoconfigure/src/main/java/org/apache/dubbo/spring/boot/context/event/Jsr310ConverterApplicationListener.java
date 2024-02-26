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

import org.apache.dubbo.common.convert.jsr310.DefaultLocalDateConverter;
import org.apache.dubbo.common.convert.jsr310.DefaultLocalDateTimeConverter;
import org.apache.dubbo.common.convert.jsr310.DefaultLocalTimeConverter;
import org.apache.dubbo.common.utils.PojoUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

/**
 * @author: Ares
 * @time: 2023-09-13 19:41:51
 * @description: Specify the time format based on the configuration when generic invoke
 * @since 3.2.7
 */
@Configuration
@Role(value = ROLE_INFRASTRUCTURE)
public class Jsr310ConverterApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String DOUBLE_HYPHEN = "\\|\\|";
    private static final AtomicBoolean PROCESSED = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        if (PROCESSED.compareAndSet(false, true)) {
            ConfigurableEnvironment environment = event.getEnvironment();

            String localDateTimeFormat = environment.resolvePlaceholders("${dubbo.generic.local-date-time-format:}");
            if (StringUtils.isNotEmpty(localDateTimeFormat)) {
                List<DateTimeFormatter> localDateTimeFormatterList = Arrays.stream(
                                localDateTimeFormat.split(DOUBLE_HYPHEN))
                        .map(DateTimeFormatter::ofPattern)
                        .collect(Collectors.toList());
                PojoUtils.registerJsr310Converter(
                        LocalDateTime.class, new DefaultLocalDateTimeConverter(localDateTimeFormatterList));
            }

            String localDateFormat = environment.resolvePlaceholders("${dubbo.generic.local-date-format:}");
            if (StringUtils.isNotEmpty(localDateFormat)) {
                List<DateTimeFormatter> localDateFormatterList = Arrays.stream(localDateFormat.split(DOUBLE_HYPHEN))
                        .map(DateTimeFormatter::ofPattern)
                        .collect(Collectors.toList());
                PojoUtils.registerJsr310Converter(
                        LocalDate.class, new DefaultLocalDateConverter(localDateFormatterList));
            }

            String localTimeFormat = environment.resolvePlaceholders("${dubbo.generic.local-time-format:}");
            if (StringUtils.isNotEmpty(localTimeFormat)) {
                List<DateTimeFormatter> localTimeFormatterList = Arrays.stream(localTimeFormat.split(DOUBLE_HYPHEN))
                        .map(DateTimeFormatter::ofPattern)
                        .collect(Collectors.toList());
                PojoUtils.registerJsr310Converter(
                        LocalTime.class, new DefaultLocalTimeConverter(localTimeFormatterList));
            }
        }
    }
}
