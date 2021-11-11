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
package org.apache.dubbo.config.spring.context.annotation.provider;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.UUID;

@DubboComponentScan(basePackages = "org.apache.dubbo.config.spring.context.annotation.provider")
@PropertySource("classpath:/META-INF/default.properties")
@EnableTransactionManagement
public class ProviderConfiguration {

    /**
     * Current application configuration, to replace XML config:
     * <prev>
     * &lt;dubbo:application name="dubbo-demo-application"/&gt;
     * </prev>
     *
     * @return {@link ApplicationConfig} Bean
     */
    @Bean("dubbo-demo-application")
    public ApplicationConfig applicationConfig() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("dubbo-demo-application");
        return applicationConfig;
    }

    /**
     * Current registry center configuration, to replace XML config:
     * <prev>
     * &lt;dubbo:registry id="my-registry" address="N/A"/&gt;
     * </prev>
     *
     * @return {@link RegistryConfig} Bean
     */
    @Bean("my-registry")
    public RegistryConfig registryConfig() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("N/A");
        return registryConfig;
    }

    /**
     * Current protocol configuration, to replace XML config:
     * <prev>
     * &lt;dubbo:protocol name="injvm" port="-1"/&gt;
     * </prev>
     *
     * @return {@link ProtocolConfig} Bean
     */
    @Bean("dubbo")
    public ProtocolConfig protocolConfig() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName("injvm");
        protocolConfig.setPort(-1);
        return protocolConfig;
    }

    @Primary
    @Bean
    public PlatformTransactionManager platformTransactionManager() {
        return new AbstractPlatformTransactionManager() {
            private Logger logger = LoggerFactory.getLogger("TestPlatformTransactionManager");

            @Override
            protected Object doGetTransaction() throws TransactionException {
                String transaction = "transaction_" + UUID.randomUUID().toString();
                logger.info("Create transaction: " + transaction);
                return transaction;
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
                logger.info("Begin transaction: " + transaction);
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
                logger.info("Commit transaction: " + status.getTransaction());
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
                logger.info("Rollback transaction: " + status.getTransaction());
            }
        };
    }

}

