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
package org.apache.dubbo.config.spring.isolation.spring.xml;

import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.spring.isolation.spring.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Map;

public class XmlIsolationTest extends BaseTest {

    @Test
    public void test() throws Exception {
        // start provider app
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext("META-INF/isolation/dubbo-provider.xml");
        providerContext.start();

        // start consumer app
        ClassPathXmlApplicationContext consumerContext = new ClassPathXmlApplicationContext("META-INF/isolation/dubbo-consumer.xml");
        consumerContext.start();

        // getAndSet serviceConfig
        setServiceConfig(providerContext);

        // assert isolation of executor
        assertExecutor(providerContext, consumerContext);

        // close context
        providerContext.close();
        consumerContext.close();
    }

    private void setServiceConfig(ClassPathXmlApplicationContext providerContext) {
        Map<String, ServiceConfig> serviceConfigMap = providerContext.getBeansOfType(ServiceConfig.class);
        serviceConfig1 = serviceConfigMap.get("org.apache.dubbo.config.spring.ServiceBean#0");
        serviceConfig2 = serviceConfigMap.get("org.apache.dubbo.config.spring.ServiceBean#1");
        serviceConfig3 = serviceConfigMap.get("org.apache.dubbo.config.spring.ServiceBean#2");
    }

}
