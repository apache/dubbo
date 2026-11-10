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
package org.apache.dubbo.config.spring.context;

import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class GracefulShutdownTest {

    @Test
    public void testDubboShutdownOrder() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(DubboConfig.class);
        context.refresh();

        MyTask myTask = context.getBean(MyTask.class);

        // Ensure Dubbo is running
        ModuleModel moduleModel = DubboBeanUtils.getModuleModel(context);
        Assertions.assertFalse(moduleModel.isDestroyed(), "Module should be running");

        System.out.println("Closing context...");
        context.close();
        System.out.println("Context closed.");

        Assertions.assertTrue(
                myTask.verified.get(), "MyTask.destroy() should have been called and verified Dubbo status");
    }

    @Configuration
    static class DubboConfig {
        @Bean
        public DubboDeployApplicationListener dubboDeployApplicationListener() {
            return new DubboDeployApplicationListener();
        }

        @Bean
        public MyTask myTask() {
            return new MyTask();
        }
    }

    static class MyTask implements DisposableBean, ApplicationContextAware {
        AtomicBoolean verified = new AtomicBoolean(false);
        ApplicationContext applicationContext;

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        public void destroy() throws Exception {
            System.out.println("MyTask destroying...");
            ModuleModel moduleModel = DubboBeanUtils.getModuleModel(applicationContext);
            // If Dubbo is destroyed on ContextClosedEvent, this will be true.
            // But we want it to be FALSE (still alive).
            boolean destroyed = moduleModel.isDestroyed();
            System.out.println("Dubbo destroyed status in MyTask: " + destroyed);

            if (!destroyed) {
                verified.set(true);
            }
        }
    }
}
