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
package org.apache.dubbo.config.spring.issues;

import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.*;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext;

/**
 * The test-case for https://github.com/apache/dubbo/issues/7752
 *
 * @since 2.7.8
 */
@Configuration
@EnableDubboConfig
@PropertySource("classpath:/META-INF/issue-7752-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class Issue7752Test {

    private static final class Issue7752TestBeanPostProcessorChecker implements BeanPostProcessor{

        private ConfigurableListableBeanFactory beanFactory;

        private int beanPostProcessorTargetCount;

        public void setParam(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount){
            this.beanFactory = beanFactory;
            this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
        }

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
                    //need + 1 self
                    this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount + 1) {
                Assertions.assertFalse(true);
            }
            return bean;
        }

        private boolean isInfrastructureBean(@Nullable String beanName) {
            if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
                BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
                return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
            }
            return false;
        }
    }

    @Test
    public void test() throws Exception {
        Class<?> aClass = Class.forName("org.springframework.context.support.PostProcessorRegistrationDelegate");

        Issue7752TestBeanPostProcessorChecker issue7752TestBeanPostProcessorChecker = new Issue7752TestBeanPostProcessorChecker();
        Mockito.mockStatic(aClass, new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if(invocation.getMethod().getName().equals("registerBeanPostProcessors") && (invocation.getArgument(1) instanceof AbstractApplicationContext)){
                    ConfigurableListableBeanFactory beanFactory = invocation.getArgument(0);

                    String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

                    // Register BeanPostProcessorChecker that logs an info message when
                    // a bean is created during BeanPostProcessor instantiation, i.e. when
                    // a bean is not eligible for getting processed by all BeanPostProcessors.
                    int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;

                    issue7752TestBeanPostProcessorChecker.setParam(beanFactory, beanProcessorTargetCount);
                    beanFactory.addBeanPostProcessor(issue7752TestBeanPostProcessorChecker);
                    Object o = invocation.callRealMethod();
                    //((AbstractBeanFactory)beanFactory).getBeanPostProcessors().remove(issue7752TestBeanPostProcessorChecker);
                    return o;
                }
                return invocation.callRealMethod();
            }
        });
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Issue7752Test.class);
        context.close();
    }

}
