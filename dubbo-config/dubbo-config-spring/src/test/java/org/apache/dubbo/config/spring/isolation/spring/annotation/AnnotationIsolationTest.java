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
package org.apache.dubbo.config.spring.isolation.spring.annotation;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.spring.isolation.spring.BaseTest;
import org.apache.dubbo.config.spring.isolation.spring.support.DemoServiceExecutor;
import org.apache.dubbo.config.spring.isolation.spring.support.HelloServiceExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.Executor;

import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE_ISOLATION;

public class AnnotationIsolationTest extends BaseTest {

    @Test
    public void test() throws Exception {
        // start provider app
        AnnotationConfigApplicationContext providerContext = new AnnotationConfigApplicationContext(ProviderConfiguration.class);
        providerContext.start();

        // start consumer app
        AnnotationConfigApplicationContext consumerContext = new AnnotationConfigApplicationContext(ConsumerConfiguration.class);
        consumerContext.start();

        // getAndSet serviceConfig
        setServiceConfig(providerContext);

        // assert isolation of executor
        assertExecutor(providerContext, consumerContext);

        // close context
        providerContext.close();
        consumerContext.close();

    }

    private void setServiceConfig(AnnotationConfigApplicationContext providerContext) {
        Map<String, ServiceConfig> serviceConfigMap = providerContext.getBeansOfType(ServiceConfig.class);
        serviceConfig1 = serviceConfigMap.get("ServiceBean:org.apache.dubbo.config.spring.api.DemoService:1.0.0:Group1");
        serviceConfig2 = serviceConfigMap.get("ServiceBean:org.apache.dubbo.config.spring.api.HelloService:2.0.0:Group2");
        serviceConfig3 = serviceConfigMap.get("ServiceBean:org.apache.dubbo.config.spring.api.HelloService:3.0.0:Group3");
    }

    // note scanBasePackages, refer three service with dubbo and tri protocol
    @Configuration
    @EnableDubbo(scanBasePackages = "org.apache.dubbo.demo.consumer.comp")
    @ComponentScan(value = {"org.apache.dubbo.config.spring.isolation.spring.annotation.consumer"})
    static class ConsumerConfiguration {
        @Bean
        public RegistryConfig registryConfig() {
            RegistryConfig registryConfig = new RegistryConfig();
            registryConfig.setAddress("zookeeper://127.0.0.1:2181");
            return registryConfig;
        }

        @Bean
        public ApplicationConfig applicationConfig() {
            ApplicationConfig applicationConfig = new ApplicationConfig("consumer-app");
            return applicationConfig;
        }
    }

    // note scanBasePackages, expose three service with dubbo and tri protocol
    @Configuration
    @EnableDubbo(scanBasePackages = "org.apache.dubbo.config.spring.isolation.spring.annotation.provider")
    static class ProviderConfiguration {
        @Bean
        public RegistryConfig registryConfig() {
            RegistryConfig registryConfig = new RegistryConfig();
            registryConfig.setAddress("zookeeper://127.0.0.1:2181");
            return registryConfig;
        }

        // NOTE: we need config executor-management-mode="isolation"
        @Bean
        public ApplicationConfig applicationConfig() {
            ApplicationConfig applicationConfig = new ApplicationConfig("provider-app");

            applicationConfig.setExecutorManagementMode(EXECUTOR_MANAGEMENT_MODE_ISOLATION);
            return applicationConfig;
        }

        // expose services with dubbo protocol
        @Bean
        public ProtocolConfig dubbo() {
            ProtocolConfig protocolConfig = new ProtocolConfig("dubbo");
            return protocolConfig;
        }

        // expose services with tri protocol
        @Bean
        public ProtocolConfig tri() {
            ProtocolConfig protocolConfig = new ProtocolConfig("tri");
            return protocolConfig;
        }

        // customized thread pool
        @Bean("executor-demo-service")
        public Executor demoServiceExecutor() {
            return new DemoServiceExecutor();
        }

        // customized thread pool
        @Bean("executor-hello-service")
        public Executor helloServiceExecutor() {
            return new HelloServiceExecutor();
        }
    }
}
