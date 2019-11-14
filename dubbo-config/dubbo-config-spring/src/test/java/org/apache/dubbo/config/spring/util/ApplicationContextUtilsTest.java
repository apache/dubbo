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
package org.apache.dubbo.config.spring.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import static org.apache.dubbo.config.spring.util.ApplicationContextUtils.addApplicationListener;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ApplicationContextUtils} Test
 *
 * @since 2.5.7
 */
public class ApplicationContextUtilsTest {

    private ApplicationContext applicationContext;

    @BeforeEach
    public void init() {
        applicationContext = new GenericApplicationContext();
    }

    @Test
    public void testAddApplicationListener() {
        assertTrue(addApplicationListener(applicationContext, event -> {
        }));
    }
}
