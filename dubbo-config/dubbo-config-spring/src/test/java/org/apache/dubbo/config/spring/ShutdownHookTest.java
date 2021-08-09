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

import org.apache.dubbo.config.ConfigKeys;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ShutdownHookTest {

    @Test
    public void testDisableShutdownHook(){
        SysProps.setProperty(ConfigKeys.DUBBO_LIFECYCLE_DISABLE_SHUTDOWN_HOOK, "true");

        try {
            ClassPathXmlApplicationContext providerContext;
            String resourcePath = "org/apache/dubbo/config/spring";
            providerContext = new ClassPathXmlApplicationContext(
                resourcePath + "/demo-provider.xml",
                resourcePath + "/demo-provider-properties.xml");
            providerContext.start();

            Assertions.assertEquals(true, DubboBootstrap.getInstance().isStarted());
            Assertions.assertEquals(false, DubboBootstrap.getInstance().isShutdown());

            // close spring context
            providerContext.close();

            // expect dubbo bootstrap will not be destroyed after closing spring context
            Assertions.assertEquals(true, DubboBootstrap.getInstance().isStarted());
            Assertions.assertEquals(false, DubboBootstrap.getInstance().isShutdown());
        } finally {
            SysProps.clear();
        }
    }
}
