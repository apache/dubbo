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
package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.HashMap;
import java.util.Map;

public class GenericApplication {
    public static void main(String[] args) {
        if (isClassic(args)) {
//            runWithRefer();
        } else {
            runWithBootstrap();
        }
    }

    private static boolean isClassic(String[] args) {
        return args.length > 0 && "classic".equalsIgnoreCase(args[0]);
    }

    private static void runWithBootstrap() {
        ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
        reference.setInterface("org.apache.dubbo.demo.DemoService");
        reference.setGeneric("true");

        ApplicationConfig applicationConfig = new ApplicationConfig("demo-consumer");
        Map<String, String> parameters = new HashMap<>();
        applicationConfig.setParameters(parameters);

        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        metadataReportConfig.setAddress("zookeeper://127.0.0.1:2181");

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap.application(applicationConfig)
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .reference(reference)
                .start();

        // generic invoke
        GenericService genericService = (GenericService) ReferenceConfigCache.getCache().get(reference);
        while (true) {
            try {
                Object genericInvokeResult = genericService.$invoke("sayHello", new String[]{String.class.getName()},
                        new Object[]{"dubbo generic invoke"});
                System.out.println(genericInvokeResult);
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

//    private static void runWithRefer() {
//        ReferenceConfig<DemoService> reference = new ReferenceConfig<>();
//        reference.setApplication(new ApplicationConfig("dubbo-demo-api-consumer"));
//        reference.setRegistry(new RegistryConfig("zookeeper://127.0.0.1:2181"));
//        reference.setInterface(DemoService.class);
//        DemoService service = reference.get();
//        String message = service.sayHello("dubbo");
//        System.out.println(message);
//    }
}
