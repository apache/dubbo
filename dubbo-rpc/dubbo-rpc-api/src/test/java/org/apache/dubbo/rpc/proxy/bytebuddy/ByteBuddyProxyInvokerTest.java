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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.proxy.RemoteService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ByteBuddyProxyInvokerTest {

    @Test
    void testNewInstance() throws Throwable {
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");
        RemoteService proxy = Mockito.mock(RemoteService.class);
        ByteBuddyProxyInvoker<RemoteService> invoker = ByteBuddyProxyInvoker.newInstance(proxy, RemoteService.class, url);
        invoker.doInvoke(proxy, "sayHello", new Class[]{String.class}, new Object[]{"test"});
        Mockito.verify(proxy, Mockito.times(1)).sayHello("test");

        Assertions.assertThrows(IllegalArgumentException.class,
            () -> invoker.doInvoke(proxy, "equals", new Class[]{String.class}, new Object[]{"test", "test2"}));
    }
}
