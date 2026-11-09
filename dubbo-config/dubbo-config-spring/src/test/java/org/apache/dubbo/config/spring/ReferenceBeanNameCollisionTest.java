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
package org.apache.dubbo.config.spring;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.beans.factory.annotation.ServiceAnnotationTestConfiguration;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

/**
 * Reproduces https://github.com/apache/dubbo/issues/12637: an auto-derived reference bean name
 * (the bare field/property name, used when no explicit {@code id} is given) can collide with the
 * default name a plain {@code @Resource}-style by-name lookup elsewhere in the same Spring
 * context uses for an unrelated bean. Once the reference bean claims that name, any other lookup
 * of that exact name expecting a different type fails with a confusing
 * {@link BeanNotOfRequiredTypeException} instead of resolving the unrelated bean by type.
 *
 * <p>Reuses the same {@link EnableDubbo} + {@link ServiceAnnotationTestConfiguration} bootstrap
 * as {@link org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessorTest},
 * which is already proven to correctly activate {@code @DubboReference} processing.
 */
@EnableDubbo(scanBasePackages = "org.apache.dubbo.config.spring.context.annotation.provider")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ServiceAnnotationTestConfiguration.class, ReferenceBeanNameCollisionTest.class})
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class ReferenceBeanNameCollisionTest {

    @BeforeAll
    static void setUp() {
        DubboBootstrap.reset();
    }

    @AfterEach
    void tearDown() {
        DubboBootstrap.reset();
    }

    // Auto-derived reference bean name will be the bare field name: "collisionProbe".
    // No other bean is declared under this name - unlike a bean already registered at
    // definition time (which Dubbo's existing rename-on-collision logic already handles),
    // this mirrors an independent @Resource field elsewhere with the same default name:
    // a pure runtime by-name lookup that Dubbo cannot see coming at registration time.
    @DubboReference(version = "2", url = "dubbo://127.0.0.1:12345?version=2")
    private DemoService collisionProbe;

    // Unrelated marker type, standing in for whatever type an independent @Resource
    // field with the same default name would declare.
    static class UnrelatedType {}

    @Test
    void lookupByNameWithUnrelatedTypeAfterReferenceBeanClaimsName(ApplicationContext context) {
        Assertions.assertTrue(
                context.containsBean("collisionProbe"),
                "the @DubboReference field should have auto-registered a reference bean "
                        + "under its bare property name");
        Assertions.assertThrows(
                BeanNotOfRequiredTypeException.class,
                () -> context.getBean("collisionProbe", UnrelatedType.class),
                "documents the known limitation: without opting into "
                        + "dubbo.application.qualify-reference-bean-name, an unrelated by-name lookup "
                        + "(e.g. an independent @Resource field with the same default name) collides "
                        + "with the reference bean instead of falling back to by-type resolution");
    }
}
