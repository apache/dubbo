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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link BeanFactoryUtils} Test
 *
 * @since 2.5.7
 */
public class BeanFactoryUtilsTest {

    private AnnotationConfigApplicationContext applicationContext;

    @BeforeEach
    public void init() {
        applicationContext = new AnnotationConfigApplicationContext();
    }

    @Test
    public void testGetOptionalBean() {

        applicationContext.register(TestBean.class);

        applicationContext.refresh();

        TestBean testBean = BeanFactoryUtils.getOptionalBean(applicationContext, "testBean", TestBean.class);

        Assertions.assertNotNull(testBean);

        Assertions.assertEquals("Hello,World", testBean.getName());

    }

    @Test
    public void testGetOptionalBeanIfAbsent() {

        applicationContext.refresh();

        TestBean testBean = BeanFactoryUtils.getOptionalBean(applicationContext, "testBean", TestBean.class);

        Assertions.assertNull(testBean);
    }

    @Test
    public void testGetBeans() {

        applicationContext.register(TestBean.class, TestBean2.class);

        applicationContext.refresh();

        List<TestBean> testBeans = BeanFactoryUtils.getBeans(applicationContext, new String[]{"testBean"}, TestBean.class);

        Assertions.assertEquals(1, testBeans.size());

        Assertions.assertEquals("Hello,World", testBeans.get(0).getName());

    }

    @Test
    public void testGetBeansIfAbsent() {

        applicationContext.refresh();

        List<TestBean> testBeans = BeanFactoryUtils.getBeans(applicationContext, new String[]{"testBean"}, TestBean.class);

        Assertions.assertTrue(testBeans.isEmpty());

    }


    @Component("testBean2")
    private static class TestBean2 extends TestBean {

    }

    @Component("testBean")
    private static class TestBean {

        private String name = "Hello,World";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
