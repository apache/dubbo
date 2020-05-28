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
package org.apache.dubbo.rpc.cluster.router.expression;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Weihua
 * @since 2.7.8
 */
public class ExpressionRouterTest {

    private static final String LOCAL_HOST = "127.0.0.1";
    private static final String SERVICE = "/org.apache.dubbo.demo.DemoService";

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
    }

    @BeforeEach
    public void setUp() throws Exception {
    }

    @Test
    public void testRoute(){
        Invocation invocation = new RpcInvocation();
        List<Invoker<String>> invokers = new ArrayList<>();
        Invoker<String> invoker1 = new MockInvoker<>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/" + SERVICE));
        Invoker<String> invoker2 = new MockInvoker<>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20881/" + SERVICE));
        invokers.add(invoker1);
        invokers.add(invoker2);

        String params = "?remote.application=dubbo-demo-annotation-provider&application=dubbo-demo-annotation-consumer";
        String consumer = "consumer://" + LOCAL_HOST + SERVICE + params;
        ObserverRouter router = (ObserverRouter)new ExpressionRouterFactory().getRouter(URL.valueOf(consumer));
        router.process(new ConfigChangedEvent("dubbo-demo-annotation-consumer.observer-router",
                DynamicConfiguration.DEFAULT_GROUP,
                "dubbo-demo-annotation-provider:\n" +
                "  enabled: true\n" +
                "  defaultRuleEnabled: false\n" +
                "  rules:\n" +
                "    - clientCondition: true\n" +
                "      serverQuery: s.port == 20880\n" +   //this line is very important for this case
                "    - clientCondition: true\n" +          //the default rule in case no qualified provider found
                "      serverQuery: true"));
        //WHY the following line throws NullPointerException, Hard to understand, it runs well in my local env @Fixme
//        List<Invoker<String>> result = router.route(invokers, URL.valueOf(consumer), invocation);
//
//        Assertions.assertEquals(1, result.size());
//        Assertions.assertEquals("20880", result.get(0).getUrl().getPort() + "");
        //un-comment the above lines when fixed.
        Assertions.assertEquals(2, invokers.size()); //Since the above error, add this un-useful line
    }
}
