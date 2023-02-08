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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.mockito.Mockito.when;

class InvokerInvocationHandlerTest {

    private Invoker<?> invoker;
    private InvokerInvocationHandler invokerInvocationHandler;

    @BeforeEach
    public void setUp() {
        URL url = URL.valueOf("mock://localhost:8080/FooService?group=mock&version=1.0.0");
        invoker = Mockito.mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        invokerInvocationHandler = new InvokerInvocationHandler(invoker);
    }

    @Test
    void testInvokeToString() throws Throwable {
        String methodName = "toString";

        when(invoker.toString()).thenReturn(methodName);
        Method method = invoker.getClass().getMethod(methodName);

        Object result = invokerInvocationHandler.invoke(null, method, new Object[]{});
        Assertions.assertEquals(methodName, result);
    }

}
