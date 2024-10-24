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

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestBeansConfiguration {

    @Bean
    ApplicationConfig application1() {
        ApplicationConfig config = new ApplicationConfig();
        config.setId("application1");
        return config;
    }

    @Bean
    ModuleConfig module1() {
        ModuleConfig config = new ModuleConfig();
        config.setId("module1");
        return config;
    }

    @Bean
    RegistryConfig registry1() {
        RegistryConfig config = new RegistryConfig();
        config.setId("registry1");
        return config;
    }

    @Bean
    MonitorConfig monitor1() {
        MonitorConfig config = new MonitorConfig();
        config.setId("monitor1");
        return config;
    }

    @Bean
    ProtocolConfig protocol1() {
        ProtocolConfig config = new ProtocolConfig();
        config.setId("protocol1");
        return config;
    }

    @Bean
    ConsumerConfig consumer1() {
        ConsumerConfig config = new ConsumerConfig();
        config.setId("consumer1");
        return config;
    }

    @Bean
    ProviderConfig provider1() {
        ProviderConfig config = new ProviderConfig();
        config.setId("provider1");
        return config;
    }
}
