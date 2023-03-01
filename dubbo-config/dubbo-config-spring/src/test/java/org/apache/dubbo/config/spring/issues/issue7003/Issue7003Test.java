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
package org.apache.dubbo.config.spring.issues.issue7003;

import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 *
 * Multiple duplicate Dubbo Reference annotations with the same attribute generate only one instance.
 * The test-case for https://github.com/apache/dubbo/issues/7003
 */
@Configuration
@EnableDubbo
@ComponentScan
@PropertySource("classpath:/META-INF/issues/issue7003/config.properties")
class Issue7003Test {

    @BeforeAll
    public static void beforeAll() {
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void afterAll() {
        DubboBootstrap.reset();
    }

    @Test
    void test() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Issue7003Test.class);
        try {

            Map<String, ReferenceBean> referenceBeanMap = context.getBeansOfType(ReferenceBean.class);
            Assertions.assertEquals(1, referenceBeanMap.size());

            Collection<ReferenceConfigBase<?>> references = ApplicationModel.defaultModel().getDefaultModule().getConfigManager().getReferences();
            Assertions.assertEquals(1, references.size());

        } finally {
            context.close();
        }
    }


    @Component
    static class ClassA {

        @DubboReference(group = "demo", version = "1.2.3", check = false)
        private HelloService helloService;

    }

    @Component
    static class ClassB {

        @DubboReference(check = false, version = "1.2.3", group = "demo")
        private HelloService helloService;

    }
}
