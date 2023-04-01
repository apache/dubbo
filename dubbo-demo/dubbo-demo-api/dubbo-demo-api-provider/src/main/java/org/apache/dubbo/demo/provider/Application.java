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
package org.apache.dubbo.demo.provider;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.*;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.context.ModuleConfigManager;
import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.concurrent.CountDownLatch;

public class Application {

    private static final String REGISTRY_URL = "zookeeper://127.0.0.1:2181";

    private static final String METADATA_REPORT_URL = "zookeeper://127.0.0.1:2181";


    public static void main(String[] args) throws Exception {
        if (isClassic(args)) {
            startWithExport();
        } else {
            startWithBootstrap();
        }
    }

    private static boolean isClassic(String[] args) {
        return args.length > 0 && "classic".equalsIgnoreCase(args[0]);
    }

    private static void startWithBootstrap() {
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap.application(new ApplicationConfig("dubbo-demo-api-provider"))
            .registry(new RegistryConfig(REGISTRY_URL))
            .protocol(new ProtocolConfig(CommonConstants.DUBBO, -1))
            .service(service)
            .start()
            .await();
    }

    private static void startWithExport() throws InterruptedException {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();

        RegistryConfig registryConfig = new RegistryConfig(REGISTRY_URL);
        MetadataReportConfig metadataReportConfig = new MetadataReportConfig(METADATA_REPORT_URL);
        ProtocolConfig protocolConfig = new ProtocolConfig(CommonConstants.DUBBO, -1);

        final String registryId = "registry-1";
        final String metadataId = "metadata-1";
        registryConfig.setId(registryId);
        metadataReportConfig.setId(metadataId);

        ConfigManager appConfigManager = applicationModel.getApplicationConfigManager();
        appConfigManager.setApplication(new ApplicationConfig("dubbo-demo-api-provider-app-1"));
        appConfigManager.addRegistry(registryConfig);
        appConfigManager.addMetadataReport(metadataReportConfig);
        appConfigManager.addProtocol(protocolConfig);

        ModuleConfigManager moduleConfigManager = moduleModel.getConfigManager();
        moduleConfigManager.setModule(new ModuleConfig("dubbo-demo-api-provider-app-1-module-1"));

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setScopeModel(moduleModel);
        serviceConfig.setProtocol(protocolConfig);
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());

        moduleConfigManager.addConfig(serviceConfig);

        serviceConfig.export();

        new CountDownLatch(1).await();
    }

}
