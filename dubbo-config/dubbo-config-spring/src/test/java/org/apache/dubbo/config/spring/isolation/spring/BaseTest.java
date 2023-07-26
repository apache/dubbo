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
package org.apache.dubbo.config.spring.isolation.spring;

import org.apache.dubbo.common.threadlocal.NamedInternalThreadFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.threadpool.manager.IsolationExecutorRepository;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.isolation.spring.support.DemoServiceExecutor;
import org.apache.dubbo.config.spring.isolation.spring.support.HelloServiceExecutor;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseTest {

    protected ServiceConfig serviceConfig1;
    protected ServiceConfig serviceConfig2;
    protected ServiceConfig serviceConfig3;

    @Test
    public void test() throws Exception {
        test();
    }

    protected void assertExecutor(ApplicationContext providerContext, ApplicationContext consumerContext) {

        // find configured "executor-demo-service" executor
        Map<String, DemoServiceExecutor> beansOfType1 = providerContext.getBeansOfType(DemoServiceExecutor.class);
        ThreadPoolExecutor executor1 = beansOfType1.get("executor-demo-service");
        NamedThreadFactory threadFactory1 = (NamedThreadFactory) executor1.getThreadFactory();

        // find configured "executor-hello-service" executor
        Map<String, HelloServiceExecutor> beansOfType2 = providerContext.getBeansOfType(HelloServiceExecutor.class);
        ThreadPoolExecutor executor2 = beansOfType2.get("executor-hello-service");
        NamedThreadFactory threadFactory2 = (NamedThreadFactory) executor2.getThreadFactory();

        // Verify that the executor is the previously configured
        Map<String, ApplicationModel> applicationModelMap = providerContext.getBeansOfType(ApplicationModel.class);
        ApplicationModel applicationModel = applicationModelMap.get(ApplicationModel.class.getName());
        ExecutorRepository repository = ExecutorRepository.getInstance(applicationModel);
        Assertions.assertTrue(repository instanceof IsolationExecutorRepository);
        Assertions.assertEquals(executor1, repository.getExecutor(serviceConfig1.toUrl()));
        Assertions.assertEquals(executor2, repository.getExecutor(serviceConfig2.toUrl()));
        // the default executor of serviceConfig3 is built using the threadpool parameter of the protocol
        ThreadPoolExecutor executor3 = (ThreadPoolExecutor) repository.getExecutor(serviceConfig3.toUrl());
        Assertions.assertTrue(executor3.getThreadFactory() instanceof NamedInternalThreadFactory);
        NamedInternalThreadFactory threadFactory3 = (NamedInternalThreadFactory) executor3.getThreadFactory();

        // rpc invoke with dubbo protocol
        DemoService demoServiceV1 = consumerContext.getBean("dubbo-demoServiceV1", DemoService.class);
        HelloService helloServiceV2 = consumerContext.getBean("dubbo-helloServiceV2", HelloService.class);
        HelloService helloServiceV3 = consumerContext.getBean("dubbo-helloServiceV3", HelloService.class);
        rpcInvoke(demoServiceV1, helloServiceV2, helloServiceV3);

        // rpc invoke with tri protocol
        demoServiceV1 = consumerContext.getBean("tri-demoServiceV1", DemoService.class);
        helloServiceV2 = consumerContext.getBean("tri-helloServiceV2", HelloService.class);
        helloServiceV3 = consumerContext.getBean("tri-helloServiceV3", HelloService.class);
        rpcInvoke(demoServiceV1, helloServiceV2, helloServiceV3);

        // Verify that when the provider accepts different service requests,
        // whether to use the respective executor(threadFactory) of different services to create threads
        AtomicInteger threadNum1 = threadFactory1.getThreadNum();
        AtomicInteger threadNum2 = threadFactory2.getThreadNum();
        AtomicInteger threadNum3 = threadFactory3.getThreadNum();
        Assertions.assertEquals(threadNum1.get(), 11);
        Assertions.assertEquals(threadNum2.get(), 101);
        Assertions.assertEquals(threadNum3.get(), 201);
    }

    private void rpcInvoke(DemoService demoServiceV1, HelloService helloServiceV2, HelloService helloServiceV3) {
        for (int i = 0; i < 250; i++) {
            demoServiceV1.sayName("name");
        }
        for (int i = 0; i < 250; i++) {
            helloServiceV2.sayHello("hello");
        }
        for (int i = 0; i < 250; i++) {
            helloServiceV3.sayHello("hello");
        }
    }

}
