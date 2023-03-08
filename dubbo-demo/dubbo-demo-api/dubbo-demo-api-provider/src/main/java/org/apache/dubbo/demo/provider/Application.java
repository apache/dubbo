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
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.demo.DemoService;

import java.util.concurrent.CountDownLatch;

public class Application {
    public static void main(String[] args) throws Exception {
        // 经典模式
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
            .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
            .protocol(new ProtocolConfig(CommonConstants.DUBBO, -1))
            .service(service)
            .start()
            .await();
    }

    /**
     * 思考:
     * <p>
     * - ServiceConfig 到底是什么?
     * <p>
     * - 对于dubbo来说, Service是什么?
     * <p>
     * 答:
     * <p>
     * Service被定义成一个dubbo服务,每个服务可以提供多个接口.
     * <p>
     * 所以ServiceConfig顾名思义,就是针对当前Service的配置信息.
     */
    private static void startWithExport() throws InterruptedException {
        // 泛型里是当前service的具体实现
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();
        // 设置要暴露的接口
        service.setInterface(DemoService.class);
        // 设置暴露接口的实现类
        service.setRef(new DemoServiceImpl());
        // 配置暴露服务的名称
        service.setApplication(new ApplicationConfig("dubbo-demo-api-provider"));
        // 配置注册中心
        service.setRegistry(new RegistryConfig("zookeeper://127.0.0.1:2181"));
        // 配置元数据上报,dubbo服务实例启动之后,肯定有自己的元数据,必须上报到zk上去
        service.setMetadataReportConfig(new MetadataReportConfig("zookeeper://127.0.0.1:2181"));

        // 配置了服务的接口、实现类、服务名、注册中心、元数据之后，进行服务暴露
        // 我们先推断: 服务暴露后,此时服务提供方会初始化并启动一个网络监听程序.
        // 网络监听程序作用: 当服务消费端调用暴露的服务时,需要跟服务提供端建立网络连接后进行通信. 根据数据传输协议,发送请求数据,执行rpc调用.
        // TODO: 2023/3/8 在这里debug,查看服务暴露的原理
        service.export();

        System.out.println("dubbo service started");
        new CountDownLatch(1).await();
    }
}
