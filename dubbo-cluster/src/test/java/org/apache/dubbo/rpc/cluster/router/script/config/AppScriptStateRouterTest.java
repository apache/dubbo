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
package org.apache.dubbo.rpc.cluster.router.script.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.governance.GovernanceRuleRepository;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_APPLICATION_KEY;

@DisabledForJreRange(min = JRE.JAVA_16)
public class AppScriptStateRouterTest {
    private static final String LOCAL_HOST = "127.0.0.1";
    private static final String RULE_SUFFIX = ".script-router";

    private static GovernanceRuleRepository ruleRepository;
    private URL url = URL.valueOf("dubbo://1.1.1.1/com.foo.BarService");
    private String rawRule = "---\n" +
        "configVersion: v3.0\n" +
        "key: demo-provider\n" +
        "type: javascript\n" +
        "script: |\n" +
        "  (function route(invokers,invocation,context) {\n" +
        "      var result = new java.util.ArrayList(invokers.size());\n" +
        "      for (i = 0; i < invokers.size(); i ++) {\n" +
        "          if (\"10.20.3.3\".equals(invokers.get(i).getUrl().getHost())) {\n" +
        "              result.add(invokers.get(i));\n" +
        "          }\n" +
        "      }\n" +
        "      return result;\n" +
        "  } (invokers, invocation, context)); // 表示立即执行方法\n" +
        "...";

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ruleRepository = Mockito.mock(GovernanceRuleRepository.class);
    }

    @Test
    void testConfigScriptRoute() {
        AppScriptStateRouter<String> router = new AppScriptStateRouter<>(url);
        router = Mockito.spy(router);
        Mockito.when(router.getRuleRepository()).thenReturn(ruleRepository);
        Mockito.when(ruleRepository.getRule("demo-provider" + RULE_SUFFIX, DynamicConfiguration.DEFAULT_GROUP)).thenReturn(rawRule);
//        Mockito.when(ruleRepository.addListener()).thenReturn();

        BitList<Invoker<String>> invokers = getInvokers();
        router.notify(invokers);

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("sayHello");
        List<Invoker<String>> result = router.route(invokers.clone(), url, invocation, false, new Holder<>());
        Assertions.assertEquals(1, result.size());
    }

    private BitList<Invoker<String>> getInvokers() {
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf(
            "dubbo://10.20.3.3:20880/com.foo.BarService?" + REMOTE_APPLICATION_KEY + "=demo-provider"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST
            + ":20880/com.foo.BarService?" + REMOTE_APPLICATION_KEY + "=demo-provider"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST
            + ":20880/com.foo.BarService?" + REMOTE_APPLICATION_KEY + "=demo-provider"));
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);
        return invokers;
    }

}
