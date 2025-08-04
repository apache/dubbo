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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@Service
@EnableDubbo
public class XdsConsumerApplication {
    private static final Logger logger = LoggerFactory.getLogger(XdsConsumerApplication.class);

    @DubboReference(providedBy = "dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local:50051")
    private DemoService demoService;
    
    public static void main(String[] args) throws InterruptedException {
        // System.setProperty(IstioConstant.WORKLOAD_NAMESPACE_KEY, "dubbo-demo");
        // // System.setProperty("API_SERVER_PATH", "https://127.0.0.1:6443");
        // System.setProperty("SA_CA_PATH", "/Users/smzdm/hjf/xds/resources/ca.crt");
        // System.setProperty("SA_TOKEN_PATH", "/Users/smzdm/hjf/xds/resources/token");
        // System.setProperty("NAMESPACE", "dubbo-demo");
        // IstioConstant.KUBERNETES_SA_PATH = "/Users/smzdm/hjf/xds/resources/token";
        // System.setProperty(IstioConstant.PILOT_CERT_PROVIDER_KEY, "istiod");

        // System.setProperty("GRPC_XDS_BOOTSTRAP",
        // "/Users/hejianfei/code/server/dubbo/dubbo-demo/dubbo-demo-xds/dubbo-demo-xds-consumer/src/main/resources/bootstrap.json");

        ConfigurableApplicationContext context = SpringApplication.run(XdsConsumerApplication.class, args);
        XdsConsumerApplication application = context.getBean(XdsConsumerApplication.class);
        
        while (true) {
            try {
                Thread.sleep(10000);
                logger.info("Attempting to call demoService.sayHello...");
                String result = application.doSayHello("world");
                logger.info("Call successful, result: {}", result);
            } catch (Exception e) {
                logger.error("Error calling service", e);
            }
        }
    }

    public String doSayHello(String name) {
        logger.info("doSayHello called with name: {}", name);
        return demoService.sayHello(name);
    }
}
