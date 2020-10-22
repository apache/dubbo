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
package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.DemoService;
import org.apache.dubbo.rpc.support.Person;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.$INVOKE;
import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_NATIVE_JAVA;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class GenericFilterTest {
    GenericFilter genericFilter = new GenericFilter();

    @Test
    public void testInvokeWithDefault() throws Exception {

        Method genericInvoke = GenericService.class.getMethods()[0];

        Map<String, Object> person = new HashMap<String, Object>();
        person.put("name", "dubbo");
        person.put("age", 10);

        RpcInvocation invocation = new RpcInvocation($INVOKE, GenericService.class.getName(), "", genericInvoke.getParameterTypes(),
                new Object[]{"getPerson", new String[]{Person.class.getCanonicalName()}, new Object[]{person}});

        URL url = URL.valueOf("test://test:11/org.apache.dubbo.rpc.support.DemoService?" +
                "accesslog=true&group=dubbo&version=1.1");
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.invoke(any(Invocation.class))).thenReturn(AsyncRpcResult.newDefaultAsyncResult(new Person("person", 10), invocation));
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.getInterface()).thenReturn(DemoService.class);

        Result asyncResult = genericFilter.invoke(invoker, invocation);

        AppResponse appResponse = (AppResponse) asyncResult.get();
        genericFilter.onResponse(appResponse, invoker, invocation);
        Assertions.assertEquals(HashMap.class, appResponse.getValue().getClass());
        Assertions.assertEquals(10, ((HashMap) appResponse.getValue()).get("age"));

    }

    @Test
    public void testInvokeWithJavaException() throws Exception {
        Assertions.assertThrows(RpcException.class, () -> {
            Method genericInvoke = GenericService.class.getMethods()[0];

            Map<String, Object> person = new HashMap<String, Object>();
            person.put("name", "dubbo");
            person.put("age", 10);

            RpcInvocation invocation = new RpcInvocation($INVOKE, GenericService.class.getName(), "", genericInvoke.getParameterTypes(),
                    new Object[]{"getPerson", new String[]{Person.class.getCanonicalName()}, new Object[]{person}});
            invocation.setAttachment(GENERIC_KEY, GENERIC_SERIALIZATION_NATIVE_JAVA);

            URL url = URL.valueOf("test://test:11/org.apache.dubbo.rpc.support.DemoService?" +
                    "accesslog=true&group=dubbo&version=1.1");
            Invoker invoker = Mockito.mock(Invoker.class);
            when(invoker.invoke(any(Invocation.class))).thenReturn(new AppResponse(new Person("person", 10)));
            when(invoker.getUrl()).thenReturn(url);
            when(invoker.getInterface()).thenReturn(DemoService.class);

            genericFilter.invoke(invoker, invocation);
        });
    }

    @Test
    public void testInvokeWithMethodNamtNot$Invoke() {

        Method genericInvoke = GenericService.class.getMethods()[0];

        Map<String, Object> person = new HashMap<String, Object>();
        person.put("name", "dubbo");
        person.put("age", 10);

        RpcInvocation invocation = new RpcInvocation("sayHi", GenericService.class.getName(), "", genericInvoke.getParameterTypes()
                , new Object[]{"getPerson", new String[]{Person.class.getCanonicalName()}, new Object[]{person}});

        URL url = URL.valueOf("test://test:11/org.apache.dubbo.rpc.support.DemoService?" +
                "accesslog=true&group=dubbo&version=1.1");
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.invoke(any(Invocation.class))).thenReturn(new AppResponse(new Person("person", 10)));
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.getInterface()).thenReturn(DemoService.class);

        Result result = genericFilter.invoke(invoker, invocation);
        Assertions.assertEquals(Person.class, result.getValue().getClass());
        Assertions.assertEquals(10, ((Person) (result.getValue())).getAge());
    }

    @Test
    public void testInvokeWithMethodArgumentSizeIsNot3() {

        Method genericInvoke = GenericService.class.getMethods()[0];

        Map<String, Object> person = new HashMap<String, Object>();
        person.put("name", "dubbo");
        person.put("age", 10);

        RpcInvocation invocation = new RpcInvocation($INVOKE, GenericService.class.getName(), "", genericInvoke.getParameterTypes()
                , new Object[]{"getPerson", new String[]{Person.class.getCanonicalName()}});

        URL url = URL.valueOf("test://test:11/org.apache.dubbo.rpc.support.DemoService?" +
                "accesslog=true&group=dubbo&version=1.1");
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.invoke(any(Invocation.class))).thenReturn(new AppResponse(new Person("person", 10)));
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.getInterface()).thenReturn(DemoService.class);

        Result result = genericFilter.invoke(invoker, invocation);
        Assertions.assertEquals(Person.class, result.getValue().getClass());
        Assertions.assertEquals(10, ((Person) (result.getValue())).getAge());
    }

}
