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
package org.apache.dubbo.config.bootstrap;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.bootstrap.rest.UserService;

/**
 * Dubbo Provider Bootstrap
 *
 * @since 2.7.5
 */
public class NacosDubboServiceConsumerBootstrap {

    public static void main(String[] args) throws Exception {

        ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-nacos-consumer-demo");
//        applicationConfig.setMetadataType("remote");
        DubboBootstrap bootstrap = DubboBootstrap.getInstance()
                .application(applicationConfig)
                // Zookeeper
//                .registry("nacos", builder -> builder.address("nacos://127.0.0.1:8848?registry.type=service&subscribed.services=dubbo-nacos-provider-demo"))
//                .registry("nacos", builder -> builder.address("nacos://127.0.0.1:8848?registry-type=service&subscribed-services=dubbo-nacos-provider-demo"))
                .registry("nacos", builder -> builder.address("nacos://127.0.0.1:8848?registry-type=service&subscribed-services=dubbo-nacos-provider-demo"))
                .metadataReport(new MetadataReportConfig("nacos://127.0.0.1:8848"))
                .reference("user", builder -> builder.interfaceClass(UserService.class).protocol("rest"))
                .start();

        UserService userService = bootstrap.getCache().get(UserService.class);

        for (int i = 0; i < 500; i++) {
            Thread.sleep(2000L);
            System.out.println(userService.getUser(i * 1L));
        }
    }
}
