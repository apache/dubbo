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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import com.service.DemoService1;
import com.service.DemoService2;
import com.service.DemoService4;
import com.service.deep1.deep2.deep3.DemoService3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import static org.apache.dubbo.common.constants.CommonConstants.CLASS_DESERIALIZE_ALLOWED_LIST;
import static org.apache.dubbo.common.constants.CommonConstants.CLASS_DESERIALIZE_BLOCKED_LIST;

class SerializeSecurityConfiguratorTest {

    @Test
    void test() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();
        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        Assertions.assertTrue(ssm.getAllowedPrefix().contains("java.util.HashMap"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.example.DemoInterface"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.sun.Interface1"));
        Assertions.assertTrue(ssm.getDisAllowedPrefix().contains("com.exampletest.DemoInterface"));
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.sun.Interface2"));
        Assertions.assertEquals(AllowClassNotifyListener.DEFAULT_STATUS, ssm.getCheckStatus());

        frameworkModel.destroy();
    }

    @Test
    void testStatus1() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();
        ApplicationConfig applicationConfig = new ApplicationConfig("Test");
        applicationConfig.setSerializeCheckStatus(SerializeCheckStatus.DISABLE.name());
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        Assertions.assertEquals(SerializeCheckStatus.DISABLE, ssm.getCheckStatus());

        frameworkModel.destroy();
    }

    @Test
    void testStatus2() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();
        ApplicationConfig applicationConfig = new ApplicationConfig("Test");
        applicationConfig.setSerializeCheckStatus(SerializeCheckStatus.WARN.name());
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        Assertions.assertEquals(SerializeCheckStatus.WARN, ssm.getCheckStatus());

        frameworkModel.destroy();
    }

    @Test
    void testStatus3() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();
        ApplicationConfig applicationConfig = new ApplicationConfig("Test");
        applicationConfig.setSerializeCheckStatus(SerializeCheckStatus.STRICT.name());
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        Assertions.assertEquals(SerializeCheckStatus.STRICT, ssm.getCheckStatus());

        frameworkModel.destroy();
    }

    @Test
    void testStatus4() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();
        System.setProperty(CommonConstants.CLASS_DESERIALIZE_OPEN_CHECK, "false");

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        Assertions.assertEquals(SerializeCheckStatus.DISABLE, ssm.getCheckStatus());

        System.clearProperty(CommonConstants.CLASS_DESERIALIZE_OPEN_CHECK);
        frameworkModel.destroy();
    }

    @Test
    void testStatus5() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();
        System.setProperty(CommonConstants.CLASS_DESERIALIZE_BLOCK_ALL, "true");

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        Assertions.assertEquals(SerializeCheckStatus.STRICT, ssm.getCheckStatus());

        System.clearProperty(CommonConstants.CLASS_DESERIALIZE_BLOCK_ALL);
        frameworkModel.destroy();
    }

    @Test
    void testConfig1() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();
        System.setProperty(CLASS_DESERIALIZE_ALLOWED_LIST, "test.package1, test.package2, ,");

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        Assertions.assertTrue(ssm.getAllowedPrefix().contains("test.package1"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("test.package2"));

        System.clearProperty(CommonConstants.CLASS_DESERIALIZE_ALLOWED_LIST);
        frameworkModel.destroy();

    }

    @Test
    void testConfig2() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();
        System.setProperty(CLASS_DESERIALIZE_BLOCKED_LIST, "test.package1, test.package2, ,");

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        Assertions.assertTrue(ssm.getDisAllowedPrefix().contains("test.package1"));
        Assertions.assertTrue(ssm.getDisAllowedPrefix().contains("test.package2"));

        System.clearProperty(CommonConstants.CLASS_DESERIALIZE_BLOCK_ALL);
        frameworkModel.destroy();

    }

    @Test
    void testConfig3() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();
        System.setProperty(CLASS_DESERIALIZE_ALLOWED_LIST, "test.package1, test.package2, ,");
        System.setProperty(CLASS_DESERIALIZE_BLOCKED_LIST, "test.package1, test.package2, ,");

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        Assertions.assertTrue(ssm.getAllowedPrefix().contains("test.package1"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("test.package2"));

        System.clearProperty(CommonConstants.CLASS_DESERIALIZE_ALLOWED_LIST);
        System.clearProperty(CommonConstants.CLASS_DESERIALIZE_BLOCK_ALL);
        frameworkModel.destroy();

    }

    @Test
    void testSerializable1() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ApplicationConfig applicationConfig = new ApplicationConfig("Test");
        applicationConfig.setCheckSerializable(false);
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);
        ModuleModel moduleModel = applicationModel.newModule();

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        Assertions.assertFalse(ssm.isCheckSerializable());

        frameworkModel.destroy();

    }

    @Test
    void testSerializable2() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        Assertions.assertTrue(ssm.isCheckSerializable());

        frameworkModel.destroy();

    }

    @Test
    void testGeneric() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        serializeSecurityConfigurator.registerInterface(DemoService4.class);
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.service.DemoService4"));

        frameworkModel.destroy();
    }
    @Test
    void testRegister1() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        serializeSecurityConfigurator.registerInterface(DemoService1.class);
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.service.DemoService1"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo1"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo2"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo3"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo4"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo5"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo6"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo7"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo8"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Simple"));

        Assertions.assertTrue(ssm.getAllowedPrefix().contains(List.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(Set.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(Map.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(LinkedList.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(Vector.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(HashSet.class.getName()));

        frameworkModel.destroy();
    }


    @Test
    void testRegister2() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        serializeSecurityConfigurator.registerInterface(DemoService2.class);
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.service.DemoService2"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo1"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo2"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo3"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo4"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo5"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo6"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo7"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo8"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Simple"));

        Assertions.assertTrue(ssm.getAllowedPrefix().contains(List.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(Set.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(Map.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(LinkedList.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(Vector.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(HashSet.class.getName()));

        frameworkModel.destroy();
    }

    @Test
    void testRegister3() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();
        ApplicationConfig applicationConfig = new ApplicationConfig("Test");
        applicationConfig.setAutoTrustSerializeClass(false);
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        serializeSecurityConfigurator.registerInterface(DemoService1.class);
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.service.DemoService1"));
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.pojo.Demo1"));
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.pojo.Demo2"));
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.pojo.Demo3"));
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.pojo.Demo4"));
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.pojo.Demo5"));
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.pojo.Demo6"));
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.pojo.Demo7"));
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.pojo.Demo8"));
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.pojo.Simple"));

        frameworkModel.destroy();
    }

    @Test
    void testRegister4() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();
        ApplicationConfig applicationConfig = new ApplicationConfig("Test");
        applicationConfig.setTrustSerializeClassLevel(4);
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        serializeSecurityConfigurator.registerInterface(DemoService3.class);
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.service.deep1.deep2."));

        frameworkModel.destroy();
    }

    @Test
    void testRegister5() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();
        ApplicationConfig applicationConfig = new ApplicationConfig("Test");
        applicationConfig.setTrustSerializeClassLevel(10);
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeSecurityConfigurator serializeSecurityConfigurator = new SerializeSecurityConfigurator(moduleModel);
        serializeSecurityConfigurator.onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());

        serializeSecurityConfigurator.registerInterface(DemoService3.class);
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.service.deep1.deep2.deep3.DemoService3"));

        frameworkModel.destroy();
    }
}
