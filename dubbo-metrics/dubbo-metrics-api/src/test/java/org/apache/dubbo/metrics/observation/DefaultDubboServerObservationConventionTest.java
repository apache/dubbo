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
package org.apache.dubbo.metrics.observation;

import org.apache.dubbo.metrics.observation.utils.ObservationConventionUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;

import io.micrometer.common.KeyValues;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
public class DefaultDubboServerObservationConventionTest {

    static DubboServerObservationConvention dubboServerObservationConvention =
            DefaultDubboServerObservationConvention.getInstance();

    @Test
    void testGetName() {
        Assertions.assertEquals("rpc.server.duration", dubboServerObservationConvention.getName());
    }

    @Test
    void testGetLowCardinalityKeyValues() throws NoSuchFieldException, IllegalAccessException {
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("testMethod");
        invocation.setAttachment("interface", "com.example.TestService");
        invocation.setTargetServiceUniqueName("targetServiceName1");

        Invoker<?> invoker = ObservationConventionUtils.getMockInvokerWithUrl();
        invocation.setInvoker(invoker);

        DubboServerContext context = new DubboServerContext(invoker, invocation);

        KeyValues keyValues = dubboServerObservationConvention.getLowCardinalityKeyValues(context);

        Assertions.assertEquals("testMethod", ObservationConventionUtils.getValueForKey(keyValues, "rpc.method"));
        Assertions.assertEquals(
                "targetServiceName1", ObservationConventionUtils.getValueForKey(keyValues, "rpc.service"));
        Assertions.assertEquals("apache_dubbo", ObservationConventionUtils.getValueForKey(keyValues, "rpc.system"));
    }

    @Test
    void testGetContextualName() {
        RpcInvocation invocation = new RpcInvocation();
        Invoker<?> invoker = ObservationConventionUtils.getMockInvokerWithUrl();
        invocation.setMethodName("testMethod");
        invocation.setServiceName("com.example.TestService");

        DubboClientContext context = new DubboClientContext(invoker, invocation);

        DefaultDubboClientObservationConvention convention = new DefaultDubboClientObservationConvention();

        String contextualName = convention.getContextualName(context);
        Assertions.assertEquals("com.example.TestService/testMethod", contextualName);
    }
}
