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
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ServiceModel;
import org.apache.dubbo.rpc.support.DemoService;
import org.apache.dubbo.rpc.support.MyInvoker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URLClassLoader;

class ClassLoaderFilterTest {

    private ClassLoaderFilter classLoaderFilter = new ClassLoaderFilter();

    @Test
    void testInvoke() throws Exception {
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1");

        String path = DemoService.class.getResource("/").getPath();
        final URLClassLoader cl = new URLClassLoader(new java.net.URL[]{new java.net.URL("file:" + path)}) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                try {
                    return findClass(name);
                } catch (ClassNotFoundException e) {
                    return super.loadClass(name);
                }
            }
        };
        final Class<?> clazz = cl.loadClass(DemoService.class.getCanonicalName());
        Invoker invoker = new MyInvoker(url) {
            @Override
            public Class getInterface() {
                return clazz;
            }

            @Override
            public Result invoke(Invocation invocation) throws RpcException {
                Assertions.assertEquals(cl, Thread.currentThread().getContextClassLoader());
                return null;
            }
        };
        Invocation invocation = Mockito.mock(Invocation.class);
        ServiceModel serviceModel = Mockito.mock(ServiceModel.class);
        Mockito.when(serviceModel.getClassLoader()).thenReturn(cl);
        Mockito.when(invocation.getServiceModel()).thenReturn(serviceModel);

        classLoaderFilter.invoke(invoker, invocation);
    }
}
