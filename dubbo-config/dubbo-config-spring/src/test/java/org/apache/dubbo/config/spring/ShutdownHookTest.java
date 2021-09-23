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

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.context.DubboSpringInitCustomizerHolder;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ShutdownHookTest {

    @Test
    public void testDisableShutdownHook(){

        // set KeepRunningOnSpringClosed flag for next spring context
        DubboSpringInitCustomizerHolder.get().addCustomizer(context-> {
            context.setKeepRunningOnSpringClosed(true);
        });

        try {
            ClassPathXmlApplicationContext providerContext;
            String resourcePath = "org/apache/dubbo/config/spring";
            providerContext = new ClassPathXmlApplicationContext(
                resourcePath + "/demo-provider.xml",
                resourcePath + "/demo-provider-properties.xml");
            providerContext.start();

            ModuleModel moduleModel = providerContext.getBean(ModuleModel.class);
            Assertions.assertTrue(moduleModel.getDeployer().isStarted());
            Assertions.assertEquals(true, DubboBootstrap.getInstance().isStarted());
            Assertions.assertEquals(false, DubboBootstrap.getInstance().isStopped());

            // close spring context
            providerContext.close();

            // expect dubbo bootstrap will not be destroyed after closing spring context
            Assertions.assertEquals(true, DubboBootstrap.getInstance().isStarted());
            Assertions.assertEquals(false, DubboBootstrap.getInstance().isStopped());
        } finally {
            SysProps.clear();
        }
    }
}
