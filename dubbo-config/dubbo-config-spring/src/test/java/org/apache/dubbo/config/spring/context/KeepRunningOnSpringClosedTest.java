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

import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.deploy.ModuleDeployer;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.DubboStateListener;
import org.apache.dubbo.config.spring.SysProps;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

class KeepRunningOnSpringClosedTest {

    @Test
    void test(){

        // set KeepRunningOnSpringClosed flag for next spring context
        DubboSpringInitCustomizerHolder.get().addCustomizer(context-> {
            context.setKeepRunningOnSpringClosed(true);
        });

        ClassPathXmlApplicationContext providerContext = null;
        try {
            String resourcePath = "org/apache/dubbo/config/spring";
            providerContext = new ClassPathXmlApplicationContext(
                resourcePath + "/demo-provider.xml",
                resourcePath + "/demo-provider-properties.xml");
            providerContext.start();

            // Expect 1: dubbo application state is STARTED after spring context start finish.
            // No need check and wait

            DubboStateListener dubboStateListener = providerContext.getBean(DubboStateListener.class);
            Assertions.assertEquals(DeployState.STARTED, dubboStateListener.getState());

            ModuleModel moduleModel = providerContext.getBean(ModuleModel.class);
            ModuleDeployer moduleDeployer = moduleModel.getDeployer();
            Assertions.assertTrue(moduleDeployer.isStarted());

            ApplicationDeployer applicationDeployer = moduleModel.getApplicationModel().getDeployer();
            Assertions.assertEquals(DeployState.STARTED, applicationDeployer.getState());
            Assertions.assertEquals(true, applicationDeployer.isStarted());
            Assertions.assertEquals(false, applicationDeployer.isStopped());
            Assertions.assertNotNull(DubboSpringInitializer.findBySpringContext(providerContext));

            // close spring context
            providerContext.close();

            // Expect 2: dubbo application will not be destroyed after closing spring context cause setKeepRunningOnSpringClosed(true)
            Assertions.assertEquals(DeployState.STARTED, applicationDeployer.getState());
            Assertions.assertEquals(true, applicationDeployer.isStarted());
            Assertions.assertEquals(false, applicationDeployer.isStopped());
            Assertions.assertNull(DubboSpringInitializer.findBySpringContext(providerContext));
        } finally {
            DubboBootstrap.getInstance().stop();
            SysProps.clear();
            if (providerContext != null) {
                providerContext.close();
            }
        }
    }
}