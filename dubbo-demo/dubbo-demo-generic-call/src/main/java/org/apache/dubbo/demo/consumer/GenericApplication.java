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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.rpc.service.GenericService;

import com.google.gson.Gson;

public class GenericApplication {

    public static void main(String[] args) {
        runWithBootstrap(args);
    }


    private static void runWithBootstrap(String[] args) {
        ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
        reference.setInterface("org.apache.dubbo.demo.DemoService");

        String param = "dubbo generic invoke";

        if (args.length > 0 && CommonConstants.GENERIC_SERIALIZATION_GSON.equals(args[0])) {
            reference.setGeneric(CommonConstants.GENERIC_SERIALIZATION_GSON);
            param = new Gson().toJson(param + " gson");
        } else {
            reference.setGeneric("true");
        }

        ApplicationConfig applicationConfig = new ApplicationConfig("demo-consumer");

        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        metadataReportConfig.setAddress("zookeeper://127.0.0.1:2181");

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap.application(applicationConfig)
            .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
            .protocol(new ProtocolConfig(CommonConstants.DUBBO, -1))
            .reference(reference)
            .start();

        // generic invoke
        GenericService genericService = bootstrap.getCache().get(reference);
        while (true) {
            try {
                Object genericInvokeResult = genericService.$invoke("sayHello", new String[]{String.class.getName()},
                    new Object[]{param});
                System.out.println(genericInvokeResult);
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
