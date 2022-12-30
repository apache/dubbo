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
package org.apache.dubbo.rpc.proxy.bytebuddy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

class ByteBuddyInterceptorTest {

    @AfterEach
    public void after(){
        Mockito.clearAllCaches();
    }

    @Test
    void testIntercept() throws Throwable {
        InvocationHandler handler = Mockito.mock(InvocationHandler.class);
        ByteBuddyInterceptor interceptor = new ByteBuddyInterceptor(handler);
        Method method = Mockito.mock(Method.class);
        Proxy proxy = Mockito.mock(Proxy.class);
        Object[] args = new Object[0];
        interceptor.intercept(proxy, args, method);
        //'intercept' method will call 'invoke' method directly
        Mockito.verify(handler, Mockito.times(1)).invoke(proxy, method, args);
    }
}
