/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.dubbo.demo.provider;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.demo.provider.rest.UserService;
import org.apache.dubbo.demo.provider.rest.UserServiceImpl;

public class Application {
    public static void main(String[] args) throws Exception {
        ServiceConfig<UserService> service = new ServiceConfig<>();
        service.setInterface(UserService.class);
        service.setRef(new UserServiceImpl());

        ProtocolConfig protocolConfig = new ProtocolConfig("rest");
        protocolConfig.setPort(8090);

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap.application(new ApplicationConfig("dubbo-provider-for-sc"))
                .registry(new RegistryConfig("consul://127.0.0.1:8500?registry-type=service"))
                .protocol(protocolConfig)
                .service(service)
                .start()
                .await();
    }
}
