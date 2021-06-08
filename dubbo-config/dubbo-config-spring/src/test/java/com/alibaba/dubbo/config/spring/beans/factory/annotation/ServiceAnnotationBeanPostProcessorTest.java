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
package com.alibaba.dubbo.config.spring.beans.factory.annotation;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.config.spring.api.HelloService;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 * {@link ServiceAnnotationBeanPostProcessor} Test
 *
 * @since 2.5.8
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = {
                ServiceAnnotationTestConfiguration.class,
                ServiceAnnotationBeanPostProcessorTest.class
        })
@TestPropertySource(properties = {
        "provider.package = com.alibaba.dubbo.config.spring.context.annotation.provider",
        "packagesToScan = ${provider.package}",
})
public class ServiceAnnotationBeanPostProcessorTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Bean
    public ServiceAnnotationBeanPostProcessor serviceAnnotationBeanPostProcessor2
            (@Value("${packagesToScan}") String... packagesToScan) {
        return new ServiceAnnotationBeanPostProcessor(packagesToScan);
    }

    @Test
    public void test() {

        Map<String, HelloService> helloServicesMap = beanFactory.getBeansOfType(HelloService.class);

        Assert.assertEquals(2, helloServicesMap.size());

        Map<String, ServiceBean> serviceBeansMap = beanFactory.getBeansOfType(ServiceBean.class);

        Assert.assertEquals(2, serviceBeansMap.size());

        Map<String, ServiceAnnotationBeanPostProcessor> beanPostProcessorsMap =
                beanFactory.getBeansOfType(ServiceAnnotationBeanPostProcessor.class);

        Assert.assertEquals(2, beanPostProcessorsMap.size());

        Assert.assertTrue(beanPostProcessorsMap.containsKey("serviceAnnotationBeanPostProcessor"));
        Assert.assertTrue(beanPostProcessorsMap.containsKey("serviceAnnotationBeanPostProcessor2"));

    }

    /**
     * Test if the {@link Service#parameters()} works well
     * see issue: https://github.com/apache/dubbo/issues/3072
     */
    @Test
    public void testDubboServiceParameter() {
        /**
         * get the {@link ServiceBean} of {@link com.alibaba.dubbo.config.spring.context.annotation.provider.DefaultHelloService}
         * */
        ServiceBean serviceBean = beanFactory.getBean("ServiceBean:com.alibaba.dubbo.config.spring.api.HelloService", ServiceBean.class);
        Assert.assertNotNull(serviceBean);
        Assert.assertNotNull(serviceBean.getParameters());
        Assert.assertTrue(serviceBean.getParameters().size() == 1);
        Assert.assertEquals(serviceBean.toUrl().getParameter("sayHello.timeout"), "3000");
    }

}
