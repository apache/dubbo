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
package com.alibaba.dubbo.common.bytecode;

import junit.framework.TestCase;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ProxyTest extends TestCase {
    public void testMain() throws Exception {
        Proxy proxy = Proxy.getProxy(ITest.class, ITest.class);
        ITest instance = (ITest) proxy.newInstance(new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("getName".equals(method.getName())) {
                    assertEquals(args.length, 0);
                } else if ("setName".equals(method.getName())) {
                    assertEquals(args.length, 2);
                    assertEquals(args[0], "qianlei");
                    assertEquals(args[1], "hello");
                }
                return null;
            }
        });

        System.out.println(instance.getName());
        assertNull(instance.getName());
        instance.setName("qianlei", "hello");
    }

    @Test
    public void testCglibProxy() throws Exception {
        ITest test = (ITest) Proxy.getProxy(ITest.class).newInstance(new InvocationHandler() {

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                System.out.println(method.getName());
                return null;
            }
        });

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(test.getClass());
        enhancer.setCallback(new MethodInterceptor() {

            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                return "123";
            }
        });
        try {
            ITest test1 = (ITest) enhancer.create();
            assertEquals(test1.getName(), "123");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testTwoClass() throws Exception {
        Proxy proxy = Proxy.getProxy(ITest1.class, ITest2.class);
        ITest1 instance = (ITest1) proxy.newInstance(new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("show1".equals(method.getName())) {
                    return "show1";
                } else if ("show2".equals(method.getName())) {
                    return "show2";
                }
                return null;
            }
        });

        assertEquals(instance.show1(), "show1");
        System.out.println(instance.getClass().getSuperclass());
        System.out.println(instance.getClass().getInterfaces().toString());
    }

    public static interface ITest {
        String getName();

        void setName(String name, String name2);
    }

    interface ITest1 {
        String show1();
    }

    interface ITest2 {
        String show2();
    }
}