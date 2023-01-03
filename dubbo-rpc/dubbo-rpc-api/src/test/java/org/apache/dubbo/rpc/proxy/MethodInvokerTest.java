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
package org.apache.dubbo.rpc.proxy;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MethodInvokerTest {

    @Test
    void testNewInstance() throws Throwable {
        String singleMethodName = "getThreadName";
        String overloadMethodName = "sayHello";

        MethodInvoker methodInvoker = MethodInvoker.newInstance(RemoteServiceImpl.class);
        assertInstanceOf(MethodInvoker.CompositeMethodInvoker.class, methodInvoker);
        MethodInvoker.CompositeMethodInvoker compositeMethodInvoker = (MethodInvoker.CompositeMethodInvoker) methodInvoker;
        Map<String, MethodInvoker> invokers = compositeMethodInvoker.getInvokers();
        MethodInvoker getThreadNameMethodInvoker = invokers.get(singleMethodName);
        assertInstanceOf(MethodInvoker.SingleMethodInvoker.class, getThreadNameMethodInvoker);
        MethodInvoker sayHelloMethodInvoker = invokers.get(overloadMethodName);
        assertInstanceOf(MethodInvoker.OverloadMethodInvoker.class, sayHelloMethodInvoker);

        RemoteServiceImpl remoteService = Mockito.mock(RemoteServiceImpl.class);
        //invoke success, SingleMethodInvoker does not check parameter types
        methodInvoker.invoke(remoteService, singleMethodName, new Class[0], new Object[0]);
        methodInvoker.invoke(remoteService, singleMethodName, new Class[]{Object.class, Object.class, Object.class}, new Object[0]);
        Mockito.verify(remoteService, Mockito.times(2)).getThreadName();

        methodInvoker.invoke(remoteService, overloadMethodName, new Class[]{String.class}, new Object[]{"Hello arg1"});
        Mockito.verify(remoteService, Mockito.times(1)).sayHello("Hello arg1");
        methodInvoker.invoke(remoteService, overloadMethodName, new Class[]{String.class, String.class}, new Object[]{"Hello arg1", "Hello arg2"});
        Mockito.verify(remoteService, Mockito.times(1)).sayHello("Hello arg1", "Hello arg2");
    }
}
