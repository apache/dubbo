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
package org.apache.dubbo.config.spring.samples;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.spring.api.Box;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.rpc.RpcContext;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.PropertySource;

import static java.lang.String.format;

/**
 * Zookeeper Dubbo Spring Provider Bootstrap
 *
 * @since 2.7.8
 */
@EnableDubbo
@PropertySource("classpath:/META-INF/service-introspection/zookeeper-dubbb-provider.properties")
public class ZookeeperDubboSpringProviderBootstrap {

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(ZookeeperDubboSpringProviderBootstrap.class);
        System.in.read();
        context.close();
    }
}

@DubboService
class DefaultDemoService implements DemoService {

    @Override
    public String sayName(String name) {
        RpcContext rpcContext = RpcContext.getContext();
        return format("[%s:%s] Say - %s", rpcContext.getLocalHost(), rpcContext.getLocalPort(), name);
    }

    @Override
    public Box getBox() {
        return null;
    }
}
