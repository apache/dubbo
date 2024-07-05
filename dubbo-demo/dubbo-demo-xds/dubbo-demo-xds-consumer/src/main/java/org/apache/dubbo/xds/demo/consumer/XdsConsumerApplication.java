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
package org.apache.dubbo.xds.demo.consumer;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.xds.demo.DemoService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

@SpringBootApplication
@Service
@EnableDubbo
public class XdsConsumerApplication {
    @DubboReference(providedBy = "dubbo-demo-xds-provider")
    private DemoService demoService;

    public static void main(String[] args) throws InterruptedException {
        // System.setProperty(IstioConstant.WORKLOAD_NAMESPACE_KEY, "dubbo-demo");
        // // System.setProperty("API_SERVER_PATH", "https://127.0.0.1:6443");
        // System.setProperty("SA_CA_PATH", "/Users/smzdm/hjf/xds/resources/ca.crt");
        // System.setProperty("SA_TOKEN_PATH", "/Users/smzdm/hjf/xds/resources/token");
        // System.setProperty("NAMESPACE", "dubbo-demo");
        // IstioConstant.KUBERNETES_SA_PATH = "/Users/smzdm/hjf/xds/resources/token";
        // System.setProperty(IstioConstant.PILOT_CERT_PROVIDER_KEY, "istiod");
        ConfigurableApplicationContext context = SpringApplication.run(XdsConsumerApplication.class, args);
        XdsConsumerApplication application = context.getBean(XdsConsumerApplication.class);
        while (true) {
            try {
                String result = application.doSayHello("world");
                System.out.println("result: " + result);

            } catch (Exception e) {
                e.printStackTrace();
            }
            Thread.sleep(2000);
        }
    }

    public String doSayHello(String name) {
        return demoService.sayHello(name);
    }
}
