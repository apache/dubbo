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
package org.apache.dubbo.common.beans;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.beans.model.FooBeanWithApplicationModel;
import org.apache.dubbo.common.beans.model.FooBeanWithFrameworkModel;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ScopeBeanFactoryTest {

    @Test
    void testInjection() {

        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
        FooBeanWithApplicationModel beanWithApplicationModel = beanFactory.registerBean(FooBeanWithApplicationModel.class);
        Assertions.assertSame(applicationModel, beanWithApplicationModel.getApplicationModel());

        FrameworkModel frameworkModel = applicationModel.getFrameworkModel();
        FooBeanWithFrameworkModel beanWithFrameworkModel = frameworkModel.getBeanFactory().registerBean(FooBeanWithFrameworkModel.class);
        Assertions.assertSame(frameworkModel, beanWithFrameworkModel.getFrameworkModel());

        // child bean factory can obtain bean from parent bean factory
        FooBeanWithFrameworkModel beanWithFrameworkModelFromApp = applicationModel.getBeanFactory().getBean(FooBeanWithFrameworkModel.class);
        Assertions.assertSame(beanWithFrameworkModel, beanWithFrameworkModelFromApp);

        Object objectBean = applicationModel.getBeanFactory().getBean(Object.class);
        Assertions.assertNull(objectBean);

        // child bean factory can obtain bean from parent bean factory by classType
        frameworkModel.getBeanFactory().registerBean(new TestBean());
        applicationModel.getBeanFactory().registerBean(new TestBean());
        List<TestBean> testBeans = applicationModel.getBeanFactory().getBeansOfType(TestBean.class);
        Assertions.assertEquals(testBeans.size(), 2);

        // father can't get son's
        List<TestBean> testBeans_1 = frameworkModel.getBeanFactory().getBeansOfType(TestBean.class);
        Assertions.assertEquals(testBeans_1.size(), 1);


        Assertions.assertFalse(beanWithApplicationModel.isDestroyed());
        Assertions.assertFalse(beanWithFrameworkModel.isDestroyed());

        // destroy
        frameworkModel.destroy();
        Assertions.assertTrue(beanWithApplicationModel.isDestroyed());
        Assertions.assertTrue(beanWithFrameworkModel.isDestroyed());
    }


    static class TestBean {

    }
}
