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
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.demo.GreeterWrapperService;

import java.io.IOException;

public class ApiWrapperConsumer {
    public static void main(String[] args) throws IOException {
        ReferenceConfig<GreeterWrapperService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(GreeterWrapperService.class);
        referenceConfig.setCheck(false);
        referenceConfig.setProtocol("tri");
        referenceConfig.setLazy(true);

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap.application(new ApplicationConfig("dubbo-demo-triple-api-wrapper-consumer"))
            .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
            .protocol(new ProtocolConfig(CommonConstants.TRIPLE, -1))
            .reference(referenceConfig)
            .start();

        final GreeterWrapperService greeterWrapperService = referenceConfig.get();
        System.out.println("dubbo referenceConfig started");
        long st = System.currentTimeMillis();
        String reply = greeterWrapperService.sayHello("haha");
        // 4MB response
        System.out.println("Reply length:" + reply.length() + " cost:" + (System.currentTimeMillis() - st));
        System.in.read();
    }
}
