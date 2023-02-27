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
package org.apache.dubbo.config.spring.beans.factory.annotation;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class DubboConfigAliasPostProcessorTest {

    private static final String APP_NAME = "APP_NAME";
    private static final String APP_ID = "APP_ID";

    @Test
    void test() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfigurationX.class);
        try {
            context.start();
            Assertions.assertEquals(context.getAliases(APP_NAME)[0], APP_ID);
            Assertions.assertEquals(context.getAliases(APP_ID)[0], APP_NAME);
            Assertions.assertEquals(context.getBean(APP_NAME), context.getBean(APP_ID));
        } finally {
            context.close();
        }
    }

    @EnableDubbo(scanBasePackages = "")
    @Configuration
    static class TestConfigurationX {

        @Bean(APP_NAME)
        public ApplicationConfig applicationConfig() {
            ApplicationConfig applicationConfig = new ApplicationConfig();
            applicationConfig.setName(APP_NAME);
            applicationConfig.setId(APP_ID);
            return applicationConfig;
        }

    }
}
