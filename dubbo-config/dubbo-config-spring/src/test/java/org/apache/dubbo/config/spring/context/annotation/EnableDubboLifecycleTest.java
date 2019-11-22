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
package org.apache.dubbo.config.spring.context.annotation;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link EnableDubboLifecycle} Test
 *
 * @since 2.7.5
 */
@EnableDubboLifecycle
public class EnableDubboLifecycleTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    public void init() {
        ApplicationModel.getConfigManager().setApplication(new ApplicationConfig("EnableDubboLifecycleTest"));
        context = new AnnotationConfigApplicationContext(EnableDubboLifecycleTest.class);
    }

    @AfterEach
    public void destroy() {
        context.close();
    }

    @Test
    public void test() {
        assertTrue(DubboBootstrap.getInstance().isInitialized());
        assertTrue(DubboBootstrap.getInstance().isStarted());
    }
}
