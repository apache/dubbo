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
package com.apache.dubbo.demo.graalvm.consumer;

import org.apace.dubbo.graalvm.demo.DemoService;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.*;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.context.ModuleConfigManager;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.HashMap;
import java.util.Map;

public class Application {

    private static final String REGISTRY_URL = "zookeeper://127.0.0.1:2181";

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        System.setProperty("dubbo.application.logger", "log4j");
        System.setProperty("native", "true");
        System.setProperty("dubbo.json-framework.prefer", "fastjson");
        if (isClassic(args)) {
            runWithRefer();
        } else {
            runWithBootstrap();
        }
    }

    private static boolean isClassic(String[] args) {
        return args.length > 0 && "classic".equalsIgnoreCase(args[0]);
    }

    private static void runWithBootstrap() {
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();

        ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-demo-api-consumer");
        applicationConfig.setQosEnable(false);
        applicationConfig.setCompiler("jdk");
        Map<String, String> m = new HashMap<>(1);
        m.put("proxy", "jdk");
        applicationConfig.setParameters(m);

        ReferenceConfig<DemoService> reference = new ReferenceConfig<>();
        reference.setInterface(DemoService.class);
        reference.setGeneric("false");

        ProtocolConfig protocolConfig = new ProtocolConfig(CommonConstants.DUBBO, -1);
        protocolConfig.setSerialization("fastjson2");
        bootstrap.application(applicationConfig)
            .registry(new RegistryConfig(REGISTRY_URL))
            .protocol(protocolConfig)
            .reference(reference)
            .start();

        DemoService demoService = bootstrap.getCache().get(reference);
        String message = demoService.sayHello("Native");
        logger.info(message);
    }

    private static void runWithRefer() {
        FrameworkModel frameworkModel = new FrameworkModel();

        ApplicationModel appModel = frameworkModel.newApplication();

        ModuleModel moduleModel = appModel.newModule();

        ConfigManager appConfigManager = appModel.getApplicationConfigManager();
        appConfigManager.setApplication(new ApplicationConfig("dubbo-demo-api-consumer-app-1"));
        appConfigManager.addRegistry(new RegistryConfig(REGISTRY_URL));

        Map<String, String> params = new HashMap<>(1);
        params.put("proxy", "jdk");
        appConfigManager.getApplication().ifPresent(applicationConfig -> applicationConfig.setParameters(params));

        ModuleConfigManager moduleConfigManager = moduleModel.getConfigManager();
        moduleConfigManager.setModule(new ModuleConfig("dubbo-demo-api-consumer-app-1-module-1"));

        ReferenceConfig<DemoService> reference = new ReferenceConfig<>();
        reference.setScopeModel(moduleModel);
        reference.setProtocol("dubbo");
        reference.setInterface(DemoService.class);
        reference.setGeneric("false");

        moduleConfigManager.addConfig(reference);

        DemoService service = reference.get();
        String message = service.sayHello("dubbo");
        logger.info(message);
    }
}
