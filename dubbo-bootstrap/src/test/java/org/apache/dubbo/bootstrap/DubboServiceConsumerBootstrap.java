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
package org.apache.dubbo.bootstrap;

import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.context.ConfigManager;


/**
 * Dubbo Provider Bootstrap
 *
 * @since 2.7.4
 */
public class DubboServiceConsumerBootstrap {

    public static void main(String[] args) throws Exception {

        new DubboBootstrap()
                .application("dubbo-consumer-demo")
                // Zookeeper
                .registry("zookeeper", builder -> builder.address("zookeeper://127.0.0.1:2181?registry-type=service&subscribed-services=dubbo-provider-demo"))
                // Nacos
//                .registry("nacos", builder -> builder.address("nacos://127.0.0.1:8848?registry-type=service&subscribed-services=dubbo-provider-demo"))
                .reference("ref", builder -> builder.interfaceClass(EchoService.class))
                .onlyRegisterProvider(true)
                .start()
                .await();

        ConfigManager configManager = ConfigManager.getInstance();

        ReferenceConfig<EchoService> referenceConfig = configManager.getReferenceConfig("ref");

        EchoService echoService = referenceConfig.get();

        for (int i = 0; i < 500; i++) {
            Thread.sleep(2000L);
            System.out.println(echoService.echo("Hello,World"));
        }

    }
}
