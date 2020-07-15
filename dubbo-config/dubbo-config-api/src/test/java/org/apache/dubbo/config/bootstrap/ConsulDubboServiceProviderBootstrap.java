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

import org.apache.dubbo.config.bootstrap.rest.UserService;
import org.apache.dubbo.config.bootstrap.rest.UserServiceImpl;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;

/**
 * TODO
 */
public class ConsulDubboServiceProviderBootstrap {

    public static void main(String[] args) {
        DubboBootstrap.getInstance()
                .application("consul-dubbo-provider", app -> app.metadata(DEFAULT_METADATA_STORAGE_TYPE))
                .registry(builder -> builder.address("consul://127.0.0.1:8500?registry-type=service")
                        .useAsConfigCenter(true)
                        .useAsMetadataCenter(true))
                .protocol("dubbo", builder -> builder.port(-1).name("dubbo"))
                .protocol("rest", builder -> builder.port(8081).name("rest"))
                .service("echo", builder -> builder.interfaceClass(EchoService.class).ref(new EchoServiceImpl()).protocolIds("dubbo"))
                .service("user", builder -> builder.interfaceClass(UserService.class).ref(new UserServiceImpl()).protocolIds("rest"))
                .start()
                .await();
    }
}
