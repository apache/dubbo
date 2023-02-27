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
package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.dubbo.spring.boot.context.event.AwaitingNonWebApplicationListener;
import org.apache.dubbo.spring.boot.context.event.DubboConfigBeanDefinitionConflictApplicationListener;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_PREFIX;

/**
 * Dubbo Listener Auto-{@link Configuration}
 *
 * @since 3.0.4
 */
@ConditionalOnProperty(prefix = DUBBO_PREFIX, name = "enabled", matchIfMissing = true)
@Configuration
public class DubboListenerAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public DubboConfigBeanDefinitionConflictApplicationListener dubboConfigBeanDefinitionConflictApplicationListener() {
        return new DubboConfigBeanDefinitionConflictApplicationListener();
    }

    @ConditionalOnMissingBean
    @Bean
    public AwaitingNonWebApplicationListener awaitingNonWebApplicationListener() {
        return new AwaitingNonWebApplicationListener();
    }
}
