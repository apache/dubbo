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
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

/**
 * Companion to {@link ReferenceBeanNameCollisionTest}: same reproduction scenario for
 * https://github.com/apache/dubbo/issues/12637, but with the opt-in
 * {@code dubbo.application.qualify-reference-bean-name} flag enabled. Verifies that the same
 * by-name lookup of an unrelated type no longer collides with the {@code @DubboReference}
 * field's auto-derived bean name once qualification is turned on.
 */
@EnableDubbo(scanBasePackages = "org.apache.dubbo.config.spring.context.annotation.provider")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ServiceAnnotationTestConfiguration.class, ReferenceBeanNameCollisionFixedTest.class})
@TestPropertySource(properties = "dubbo.application.qualify-reference-bean-name=true")
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class ReferenceBeanNameCollisionFixedTest {

    @BeforeAll
    static void setUp() {
        DubboBootstrap.reset();
    }

    @AfterEach
    void tearDown() {
        DubboBootstrap.reset();
    }

    // With qualification enabled, this no longer registers under the bare name
    // "collisionProbe" - it's suffixed, so it can no longer collide with an unrelated
    // by-name lookup for "collisionProbe" elsewhere in the context.
    @DubboReference(version = "2", url = "dubbo://127.0.0.1:12345?version=2")
    private DemoService collisionProbe;

    static class UnrelatedType {}

    @Test
    void unrelatedByNameLookupNoLongerCollidesWhenQualificationEnabled(ApplicationContext context) {
        Assertions.assertFalse(
                context.containsBean("collisionProbe"),
                "bare name should no longer be claimed by the reference bean when qualification is enabled");
        Assertions.assertThrows(
                org.springframework.beans.factory.NoSuchBeanDefinitionException.class,
                () -> context.getBean("collisionProbe", UnrelatedType.class),
                "no bean should exist under the bare name at all now - a real @Resource field would "
                        + "correctly fall back to by-type resolution instead of hitting a type mismatch");
    }
}
