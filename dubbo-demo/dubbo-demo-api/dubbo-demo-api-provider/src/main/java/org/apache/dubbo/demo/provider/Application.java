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
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.demo.DemoService;

public class Application {

    private static final String REGISTRY_URL = "zookeeper://127.0.0.1:2181";

    public static void main(String[] args) {
        startWithBootstrap();
    }

    private static void startWithBootstrap() {
        // 代码方式启动dubbo服务
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();
        // 设置接口
        service.setInterface(DemoService.class);
        // 设置实际引用
        service.setRef(new DemoServiceImpl());
        // dubbo启动器
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        // 启动服务
        bootstrap.application(new ApplicationConfig("dubbo-demo-api-provider"))
            //设置注册中心
            .registry(new RegistryConfig(REGISTRY_URL))
            //设置协议
            .protocol(new ProtocolConfig(CommonConstants.DUBBO, -1))
            //设置服务
            .service(service)
            //启动服务
            .start()
            .await();
    }

}
