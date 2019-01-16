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
package org.apache.dubbo.config.spring.context.context.annotation.consumer.test;

import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Test Consumer Configuration
 *
 * @since 2.5.7
 */
@EnableDubbo(scanBasePackageClasses = TestConsumerConfiguration.class, multipleConfig = true)
@PropertySource("META-INF/dubbb-consumer.properties")
@EnableTransactionManagement
public class TestConsumerConfiguration {

    @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345", application = "dubbo-demo-application")
    private DemoService demoService;

    public DemoService getDemoService() {
        return demoService;
    }

    public void setDemoService(DemoService demoService) {
        this.demoService = demoService;
    }


    @Bean
    public TestConsumerConfiguration.Child c() {
        return new TestConsumerConfiguration.Child();
    }

    public static abstract class Ancestor {

        @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345", application = "dubbo-demo-application")
        private DemoService demoServiceFromAncestor;

        public DemoService getDemoServiceFromAncestor() {
            return demoServiceFromAncestor;
        }

        public void setDemoServiceFromAncestor(DemoService demoServiceFromAncestor) {
            this.demoServiceFromAncestor = demoServiceFromAncestor;
        }
    }

    public static abstract class Parent extends TestConsumerConfiguration.Ancestor {

        private DemoService demoServiceFromParent;

        public DemoService getDemoServiceFromParent() {
            return demoServiceFromParent;
        }

        @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345", application = "dubbo-demo-application")
        public void setDemoServiceFromParent(DemoService demoServiceFromParent) {
            this.demoServiceFromParent = demoServiceFromParent;
        }

    }

    public static class Child extends TestConsumerConfiguration.Parent {

        @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345", application = "dubbo-demo-application")
        private DemoService demoServiceFromChild;

        public DemoService getDemoServiceFromChild() {
            return demoServiceFromChild;
        }

        public void setDemoServiceFromChild(DemoService demoServiceFromChild) {
            this.demoServiceFromChild = demoServiceFromChild;
        }
    }
}
