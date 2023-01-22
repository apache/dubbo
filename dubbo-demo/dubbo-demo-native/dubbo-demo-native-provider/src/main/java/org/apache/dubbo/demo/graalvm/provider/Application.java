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
package org.apache.dubbo.demo.graalvm.provider;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;

import org.apace.dubbo.graalvm.demo.DemoService;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Application {

    public static void main(String[] args) throws Exception {
        System.setProperty("dubbo.application.logger", "log4j");
        System.setProperty("native", "true");
        System.setProperty("dubbo.json-framework.prefer", "fastjson");
        if (isClassic(args)) {
            startWithExport();
        } else {
            startWithBootstrap();
        }
        System.in.read();
    }

    private static boolean isClassic(String[] args) {
        return args.length > 0 && "classic".equalsIgnoreCase(args[0]);
    }

    private static void startWithBootstrap() {
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();

        ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-demo-api-provider");
        applicationConfig.setQosEnable(false);
        applicationConfig.setCompiler("jdk");
        Map<String, String> m = new HashMap<>(1);
        m.put("proxy", "jdk");
        applicationConfig.setParameters(m);

        ServiceConfig<DemoService> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        ProtocolConfig protocolConfig = new ProtocolConfig(CommonConstants.DUBBO, -1);
        protocolConfig.setSerialization("fastjson2");
        bootstrap.application(applicationConfig)
            .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
            .protocol(protocolConfig)
            .service(service)
            .start()
            .await();

        System.out.println("dubbo service started");
    }

    private static void startWithExport() throws InterruptedException {
        ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-demo-api-provider");
        applicationConfig.setQosEnable(false);
        applicationConfig.setCompiler("jdk");

        Map<String, String> m = new HashMap<>(1);
        m.put("proxy", "jdk");
        applicationConfig.setParameters(m);

        ModuleModel moduleModel = applicationConfig.getApplicationModel().newModule();


        ServiceConfig<DemoService> service = new ServiceConfig<>(moduleModel);
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        service.setApplication(applicationConfig);
        service.setRegistry(new RegistryConfig("zookeeper://127.0.0.1:2181"));
        service.export();

        System.out.println("dubbo service started");
        new CountDownLatch(1).await();
    }
}
